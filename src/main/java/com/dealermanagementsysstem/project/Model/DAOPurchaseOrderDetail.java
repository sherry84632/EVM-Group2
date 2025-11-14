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

    // Dealer-aware unit price (DiscountPolicy.HangPercent filtered by DealerID if provided)
    public BigDecimal computeUnitPrice(int versionId, Integer dealerId) {
        String sql = """
            SELECT vm.BasePrice,
                   pol.HangPercent AS DiscountPercent
            FROM VehicleVersion vv
            JOIN VehicleModel vm ON vv.ModelID = vm.ModelID
            OUTER APPLY (
                SELECT TOP 1 HangPercent
                FROM DiscountPolicy pol
                WHERE (? IS NULL OR pol.DealerID = ?)
                ORDER BY pol.CreatedAt DESC
            ) pol
            WHERE vv.VersionID = ?
        """;
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (dealerId == null) { ps.setNull(1, java.sql.Types.INTEGER); ps.setNull(2, java.sql.Types.INTEGER); }
            else { ps.setInt(1, dealerId); ps.setInt(2, dealerId); }
            ps.setInt(3, versionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal base = rs.getBigDecimal("BasePrice");
                    java.math.BigDecimal discountPercentBD = rs.getBigDecimal("DiscountPercent");
                    Double discountPercent = discountPercentBD != null ? discountPercentBD.doubleValue() : null;
                    if (base == null) return BigDecimal.ZERO;
                    if (discountPercent != null && discountPercent > 0) {
                        BigDecimal discount = base.multiply(BigDecimal.valueOf(discountPercent / 100.0));
                        return base.subtract(discount);
                    }
                    return base;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO;
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
}
