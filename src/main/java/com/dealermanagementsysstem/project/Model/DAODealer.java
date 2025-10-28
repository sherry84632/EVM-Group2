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

    // 🟢 Thêm Dealer mới
    public void insertDealer(DTODealer d) throws SQLException {
        String sql = "INSERT INTO Dealer (DealerName, Address, Phone, Email, EvmID, LevelID, PolicyID) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, d.getDealerName());
            ps.setString(2, d.getAddress());
            ps.setString(3, d.getPhone());
            ps.setString(4, d.getEmail());
            ps.setInt(5, d.getEvmID());
            ps.setInt(6, d.getLevelID());
            ps.setInt(7, d.getPolicyID());
            ps.executeUpdate();
        }
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
    public void deleteDealer(int id) throws SQLException {
        String sql = "DELETE FROM Dealer WHERE DealerID = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
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
                    System.out.println("✅ Found StaffID=" + staffID + " for DealerID=" + dealerID);
                    return staffID;
                } else {
                    System.out.println("⚠️ No staff found for DealerID=" + dealerID);
                    return null;
                }
            }

        } catch (SQLException e) {
            System.out.println("❌ Error getting staff for DealerID=" + dealerID);
            e.printStackTrace();
            return null;
        }
    }
}
