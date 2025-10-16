package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DAOPurchaseOrderItem {

    public int addItem(DTOPurchaseOrderItem item) {
        String sql = "INSERT INTO PurchaseOrderItem (PurchaseOrderID, VehicleVIN, Quantity, UnitPrice, DiscountPercent) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, item.getPurchaseOrderId());
            ps.setString(2, item.getVehicleVin());
            ps.setInt(3, item.getQuantity());
            ps.setBigDecimal(4, item.getUnitPrice());
            if (item.getDiscountPercent() != null) ps.setDouble(5, item.getDiscountPercent()); else ps.setNull(5, Types.DOUBLE);
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<DTOPurchaseOrderItem> getItemsByOrderId(int orderId) {
        List<DTOPurchaseOrderItem> list = new ArrayList<>();
        String sql = "SELECT * FROM PurchaseOrderItem WHERE PurchaseOrderID = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DTOPurchaseOrderItem it = new DTOPurchaseOrderItem();
                    it.setPurchaseOrderItemId(rs.getInt("PurchaseOrderItemID"));
                    it.setPurchaseOrderId(rs.getInt("PurchaseOrderID"));
                    it.setVehicleVin(rs.getString("VehicleVIN"));
                    it.setQuantity(rs.getInt("Quantity"));
                    it.setUnitPrice(rs.getBigDecimal("UnitPrice"));
                    it.setDiscountPercent((Double) rs.getObject("DiscountPercent"));
                    list.add(it);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int deleteItem(int purchaseOrderItemId) {
        String sql = "DELETE FROM PurchaseOrderItem WHERE PurchaseOrderItemID = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, purchaseOrderItemId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}


