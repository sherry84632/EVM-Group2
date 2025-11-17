package com.dealermanagementsysstem.project.Model;

import org.springframework.stereotype.Repository;
import utils.DBUtils;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class DAOPurchaseOrderDetail {

    public boolean insertOrderDetail(int purchaseOrderId, int colorId, int versionId, int quantity, java.math.BigDecimal unitPrice) {
        // If unitPrice passed is ZERO, auto-calculate from VehicleModel base price via VersionID + optional discount
        BigDecimal finalUnitPrice = unitPrice;
        if (finalUnitPrice == null || finalUnitPrice.compareTo(BigDecimal.ZERO) == 0) {
            finalUnitPrice = computeUnitPrice(versionId, null); // no dealer filter
        }
        String sql = "INSERT INTO PurchaseOrderDetail (PurchaseOrderID, ColorID, VersionID, Quantity, UnitPrice, Subtotal, PaymentStatus) VALUES (?, ?, ?, ?, ?, ?, 'UNPAID')";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, purchaseOrderId);
            ps.setInt(2, colorId);
            ps.setInt(3, versionId);
            ps.setInt(4, quantity);
            ps.setBigDecimal(5, finalUnitPrice);
            ps.setBigDecimal(6, finalUnitPrice.multiply(BigDecimal.valueOf(quantity))); // Calculate subtotal

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
        BigDecimal autoPrice = computeUnitPrice(versionId, null);
        return insertOrderDetail(purchaseOrderId, colorId, versionId, quantity, autoPrice);
    }

    // Consistent insertion with dealer-specific discount
    public boolean insertOrderDetailConsistent(int purchaseOrderId, int colorId, int versionId, int quantity, Integer dealerId) {
        BigDecimal unit = computeUnitPrice(versionId, dealerId);
        if (unit == null || unit.compareTo(BigDecimal.ZERO) == 0) unit = fetchBasePriceFromVersion(versionId);
        return insertOrderDetail(purchaseOrderId, colorId, versionId, quantity, unit);
    }

    // Dealer-aware unit price (REMOVE discount logic: always use pure BasePrice)
    public BigDecimal computeUnitPrice(int versionId, Integer dealerId) {
        String sql = "SELECT vm.BasePrice FROM VehicleVersion vv JOIN VehicleModel vm ON vv.ModelID = vm.ModelID WHERE vv.VersionID = ?";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, versionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal base = rs.getBigDecimal("BasePrice");
                    return base != null ? base : BigDecimal.ZERO;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return BigDecimal.ZERO;
    }

    private BigDecimal fetchBasePriceFromVersion(int versionId) {
        String sql = "SELECT vm.BasePrice FROM VehicleVersion vv JOIN VehicleModel vm ON vv.ModelID = vm.ModelID WHERE vv.VersionID = ?";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, versionId);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getBigDecimal("BasePrice"); }
        } catch (SQLException e) { e.printStackTrace(); }
        return BigDecimal.ZERO;
    }

    // Optionally expose list retrieval if needed (placeholder)
    public List<DTOPurchaseOrderDetail> placeholder() { return new ArrayList<>(); }

    /**
     * Update payment status for a specific purchase order detail
     */
    public boolean updatePaymentStatus(int poDetailId, String paymentStatus) {
        String sql = "UPDATE PurchaseOrderDetail SET PaymentStatus = ? WHERE PODetailID = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, paymentStatus);
            ps.setInt(2, poDetailId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update payment status for all details of a purchase order
     */
    public boolean updatePaymentStatusByPurchaseOrderId(int purchaseOrderId, String paymentStatus) {
        String sql = "UPDATE PurchaseOrderDetail SET PaymentStatus = ? WHERE PurchaseOrderID = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, paymentStatus);
            ps.setInt(2, purchaseOrderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Check if all order details for a purchase order are paid
     */
    public boolean areAllDetailsPaid(int purchaseOrderId) {
        String sql = "SELECT COUNT(*) as UnpaidCount FROM PurchaseOrderDetail WHERE PurchaseOrderID = ? AND PaymentStatus = 'UNPAID'";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, purchaseOrderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("UnpaidCount") == 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Get payment summary for a purchase order
     */
    public String getPaymentSummary(int purchaseOrderId) {
        String sql = """
            SELECT 
                COUNT(*) as TotalItems,
                SUM(CASE WHEN PaymentStatus = 'PAID' THEN 1 ELSE 0 END) as PaidItems,
                SUM(CASE WHEN PaymentStatus = 'UNPAID' THEN 1 ELSE 0 END) as UnpaidItems
            FROM PurchaseOrderDetail 
            WHERE PurchaseOrderID = ?
        """;
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, purchaseOrderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int total = rs.getInt("TotalItems");
                    int paid = rs.getInt("PaidItems");
                    int unpaid = rs.getInt("UnpaidItems");
                    return paid + "/" + total + " items paid (" + unpaid + " unpaid)";
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "N/A";
    }

    // --- NEW: Cập nhật lại đơn giá và subtotal cho 1 dòng chi tiết (fix giá sai) ---
    public boolean updateUnitAndSubtotal(int poDetailId, BigDecimal newUnitPrice, int quantity) {
        if (newUnitPrice == null) return false;
        BigDecimal newSubtotal = newUnitPrice.multiply(BigDecimal.valueOf(Math.max(1, quantity)));
        String sql = "UPDATE PurchaseOrderDetail SET UnitPrice = ?, Subtotal = ? WHERE PODetailID = ?";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, newUnitPrice);
            ps.setBigDecimal(2, newSubtotal);
            ps.setInt(3, poDetailId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }
}
