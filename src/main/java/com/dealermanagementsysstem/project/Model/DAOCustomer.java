package com.dealermanagementsysstem.project.Model;

import com.dealermanagementsysstem.project.Model.DTOCustomer;
import utils.DBUtils;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.regex.Pattern;

public class DAOCustomer {

    // ✅ Kiểm tra số điện thoại hợp lệ (theo nhà mạng VN)
    private boolean isValidPhone(String phone) {
        String regex = "^(09|03|07|08|05)\\d{8}$";
        return Pattern.matches(regex, phone);
    }

    // ✅ Kiểm tra email hợp lệ
    private boolean isValidEmail(String email) {
        String regex = "^[\\w.%+-]+@[\\w.-]+\\.[A-Za-z]{2,}$";
        return Pattern.matches(regex, email);
    }

    // ✅ Kiểm tra ngày không nằm trong quá khứ
    private boolean isFutureOrPresent(Timestamp timestamp) {
        return timestamp == null || !timestamp.before(new Timestamp(System.currentTimeMillis()));
    }

    private boolean isFutureOrPresent(Date  date) {
        return date == null || !date.before(new Date(System.currentTimeMillis()));
    }

    // ✅ Lấy danh sách Customer
    public List<DTOCustomer> getAllCustomers() {
        List<DTOCustomer> list = new ArrayList<>();
        String sql = "SELECT * FROM Customer";

        try (Connection conn = DBUtils.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                DTOCustomer c = new DTOCustomer();
                c.setCustomerID(rs.getInt("CustomerID"));
                c.setFullName(rs.getString("FullName"));
                c.setPhone(rs.getString("Phone"));
                c.setEmail(rs.getString("Email"));
                c.setAddress(rs.getString("Address"));
                c.setCreatedAt(rs.getTimestamp("CreatedAt"));
                c.setBirthDate(rs.getDate("BirthDate"));
                c.setNote(rs.getString("Note"));
                c.setTestDriveSchedule(rs.getTimestamp("TestDriveSchedule"));
                c.setVehicleInterest(rs.getString("VehicleInterest"));
                list.add(c);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ✅ Thêm mới Customer
    public boolean insertCustomer(DTOCustomer c) {
        if (!isValidPhone(c.getPhone())) {
            System.out.println("❌ Số điện thoại không hợp lệ!");
            return false;
        }
        if (!isValidEmail(c.getEmail())) {
            System.out.println("❌ Email không hợp lệ!");
            return false;
        }
        if (c.getCreatedAt() != null && c.getCreatedAt().before(new Timestamp(System.currentTimeMillis()))) {
            System.out.println("❌ CreatedAt không được là quá khứ!");
            return false;
        }
        if (c.getTestDriveSchedule() != null && c.getTestDriveSchedule().before(new Timestamp(System.currentTimeMillis()))) {
            System.out.println("❌ TestDriveSchedule không được là quá khứ!");
            return false;
        }

        String sql = """
            INSERT INTO Customer (FullName, Phone, Email, Address, CreatedAt, BirthDate, Note, TestDriveSchedule, VehicleInterest)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, c.getFullName());
            ps.setString(2, c.getPhone());
            ps.setString(3, c.getEmail());
            ps.setString(4, c.getAddress());
            ps.setTimestamp(5, c.getCreatedAt());
            ps.setDate(6, c.getBirthDate());
            ps.setString(7, c.getNote());
            ps.setTimestamp(8, c.getTestDriveSchedule());
            ps.setString(9, c.getVehicleInterest());

            ps.executeUpdate();
            System.out.println("✅ Customer inserted successfully!");
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ✅ Cập nhật Customer
    public boolean updateCustomer(DTOCustomer c) {
        if (!isValidPhone(c.getPhone())) {
            System.out.println("❌ Số điện thoại không hợp lệ!");
            return false;
        }
        if (!isValidEmail(c.getEmail())) {
            System.out.println("❌ Email không hợp lệ!");
            return false;
        }
        if (c.getCreatedAt() != null && c.getCreatedAt().before(new Timestamp(System.currentTimeMillis()))) {
            System.out.println("❌ CreatedAt không được là quá khứ!");
            return false;
        }
        if (c.getTestDriveSchedule() != null && c.getTestDriveSchedule().before(new Timestamp(System.currentTimeMillis()))) {
            System.out.println("❌ TestDriveSchedule không được là quá khứ!");
            return false;
        }

        String sql = """
            UPDATE Customer 
            SET FullName=?, Phone=?, Email=?, Address=?, CreatedAt=?, BirthDate=?, Note=?, TestDriveSchedule=?, VehicleInterest=? 
            WHERE CustomerID=?
        """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, c.getFullName());
            ps.setString(2, c.getPhone());
            ps.setString(3, c.getEmail());
            ps.setString(4, c.getAddress());
            ps.setTimestamp(5, c.getCreatedAt());
            ps.setDate(6, c.getBirthDate());
            ps.setString(7, c.getNote());
            ps.setTimestamp(8, c.getTestDriveSchedule());
            ps.setString(9, c.getVehicleInterest());
            ps.setInt(10, c.getCustomerID());

            int updated = ps.executeUpdate();
            if (updated > 0) {
                System.out.println("✅ Customer updated successfully!");
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ✅ Xóa Customer
    public boolean deleteCustomer(int id) {
        String sql = "DELETE FROM Customer WHERE CustomerID=?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            int deleted = ps.executeUpdate();
            if (deleted > 0) {
                System.out.println("✅ Customer deleted successfully!");
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ✅ Tìm kiếm theo tên hoặc điện thoại
    public List<DTOCustomer> searchCustomer(String keyword) {
        List<DTOCustomer> list = new ArrayList<>();
        String sql = "SELECT * FROM Customer WHERE FullName LIKE ? OR Phone LIKE ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + keyword + "%");
            ps.setString(2, "%" + keyword + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DTOCustomer c = new DTOCustomer();
                    c.setCustomerID(rs.getInt("CustomerID"));
                    c.setFullName(rs.getString("FullName"));
                    c.setPhone(rs.getString("Phone"));
                    c.setEmail(rs.getString("Email"));
                    c.setAddress(rs.getString("Address"));
                    c.setCreatedAt(rs.getTimestamp("CreatedAt"));
                    c.setBirthDate(rs.getDate("BirthDate"));
                    c.setNote(rs.getString("Note"));
                    c.setTestDriveSchedule(rs.getTimestamp("TestDriveSchedule"));
                    c.setVehicleInterest(rs.getString("VehicleInterest"));
                    list.add(c);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
