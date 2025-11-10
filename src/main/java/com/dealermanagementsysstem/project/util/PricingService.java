package com.dealermanagementsysstem.project.util;

import utils.DBUtils;
import java.math.BigDecimal;
import java.sql.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central pricing logic: gathers BasePrice, active DealerPriceAdjustment, DiscountPolicy.
 * Rules (assumptions – adjust if business clarifies differently):
 * 1. Start from VehicleModel.BasePrice.
 * 2. Apply active DealerPriceAdjustment discountPercent (promotion) if any.
 * 3. Apply DiscountPolicy.discountPercent if any (NOT hangPercent/dailyPercent which are revenue split, not price markdown).
 * 4. Percent stacking is multiplicative: price = base*(1 - promo%)*(1 - policy%).
 * 5. Safeguards: negative or >100% percent treated as 0.
 * 6. Result rounded HALF_UP scale 2.
 */
public class PricingService {
    private static final Logger log = LoggerFactory.getLogger(PricingService.class);

    private PricingService() {}

    public static BigDecimal computeDealerUnitPrice(int versionId, Integer dealerId) {
        String sql = """
            SELECT vm.BasePrice,
                   pol.DiscountPercent AS PolicyDiscountPercent,
                   promo.DiscountPercent AS PromoDiscountPercent
            FROM VehicleVersion vv
            JOIN VehicleModel vm ON vv.ModelID = vm.ModelID
            OUTER APPLY (
               SELECT TOP 1 DiscountPercent
               FROM DiscountPolicy pol
               WHERE (? IS NOT NULL AND pol.DealerID = ?)
               ORDER BY pol.CreatedAt DESC
            ) pol
            OUTER APPLY (
               SELECT TOP 1 DiscountPercent
               FROM DealerPriceAdjustment promo
               WHERE (? IS NOT NULL AND promo.DealerID = ? AND promo.ModelID = vm.ModelID
                     AND promo.StartDate <= GETDATE()
                     AND (promo.EndDate IS NULL OR promo.EndDate >= GETDATE()))
               ORDER BY promo.StartDate DESC
            ) promo
            WHERE vv.VersionID = ?
        """;
        BigDecimal base = BigDecimal.ZERO;
        Double policyPct = null;
        Double promoPct = null;
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            // parameters repeated for OUTER APPLY subqueries
            if (dealerId == null) {
                ps.setNull(1, Types.INTEGER); ps.setNull(2, Types.INTEGER);
                ps.setNull(3, Types.INTEGER); ps.setNull(4, Types.INTEGER);
            } else {
                ps.setInt(1, dealerId); ps.setInt(2, dealerId);
                ps.setInt(3, dealerId); ps.setInt(4, dealerId);
            }
            ps.setInt(5, versionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    base = rs.getBigDecimal("BasePrice");
                    BigDecimal policyBD = rs.getBigDecimal("PolicyDiscountPercent");
                    BigDecimal promoBD = rs.getBigDecimal("PromoDiscountPercent");
                    policyPct = policyBD != null ? policyBD.doubleValue() : null;
                    promoPct = promoBD != null ? promoBD.doubleValue() : null;
                }
            }
        } catch (SQLException e) {
            log.error("Pricing query failed versionId={} dealerId={}", versionId, dealerId, e);
            return BigDecimal.ZERO;
        }
        if (base == null) base = BigDecimal.ZERO;
        // sanitize percents
        policyPct = sanitizePercent(policyPct);
        promoPct = sanitizePercent(promoPct);
        BigDecimal price = base;
        if (promoPct != null && promoPct > 0) {
            price = applyPercent(price, promoPct);
        }
        if (policyPct != null && policyPct > 0) {
            price = applyPercent(price, policyPct);
        }
        return price.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private static Double sanitizePercent(Double pct) {
        if (pct == null) return null;
        if (pct < 0) return 0.0;
        if (pct > 100) return 100.0;
        return pct;
    }

    private static BigDecimal applyPercent(BigDecimal base, Double pct) {
        if (base == null) base = BigDecimal.ZERO;
        if (pct == null || pct <= 0) return base;
        BigDecimal discount = base.multiply(BigDecimal.valueOf(pct / 100.0));
        return base.subtract(discount);
    }
}

