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
        String sql = "INSERT INTO PurchaseOrderDetail (PurchaseOrderID, ColorID, VersionID, Quantity, UnitPrice, Subtotal) VALUES (?, ?, ?, ?, ?, ?)";
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

    // Dealer-aware unit price (DealerPriceAdjustment filtered by DealerID if provided)
    public BigDecimal computeUnitPrice(int versionId, Integer dealerId) {
        String sql = """
            SELECT vm.BasePrice,
                   pa.DiscountPercent
            FROM VehicleVersion vv
            JOIN VehicleModel vm ON vv.ModelID = vm.ModelID
            OUTER APPLY (
                SELECT TOP 1 DiscountPercent
                FROM DealerPriceAdjustment pa
                WHERE pa.ModelID = vm.ModelID
                  AND (? IS NULL OR pa.DealerID = ?)
                  AND pa.StartDate <= GETDATE()
                  AND (pa.EndDate IS NULL OR pa.EndDate >= GETDATE())
                ORDER BY pa.StartDate DESC
            ) pa
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
                    Double discountPercent = rs.getObject("DiscountPercent", Double.class);
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
}
