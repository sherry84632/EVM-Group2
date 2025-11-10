package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Date;
import java.sql.SQLException;


import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;


import org.springframework.stereotype.Repository;

@Repository
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

                // ✅ Đồng bộ LocalDateTime
                Timestamp createdAt = rs.getTimestamp("CreatedAt");
                c.setCreatedAt(createdAt != null ? createdAt.toLocalDateTime() : null);

                Date birthDate = rs.getDate("BirthDate");
                c.setBirthDate(birthDate != null ? birthDate.toLocalDate() : null);

                c.setNote(rs.getString("Note"));

                Timestamp updatedAt = rs.getTimestamp("UpdatedAt");
                c.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);

                c.setVehicleInterest(rs.getString("VehicleInterest"));
                list.add(c);
            }
        } catch (SQLException e) {
            System.out.println("❌ Error while fetching customers:");
            e.printStackTrace();
        }
        return list;
    }

    // ✅ Thêm mới Customer - trả về customerID
    public int insertCustomer(DTOCustomer c) {
        String sql = """
                    INSERT INTO Customer (DealerID, FullName, Phone, Email, Address, CreatedAt, UpdatedAt, BirthDate, Note, VehicleInterest)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // ✅ DealerID must be set by controller from session (no default value)
            if (c.getDealer() != null && c.getDealer().getDealerID() > 0) {
                ps.setInt(1, c.getDealer().getDealerID());
            } else {
                ps.setNull(1, java.sql.Types.INTEGER); // Allow NULL if no dealer assigned
            }
            ps.setString(2, c.getFullName());
            ps.setString(3, c.getPhone());
            ps.setString(4, c.getEmail());
            ps.setNString(5, c.getAddress());

            ps.setTimestamp(6, c.getCreatedAt() != null ? Timestamp.valueOf(c.getCreatedAt()) : null);
            ps.setTimestamp(7, c.getUpdatedAt() != null ? Timestamp.valueOf(c.getUpdatedAt()) : null);

            ps.setDate(8, c.getBirthDate() != null ? java.sql.Date.valueOf(c.getBirthDate()) : null);
            ps.setNString(9, c.getNote());
            ps.setNString(10, c.getVehicleInterest());

            int rows = ps.executeUpdate();
            if (rows > 0) {
                // ✅ Lấy customerID vừa tạo
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int newCustomerID = generatedKeys.getInt(1);
                        System.out.println("✅ Customer inserted successfully: " + c.getFullName() + " (ID: " + newCustomerID + ")");
                        return newCustomerID;
                    }
                }
            }

        } catch (SQLException e) {
            System.out.println("❌ Failed to insert customer!");
            e.printStackTrace();
        }
        return -1; // ✅ Trả về -1 nếu thất bại
    }

    // ✅ Cập nhật Customer
    public boolean updateCustomer(DTOCustomer c) {
        String sql = """
                    UPDATE Customer 
                    SET DealerID=?, FullName=?, Phone=?, Email=?, Address=?, CreatedAt=?, UpdatedAt=?, BirthDate=?, Note=?, VehicleInterest=? 
                    WHERE CustomerID=?
                """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // ✅ DealerID must be set by controller (no default value)
            if (c.getDealer() != null && c.getDealer().getDealerID() > 0) {
                ps.setInt(1, c.getDealer().getDealerID());
            } else {
                ps.setNull(1, java.sql.Types.INTEGER); // Allow NULL if no dealer assigned
            }
            ps.setString(2, c.getFullName());
            ps.setString(3, c.getPhone());
            ps.setString(4, c.getEmail());
            ps.setNString(5, c.getAddress());
            ps.setTimestamp(6, c.getCreatedAt() != null ? Timestamp.valueOf(c.getCreatedAt()) : null);
            ps.setTimestamp(7, c.getUpdatedAt() != null ? Timestamp.valueOf(c.getUpdatedAt()) : null);
            ps.setDate(8, c.getBirthDate() != null ? java.sql.Date.valueOf(c.getBirthDate()) : null);
            ps.setNString(9, c.getNote());
            ps.setNString(10, c.getVehicleInterest());
            ps.setInt(11, c.getCustomerID());

            int updated = ps.executeUpdate();
            if (updated > 0) {
                System.out.println("✅ Customer updated successfully: " + c.getFullName());
                return true;
            }

        } catch (SQLException e) {
            System.out.println("❌ Failed to update customer!");
            e.printStackTrace();
        }
        return false;
    }

    // ✅ Xóa Customer (xóa cascade TestDrive trước)
    public boolean deleteCustomer(int id) {
        Connection conn = null;
        try {
            conn = DBUtils.getConnection();
            conn.setAutoCommit(false); // Bắt đầu transaction

            // 1. Xóa TestDrive liên quan trước
            String deleteTestDriveSQL = "DELETE FROM TestDrive WHERE CustomerID = ?";
            try (PreparedStatement ps1 = conn.prepareStatement(deleteTestDriveSQL)) {
                ps1.setInt(1, id);
                int testDrivesDeleted = ps1.executeUpdate();
                System.out.println("🗑️ Deleted " + testDrivesDeleted + " test drive(s) for Customer ID: " + id);
            }

            // 2. Xóa Customer
            String deleteCustomerSQL = "DELETE FROM Customer WHERE CustomerID = ?";
            try (PreparedStatement ps2 = conn.prepareStatement(deleteCustomerSQL)) {
                ps2.setInt(1, id);
                int deleted = ps2.executeUpdate();
                if (deleted > 0) {
                    conn.commit(); // Commit transaction
                    System.out.println("🗑️ Customer deleted successfully (ID: " + id + ")");
                    return true;
                } else {
                    conn.rollback();
                    System.out.println("⚠️ Customer not found (ID: " + id + ")");
                    return false;
                }
            }

        } catch (SQLException e) {
            System.out.println("❌ Failed to delete customer!");
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // ✅ Tìm kiếm Customer
    public List<DTOCustomer> searchCustomer(String keyword) {
        List<DTOCustomer> list = new ArrayList<>();
        String sql = "SELECT * FROM Customer WHERE FullName LIKE ? OR Phone LIKE ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setNString(1, "%" + keyword + "%");
            ps.setString(2, "%" + keyword + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DTOCustomer c = new DTOCustomer();
                    c.setCustomerID(rs.getInt("CustomerID"));
                    c.setFullName(rs.getString("FullName"));
                    c.setPhone(rs.getString("Phone"));
                    c.setEmail(rs.getString("Email"));
                    c.setAddress(rs.getString("Address"));

                    Timestamp createdAt = rs.getTimestamp("CreatedAt");
                    c.setCreatedAt(createdAt != null ? createdAt.toLocalDateTime() : null);

                    Date birthDate = rs.getDate("BirthDate");
                    c.setBirthDate(birthDate != null ? birthDate.toLocalDate() : null);

                    c.setNote(rs.getString("Note"));

                    Timestamp updatedAt = rs.getTimestamp("UpdatedAt");
                    c.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);

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

    // ✅ Lấy Customer theo ID
    public DTOCustomer getCustomerById(int id) {
        String sql = "SELECT * FROM Customer WHERE CustomerID = ?";
        DTOCustomer c = null;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    c = new DTOCustomer();
                    c.setCustomerID(rs.getInt("CustomerID"));
                    c.setFullName(rs.getString("FullName"));
                    c.setPhone(rs.getString("Phone"));
                    c.setEmail(rs.getString("Email"));
                    c.setAddress(rs.getString("Address"));

                    Timestamp createdAt = rs.getTimestamp("CreatedAt");
                    c.setCreatedAt(createdAt != null ? createdAt.toLocalDateTime() : null);

                    Date birthDate = rs.getDate("BirthDate");
                    c.setBirthDate(birthDate != null ? birthDate.toLocalDate() : null);

                    c.setNote(rs.getString("Note"));

                    Timestamp updatedAt = rs.getTimestamp("UpdatedAt");
                    c.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);

                    c.setVehicleInterest(rs.getString("VehicleInterest"));
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Failed to get customer by ID: " + id);
            e.printStackTrace();
        }
        return c;
    }

    // ✅ Lấy danh sách Customer theo DealerID
    public List<DTOCustomer> getCustomersByDealerId(int dealerId) {
        List<DTOCustomer> list = new ArrayList<>();
        String sql = "SELECT * FROM Customer WHERE DealerID = ? ORDER BY CreatedAt DESC";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, dealerId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DTOCustomer c = new DTOCustomer();
                    c.setCustomerID(rs.getInt("CustomerID"));
                    c.setFullName(rs.getString("FullName"));
                    c.setPhone(rs.getString("Phone"));
                    c.setEmail(rs.getString("Email"));
                    c.setAddress(rs.getString("Address"));

                    Timestamp createdAt = rs.getTimestamp("CreatedAt");
                    c.setCreatedAt(createdAt != null ? createdAt.toLocalDateTime() : null);

                    Date birthDate = rs.getDate("BirthDate");
                    c.setBirthDate(birthDate != null ? birthDate.toLocalDate() : null);

                    c.setNote(rs.getString("Note"));

                    Timestamp updatedAt = rs.getTimestamp("UpdatedAt");
                    c.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);

                    c.setVehicleInterest(rs.getString("VehicleInterest"));
                    list.add(c);
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Failed to get customers by dealer ID: " + dealerId);
            e.printStackTrace();
        }
        return list;
    }

    // ✅ Tìm kiếm Customer theo keyword và DealerID
    public List<DTOCustomer> searchCustomerByDealerId(String keyword, int dealerId) {
        List<DTOCustomer> list = new ArrayList<>();
        String sql = "SELECT * FROM Customer WHERE (FullName LIKE ? OR Phone LIKE ?) AND DealerID = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + keyword + "%");
            ps.setString(2, "%" + keyword + "%");
            ps.setInt(3, dealerId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DTOCustomer c = new DTOCustomer();
                    c.setCustomerID(rs.getInt("CustomerID"));
                    c.setFullName(rs.getString("FullName"));
                    c.setPhone(rs.getString("Phone"));
                    c.setEmail(rs.getString("Email"));
                    c.setAddress(rs.getString("Address"));

                    Timestamp createdAt = rs.getTimestamp("CreatedAt");
                    c.setCreatedAt(createdAt != null ? createdAt.toLocalDateTime() : null);

                    Date birthDate = rs.getDate("BirthDate");
                    c.setBirthDate(birthDate != null ? birthDate.toLocalDate() : null);

                    c.setNote(rs.getString("Note"));

                    Timestamp updatedAt = rs.getTimestamp("UpdatedAt");
                    c.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);

                    c.setVehicleInterest(rs.getString("VehicleInterest"));
                    list.add(c);
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Failed to search customer by dealer ID: " + dealerId);
            e.printStackTrace();
        }
        return list;
    }
}
