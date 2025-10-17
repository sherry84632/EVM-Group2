package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;
import java.sql.*;
import java.util.*;

public class DAOCustomer {

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
            System.out.println("❌ Error while fetching customers:");
            e.printStackTrace();
        }
        return list;
    }

    // ✅ Thêm mới Customer (KHÔNG kiểm tra trùng, KHÔNG kiểm tra ngày)
    public boolean insertCustomer(DTOCustomer c) {
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

            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Customer inserted successfully: " + c.getFullName());
                return true;
            } else {
                System.out.println("⚠️ No customer was inserted (0 rows affected).");
            }

        } catch (SQLException e) {
            System.out.println("❌ Failed to insert customer!");
            e.printStackTrace();
        }
        return false;
    }

    // ✅ Cập nhật Customer (KHÔNG kiểm tra trùng)
    public boolean updateCustomer(DTOCustomer c) {
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
                System.out.println("✅ Customer updated successfully: " + c.getFullName());
                return true;
            } else {
                System.out.println("⚠️ No customer updated (ID not found: " + c.getCustomerID() + ")");
            }

        } catch (SQLException e) {
            System.out.println("❌ Failed to update customer!");
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
                System.out.println("🗑️ Customer deleted successfully (ID: " + id + ")");
                return true;
            } else {
                System.out.println("⚠️ No customer deleted (ID not found: " + id + ")");
            }
        } catch (SQLException e) {
            System.out.println("❌ Failed to delete customer!");
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
            System.out.println("❌ Failed to search customer!");
            e.printStackTrace();
        }
        return list;
    }
}
