package com.dealermanagementsysstem.project.Model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import utils.DBUtils;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DAOPromoCode - Manages customer promotional discount codes
 *
 * This DAO handles CRUD operations for promotional codes that customers
 * can use when purchasing vehicles (e.g., "SUMMER2024", "NEWCAR15")
 *
 * Replaces the old DiscountPolicy logic which was incorrectly used for
 * manufacturer-dealer commission splits.
 */
@Repository
@SuppressWarnings({"SqlDialectInspection", "SqlNoDataSourceInspection", "unused"})
public class DAOPromoCode {

    private static final Logger log = LoggerFactory.getLogger(DAOPromoCode.class);

    /**
     * Create a new promo code
     */
    public int createPromoCode(DTODiscountPolicy promo) {
        String sql = """
            INSERT INTO DiscountPolicy 
            (PolicyName, PromoCode, Description, DiscountPercent, DiscountAmount,
             MinPurchaseAmount, MaxDiscountAmount, UsageLimit, UsedCount,
             ApplicableToModels, StartDate, EndDate, Status, CreatedAt, CreatedBy)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, GETDATE(), ?)
        """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, promo.getPolicyName());
            ps.setString(2, promo.getPromoCode());
            ps.setString(3, promo.getDescription());
            ps.setBigDecimal(4, promo.getDiscountPercent());
            ps.setBigDecimal(5, promo.getDiscountAmount());
            ps.setBigDecimal(6, promo.getMinPurchaseAmount());
            ps.setBigDecimal(7, promo.getMaxDiscountAmount());
            ps.setObject(8, promo.getUsageLimit(), Types.INTEGER);
            ps.setInt(9, promo.getUsedCount() != null ? promo.getUsedCount() : 0);
            ps.setString(10, promo.getApplicableToModels());
            ps.setDate(11, Date.valueOf(promo.getStartDate()));
            ps.setDate(12, promo.getEndDate() != null ? Date.valueOf(promo.getEndDate()) : null);
            ps.setString(13, promo.getStatus().toString());
            ps.setString(14, promo.getCreatedBy());

            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int promoId = rs.getInt(1);
                        log.info("✅ Created promo code: {} (ID={})", promo.getPromoCode(), promoId);
                        return promoId;
                    }
                }
            }
        } catch (SQLException e) {
            log.error("❌ Error creating promo code: {}", promo.getPromoCode(), e);
        }
        return -1;
    }

    /**
     * Get all active promo codes
     */
    public List<DTODiscountPolicy> getActivePromoCodes() {
        List<DTODiscountPolicy> list = new ArrayList<>();
        String sql = """
            SELECT * FROM DiscountPolicy
            WHERE Status = 'ACTIVE'
            AND StartDate <= CAST(GETDATE() AS DATE)
            AND (EndDate IS NULL OR EndDate >= CAST(GETDATE() AS DATE))
            AND (UsageLimit IS NULL OR UsedCount < UsageLimit)
            ORDER BY CreatedAt DESC
        """;

        try (Connection conn = DBUtils.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapPromoCode(rs));
            }
            log.info("📋 Loaded {} active promo codes", list.size());
        } catch (SQLException e) {
            log.error("❌ Error fetching active promo codes", e);
        }
        return list;
    }

    /**
     * Get all promo codes (for admin management)
     */
    public List<DTODiscountPolicy> getAllPromoCodes() {
        List<DTODiscountPolicy> list = new ArrayList<>();
        String sql = "SELECT * FROM DiscountPolicy ORDER BY CreatedAt DESC";

        try (Connection conn = DBUtils.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapPromoCode(rs));
            }
            log.info("📋 Loaded {} total promo codes", list.size());
        } catch (SQLException e) {
            log.error("❌ Error fetching all promo codes", e);
        }
        return list;
    }

    /**
     * Find promo code by code string (for customer validation)
     */
    public DTODiscountPolicy findByPromoCode(String promoCode) {
        String sql = "SELECT * FROM DiscountPolicy WHERE PromoCode = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, promoCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DTODiscountPolicy promo = mapPromoCode(rs);
                    log.info("🔍 Found promo code: {}", promoCode);
                    return promo;
                }
            }
        } catch (SQLException e) {
            log.error("❌ Error finding promo code: {}", promoCode, e);
        }

        log.warn("⚠️ Promo code not found: {}", promoCode);
        return null;
    }

    /**
     * Get promo code by ID
     */
    public DTODiscountPolicy getPromoCodeById(int policyId) {
        String sql = "SELECT * FROM DiscountPolicy WHERE PolicyID = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, policyId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapPromoCode(rs);
                }
            }
        } catch (SQLException e) {
            log.error("❌ Error fetching promo code ID={}", policyId, e);
        }
        return null;
    }

    /**
     * Update promo code
     */
    public boolean updatePromoCode(DTODiscountPolicy promo) {
        String sql = """
            UPDATE DiscountPolicy SET
            PolicyName = ?, PromoCode = ?, Description = ?,
            DiscountPercent = ?, DiscountAmount = ?,
            MinPurchaseAmount = ?, MaxDiscountAmount = ?,
            UsageLimit = ?, UsedCount = ?,
            ApplicableToModels = ?, StartDate = ?, EndDate = ?,
            Status = ?
            WHERE PolicyID = ?
        """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, promo.getPolicyName());
            ps.setString(2, promo.getPromoCode());
            ps.setString(3, promo.getDescription());
            ps.setBigDecimal(4, promo.getDiscountPercent());
            ps.setBigDecimal(5, promo.getDiscountAmount());
            ps.setBigDecimal(6, promo.getMinPurchaseAmount());
            ps.setBigDecimal(7, promo.getMaxDiscountAmount());
            ps.setObject(8, promo.getUsageLimit(), Types.INTEGER);
            ps.setInt(9, promo.getUsedCount() != null ? promo.getUsedCount() : 0);
            ps.setString(10, promo.getApplicableToModels());
            ps.setDate(11, Date.valueOf(promo.getStartDate()));
            ps.setDate(12, promo.getEndDate() != null ? Date.valueOf(promo.getEndDate()) : null);
            ps.setString(13, promo.getStatus().toString());
            ps.setInt(14, promo.getPolicyID());

            int rows = ps.executeUpdate();
            if (rows > 0) {
                log.info("✅ Updated promo code ID={}", promo.getPolicyID());
                return true;
            }
        } catch (SQLException e) {
            log.error("❌ Error updating promo code ID={}", promo.getPolicyID(), e);
        }
        return false;
    }

    /**
     * Delete promo code
     */
    public boolean deletePromoCode(int policyId) {
        String sql = "DELETE FROM DiscountPolicy WHERE PolicyID = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, policyId);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                log.info("🗑️ Deleted promo code ID={}", policyId);
                return true;
            }
        } catch (SQLException e) {
            log.error("❌ Error deleting promo code ID={}", policyId, e);
        }
        return false;
    }

    /**
     * Increment usage count when promo code is used
     */
    public boolean incrementUsageCount(int policyId) {
        String sql = "UPDATE DiscountPolicy SET UsedCount = UsedCount + 1 WHERE PolicyID = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, policyId);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                log.info("📈 Incremented usage count for promo code ID={}", policyId);
                return true;
            }
        } catch (SQLException e) {
            log.error("❌ Error incrementing usage count for ID={}", policyId, e);
        }
        return false;
    }

    /**
     * Validate promo code for a purchase
     * Returns error message if invalid, null if valid
     */
    public String validatePromoCode(String promoCode, int modelId, BigDecimal purchaseAmount) {
        DTODiscountPolicy promo = findByPromoCode(promoCode);

        if (promo == null) {
            return "Mã giảm giá không tồn tại";
        }

        if (!promo.isValid()) {
            if (promo.getStatus() != DiscountPolicyStatus.ACTIVE) {
                return "Mã giảm giá không còn hiệu lực";
            }

            LocalDate now = LocalDate.now();
            if (promo.getStartDate() != null && now.isBefore(promo.getStartDate())) {
                return "Mã giảm giá chưa bắt đầu";
            }
            if (promo.getEndDate() != null && now.isAfter(promo.getEndDate())) {
                return "Mã giảm giá đã hết hạn";
            }

            if (promo.getUsageLimit() != null && promo.getUsedCount() != null
                && promo.getUsedCount() >= promo.getUsageLimit()) {
                return "Mã giảm giá đã hết lượt sử dụng";
            }
        }

        if (!promo.appliesToModel(modelId)) {
            return "Mã giảm giá không áp dụng cho mẫu xe này";
        }

        if (promo.getMinPurchaseAmount() != null
            && purchaseAmount.compareTo(promo.getMinPurchaseAmount()) < 0) {
            return "Giá trị đơn hàng chưa đạt mức tối thiểu để áp dụng mã giảm giá";
        }

        return null; // Valid!
    }

    /**
     * Search promo codes by name or code
     */
    public List<DTODiscountPolicy> searchPromoCodes(String keyword) {
        List<DTODiscountPolicy> list = new ArrayList<>();
        String sql = """
            SELECT * FROM DiscountPolicy
            WHERE PolicyName LIKE ? OR PromoCode LIKE ?
            ORDER BY CreatedAt DESC
        """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String pattern = "%" + keyword + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapPromoCode(rs));
                }
            }
            log.info("🔍 Found {} promo codes matching '{}'", list.size(), keyword);
        } catch (SQLException e) {
            log.error("❌ Error searching promo codes: {}", keyword, e);
        }
        return list;
    }

    /**
     * Map ResultSet to DTODiscountPolicy
     */
    private DTODiscountPolicy mapPromoCode(ResultSet rs) throws SQLException {
        DTODiscountPolicy promo = new DTODiscountPolicy();
        promo.setPolicyID(rs.getInt("PolicyID"));
        promo.setPolicyName(rs.getString("PolicyName"));
        promo.setPromoCode(rs.getString("PromoCode"));
        promo.setDescription(rs.getString("Description"));
        promo.setDiscountPercent(rs.getBigDecimal("DiscountPercent"));
        promo.setDiscountAmount(rs.getBigDecimal("DiscountAmount"));
        promo.setMinPurchaseAmount(rs.getBigDecimal("MinPurchaseAmount"));
        promo.setMaxDiscountAmount(rs.getBigDecimal("MaxDiscountAmount"));
        promo.setUsageLimit((Integer) rs.getObject("UsageLimit"));
        promo.setUsedCount(rs.getInt("UsedCount"));
        promo.setApplicableToModels(rs.getString("ApplicableToModels"));

        Date startDate = rs.getDate("StartDate");
        if (startDate != null) {
            promo.setStartDate(startDate.toLocalDate());
        }

        Date endDate = rs.getDate("EndDate");
        if (endDate != null) {
            promo.setEndDate(endDate.toLocalDate());
        }

        promo.setStatus(DiscountPolicyStatus.valueOf(rs.getString("Status")));
        promo.setCreationDate(rs.getTimestamp("CreatedAt"));
        promo.setCreatedBy(rs.getString("CreatedBy"));

        return promo;
    }
}

