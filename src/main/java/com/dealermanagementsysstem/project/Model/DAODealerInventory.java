package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DAODealerInventory {

    public boolean insertInventory(int dealerId, String vin, String status) {
        String sql = "INSERT INTO DealerInventory (DealerID, VIN, ReceivedDate, Status) VALUES (?, ?, GETDATE(), ?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dealerId);
            ps.setString(2, vin);
            ps.setString(3, status);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean exists(String vin) {
        String sql = "SELECT 1 FROM DealerInventory WHERE VIN = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, vin);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public java.util.List<DTODealerInventory> listByDealer(int dealerId) {
        java.util.List<DTODealerInventory> list = new java.util.ArrayList<>();
        String sql = "SELECT DealerID, VIN, ReceivedDate, Status FROM DealerInventory WHERE DealerID = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dealerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DTODealerInventory d = new DTODealerInventory();
                    d.setDealerId(rs.getInt("DealerID"));
                    d.setVin(rs.getString("VIN"));
                    d.setReceivedDate(rs.getDate("ReceivedDate"));
                    d.setStatus(rs.getString("Status"));
                    list.add(d);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}


