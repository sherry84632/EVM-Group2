package com.dealermanagementsysstem.project.Model;

import org.springframework.stereotype.Repository;
import utils.DBUtils;
import java.sql.*;
import java.util.*;

@Repository
public class DAODealer {

    // Lấy toàn bộ danh sách Dealer
    public List<DTODealer> getAllDealers() {
        List<DTODealer> dealers = new ArrayList<>();
        String sql = "SELECT DealerID, DealerName, Address, Phone, Email, EvmID, LevelID, PolicyID FROM Dealer";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                DTODealer dealer = new DTODealer();
                dealer.setDealerID(rs.getInt("DealerID"));
                dealer.setDealerName(rs.getString("DealerName"));
                dealer.setAddress(rs.getString("Address"));
                dealer.setPhone(rs.getString("Phone"));
                dealer.setEmail(rs.getString("Email"));
                dealer.setEvmID(rs.getInt("EvmID"));
                dealer.setLevelID(rs.getInt("LevelID"));
                dealer.setPolicyID(rs.getInt("PolicyID"));
                dealers.add(dealer);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return dealers;
    }

    // Lấy Dealer theo ID
    public DTODealer getDealerById(int id) throws SQLException {
        String sql = "SELECT DealerID, DealerName, Address, Phone, Email, EvmID, LevelID, PolicyID FROM Dealer WHERE DealerID = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DTODealer dealer = new DTODealer();
                    dealer.setDealerID(rs.getInt("DealerID"));
                    dealer.setDealerName(rs.getString("DealerName"));
                    dealer.setAddress(rs.getString("Address"));
                    dealer.setPhone(rs.getString("Phone"));
                    dealer.setEmail(rs.getString("Email"));
                    dealer.setEvmID(rs.getInt("EvmID"));
                    dealer.setLevelID(rs.getInt("LevelID"));
                    dealer.setPolicyID(rs.getInt("PolicyID"));
                    return dealer;
                }
            }
        }
        return null;
    }

    // 🟢 Thêm Dealer mới - Returns generated DealerID
    public int insertDealer(DTODealer d) {
        String sql = "INSERT INTO Dealer (DealerName, Address, Phone, Email, EvmID, LevelID, PolicyID) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, d.getDealerName());
            ps.setString(2, d.getAddress() != null ? d.getAddress() : "");
            ps.setString(3, d.getPhone());
            ps.setString(4, d.getEmail());

            // Handle nullable EvmID (0 = null)
            if (d.getEvmID() > 0) {
                ps.setInt(5, d.getEvmID());
            } else {
                ps.setNull(5, Types.INTEGER);
            }

            ps.setInt(6, d.getLevelID());

            // Handle nullable PolicyID (0 = null)
            if (d.getPolicyID() > 0) {
                ps.setInt(7, d.getPolicyID());
            } else {
                ps.setNull(7, Types.INTEGER);
            }

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int dealerId = rs.getInt(1);
                        System.out.println(" Dealer created successfully with ID: " + dealerId);
                        return dealerId;
                    }
                }
            }

        } catch (SQLException e) {
            System.out.println(" Error inserting dealer: " + e.getMessage());
            e.printStackTrace();
        }
        return -1; // Failed
    }

    // Cập nhật Dealer
    public void updateDealer(DTODealer d) throws SQLException {
        String sql = "UPDATE Dealer SET DealerName=?, Address=?, Phone=?, Email=?, EvmID=?, LevelID=?, PolicyID=? WHERE DealerID=?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, d.getDealerName());
            ps.setString(2, d.getAddress());
            ps.setString(3, d.getPhone());
            ps.setString(4, d.getEmail());
            ps.setInt(5, d.getEvmID());
            ps.setInt(6, d.getLevelID());
            ps.setInt(7, d.getPolicyID());
            ps.setInt(8, d.getDealerID());
            ps.executeUpdate();
        }
    }

    // Xóa Dealer theo ID
    public boolean deleteDealer(int id) throws SQLException {
        String sql = "DELETE FROM Dealer WHERE DealerID = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Lấy StaffID đầu tiên thuộc Dealer (để gán vào TestDrive)
     * @param dealerID ID của dealer
     * @return StaffID hoặc null nếu dealer không có staff
     */
    public Integer getFirstStaffIdByDealerId(int dealerID) {
        String sql = "SELECT TOP 1 StaffID FROM DealerStaff WHERE DealerID = ? ORDER BY StaffID ASC";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, dealerID);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int staffID = rs.getInt("StaffID");
                    System.out.println(" Found StaffID=" + staffID + " for DealerID=" + dealerID);
                    return staffID;
                } else {
                    System.out.println(" No staff found for DealerID=" + dealerID);
                    return null;
                }
            }

        } catch (SQLException e) {
            System.out.println(" Error getting staff for DealerID=" + dealerID);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Update only the LevelID for a Dealer.
     */
    public boolean updateDealerLevel(int dealerID, int newLevelID) {
        String sql = "UPDATE Dealer SET LevelID=? WHERE DealerID=?";
        try (java.sql.Connection conn = utils.DBUtils.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newLevelID);
            ps.setInt(2, dealerID);
            return ps.executeUpdate() > 0;
        } catch (java.sql.SQLException e) {
            System.err.println("Failed to update dealer level dealerID=" + dealerID + " levelID=" + newLevelID);
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Get DealerLevel record by LevelID.
     */
    public DTODealerLevel getDealerLevelById(int levelID) {
        String sql = "SELECT LevelID, LevelName, MinOrderValue, MaxOrderValue FROM DealerLevel WHERE LevelID=?";
        try (java.sql.Connection conn = utils.DBUtils.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, levelID);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DTODealerLevel lvl = new DTODealerLevel();
                    lvl.setLevelID(rs.getInt("LevelID"));
                    lvl.setLevelName(rs.getString("LevelName"));
                    lvl.setMinOrderValue(rs.getBigDecimal("MinOrderValue"));
                    lvl.setMaxOrderValue(rs.getBigDecimal("MaxOrderValue"));
                    return lvl;
                }
            }
        } catch (java.sql.SQLException e) {
            System.err.println("Failed to get dealer level id=" + levelID);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Fetch all dealer levels from DB.
     */
    public java.util.List<DTODealerLevel> getAllDealerLevels() {
        java.util.List<DTODealerLevel> list = new java.util.ArrayList<>();
        String sql = "SELECT LevelID, LevelName, MinOrderValue, MaxOrderValue FROM DealerLevel ORDER BY LevelID";
        try (java.sql.Connection conn = utils.DBUtils.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                DTODealerLevel lvl = new DTODealerLevel();
                lvl.setLevelID(rs.getInt("LevelID"));
                lvl.setLevelName(rs.getString("LevelName"));
                lvl.setMinOrderValue(rs.getBigDecimal("MinOrderValue"));
                lvl.setMaxOrderValue(rs.getBigDecimal("MaxOrderValue"));
                list.add(lvl);
            }
        } catch (java.sql.SQLException e) {
            System.err.println("Failed to fetch dealer levels: " + e.getMessage());
        }
        return list;
    }
}
