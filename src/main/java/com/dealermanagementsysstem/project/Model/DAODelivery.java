package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;

import java.sql.*;

public class DAODelivery {

    public Integer createDelivery(int purchaseOrderId, int dealerId, String status) {
        String sql = "INSERT INTO Delivery (PurchaseOrderID, DealerID, DeliveryDate, Status) VALUES (?, ?, GETDATE(), ?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, purchaseOrderId);
            ps.setInt(2, dealerId);
            ps.setString(3, status);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}


