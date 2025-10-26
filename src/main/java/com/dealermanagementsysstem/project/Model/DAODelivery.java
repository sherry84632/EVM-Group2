package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class DAODelivery {

    // ✅ Lấy danh sách Delivery
    public List<DTODelivery> getAllDeliveries() {
        List<DTODelivery> list = new ArrayList<>();
        String sql = """
            SELECT d.DeliveryID, d.PurchaseOrderID, d.DeliveryDate, d.DeliveryStatus,
                   po.PurchaseOrderID, po.CreatedAt AS OrderDate
            FROM Delivery d
            JOIN PurchaseOrder po ON d.PurchaseOrderID = po.PurchaseOrderID
            ORDER BY d.DeliveryDate DESC
        """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                DTODelivery delivery = new DTODelivery();
                delivery.setDeliveryID(rs.getInt("DeliveryID"));
                delivery.setDeliveryDate(rs.getDate("DeliveryDate"));
                delivery.setDeliveryStatus(DeliveryStatus.valueOf(rs.getString("DeliveryStatus")));

                // PurchaseOrder info
                DTOPurchaseOrder purchaseOrder = new DTOPurchaseOrder();
                purchaseOrder.setPurchaseOrderId(rs.getInt("PurchaseOrderID"));
                delivery.setPurchaseOrder(purchaseOrder);

                list.add(delivery);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ✅ Lấy Delivery theo ID
    public DTODelivery getDeliveryById(int deliveryID) {
        String sql = """
            SELECT d.DeliveryID, d.PurchaseOrderID, d.DeliveryDate, d.DeliveryStatus,
                   po.PurchaseOrderID, po.CreatedAt AS OrderDate
            FROM Delivery d
            JOIN PurchaseOrder po ON d.PurchaseOrderID = po.PurchaseOrderID
            WHERE d.DeliveryID = ?
        """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, deliveryID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DTODelivery delivery = new DTODelivery();
                    delivery.setDeliveryID(rs.getInt("DeliveryID"));
                    delivery.setDeliveryDate(rs.getDate("DeliveryDate"));
                    delivery.setDeliveryStatus(DeliveryStatus.valueOf(rs.getString("DeliveryStatus")));

                    // PurchaseOrder info
                    DTOPurchaseOrder purchaseOrder = new DTOPurchaseOrder();
                    purchaseOrder.setPurchaseOrderId(rs.getInt("PurchaseOrderID"));
                    delivery.setPurchaseOrder(purchaseOrder);

                    return delivery;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ✅ Tạo Delivery mới
    public boolean createDelivery(DTODelivery delivery) {
        String sql = "INSERT INTO Delivery (PurchaseOrderID, DeliveryDate, DeliveryStatus) VALUES (?, ?, ?)";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, delivery.getPurchaseOrder().getPurchaseOrderId());
            ps.setDate(2, (Date) delivery.getDeliveryDate());
            ps.setString(3, delivery.getDeliveryStatus().toString());

            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ✅ Cập nhật trạng thái Delivery
    public boolean updateDeliveryStatus(int deliveryID, DeliveryStatus status) {
        String sql = "UPDATE Delivery SET DeliveryStatus = ? WHERE DeliveryID = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.toString());
            ps.setInt(2, deliveryID);

            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
