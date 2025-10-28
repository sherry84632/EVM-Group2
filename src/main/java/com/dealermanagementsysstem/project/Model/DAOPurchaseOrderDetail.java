package com.dealermanagementsysstem.project.Model;

import org.springframework.stereotype.Repository;
import utils.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class DAOPurchaseOrderDetail {

    public boolean insertOrderDetail(int purchaseOrderId, int colorId, int versionId, int quantity, java.math.BigDecimal unitPrice) {
        String sql = "INSERT INTO PurchaseOrderDetail (PurchaseOrderID, ColorID, VersionID, Quantity, UnitPrice, Subtotal) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, purchaseOrderId);
            ps.setInt(2, colorId);
            ps.setInt(3, versionId);
            ps.setInt(4, quantity);
            ps.setBigDecimal(5, unitPrice);
            ps.setBigDecimal(6, unitPrice.multiply(java.math.BigDecimal.valueOf(quantity))); // Calculate subtotal

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Overloaded method for backward compatibility
    public boolean insertOrderDetail(int purchaseOrderId, int modelId, int colorId, int quantity, String version) {
        // Convert version string to version ID (assuming version is the ID)
        int versionId = Integer.parseInt(version);
        return insertOrderDetail(purchaseOrderId, colorId, versionId, quantity, java.math.BigDecimal.ZERO);
    }

}
