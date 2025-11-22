package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAODiscountPolicy - DEPRECATED
 *
 * This DAO is kept for backward compatibility with old discount policy structure.
 * For new promo code functionality, use DAOPromoCode instead.
 *
 * @deprecated Use {@link DAOPromoCode} for customer promotional codes
 */
@Repository
@Deprecated
@SuppressWarnings({"SqlDialectInspection", "SqlNoDataSourceInspection", "deprecation"})
public class DAODiscountPolicy {

    private static final Logger log = LoggerFactory.getLogger(DAODiscountPolicy.class);

    // ===== HELPER METHODS =====

    private BigDecimal getBigDecimalSafely(ResultSet rs, String columnName) {
        try {
            return rs.getBigDecimal(columnName);
        } catch (SQLException e) {
            return null;
        }
    }

    private String getStringSafely(ResultSet rs, String columnName) {
        try {
            return rs.getString(columnName);
        } catch (SQLException e) {
            return null;
        }
    }

    private Integer getIntSafely(ResultSet rs, String columnName) {
        try {
            int value = rs.getInt(columnName);
            return rs.wasNull() ? null : value;
        } catch (SQLException e) {
            return null;
        }
    }

    // ===== CRUD METHODS (DEPRECATED) =====

    /**
     * Create Discount Policy (DEPRECATED)
     * @deprecated Use DAOPromoCode.createPromoCode() instead
     */
    @Deprecated
    public boolean createDiscountPolicy(DTODiscountPolicy dto) {
        log.warn("Using deprecated createDiscountPolicy(). Use DAOPromoCode for new promo codes.");

        String sql = "INSERT INTO DiscountPolicy " +
                "(PolicyName, PromoCode, Description, DiscountPercent, DiscountAmount, " +
                "MinPurchaseAmount, MaxDiscountAmount, UsageLimit, UsedCount, " +
                "ApplicableToModels, StartDate, EndDate, Status, CreatedAt, CreatedBy, " +
                "DealerID, HangPercent, DailyPercent, LevelID) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, GETDATE(), ?, ?, ?, ?, ?)";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, dto.getPolicyName());
            ps.setString(2, dto.getPromoCode());
            ps.setString(3, dto.getDescription());
            ps.setBigDecimal(4, dto.getDiscountPercent());
            ps.setBigDecimal(5, dto.getDiscountAmount());
            ps.setBigDecimal(6, dto.getMinPurchaseAmount());
            ps.setBigDecimal(7, dto.getMaxDiscountAmount());
            ps.setObject(8, dto.getUsageLimit(), Types.INTEGER);
            ps.setInt(9, dto.getUsedCount() != null ? dto.getUsedCount() : 0);
            ps.setString(10, dto.getApplicableToModels());
            ps.setDate(11, dto.getStartDate() != null ? Date.valueOf(dto.getStartDate()) : null);
            ps.setDate(12, dto.getEndDate() != null ? Date.valueOf(dto.getEndDate()) : null);
            ps.setString(13, dto.getStatus() != null ? dto.getStatus().toString() : "ACTIVE");
            ps.setString(14, dto.getCreatedBy());

            // Deprecated fields (nullable)
            ps.setObject(15, dto.getDealerID(), Types.INTEGER);
            ps.setBigDecimal(16, dto.getHangPercent());
            ps.setBigDecimal(17, dto.getDailyPercent());
            ps.setObject(18, dto.getLevelID(), Types.INTEGER);

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int newPolicyId = rs.getInt(1);
                        log.info("Policy created with ID: {}", newPolicyId);
                        return true;
                    }
                }
            }

        } catch (SQLException e) {
            log.error("Error creating Discount Policy", e);
        }
        return false;
    }

    /**
     * Get all policies (DEPRECATED)
     * @deprecated Use DAOPromoCode.getAllPromoCodes() instead
     */
    @Deprecated
    public List<DTODiscountPolicy> getAllPolicies() {
        List<DTODiscountPolicy> list = new ArrayList<>();
        String sql = "SELECT * FROM DiscountPolicy ORDER BY CreatedAt DESC";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToDTO(rs));
            }

        } catch (SQLException e) {
            log.error("Error fetching all policies", e);
        }
        return list;
    }

    /**
     * Search by policy name (DEPRECATED)
     * @deprecated Use DAOPromoCode.searchPromoCodes() instead
     */
    @Deprecated
    public List<DTODiscountPolicy> searchPolicyByName(String keyword) {
        List<DTODiscountPolicy> list = new ArrayList<>();
        String sql = "SELECT * FROM DiscountPolicy WHERE PolicyName LIKE ? OR PromoCode LIKE ? ORDER BY CreatedAt DESC";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String pattern = "%" + keyword + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToDTO(rs));
                }
            }

        } catch (SQLException e) {
            log.error("Error searching policies by name: {}", keyword, e);
        }
        return list;
    }

    /**
     * Get policy by ID (DEPRECATED)
     * @deprecated Use DAOPromoCode.getPromoCodeById() instead
     */
    @Deprecated
    public DTODiscountPolicy getPolicyById(int policyId) {
        String sql = "SELECT * FROM DiscountPolicy WHERE PolicyID = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, policyId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToDTO(rs);
                }
            }

        } catch (SQLException e) {
            log.error("Error getting policy by ID: {}", policyId, e);
        }
        return null;
    }

    /**
     * Update policy (DEPRECATED)
     * @deprecated Use DAOPromoCode.updatePromoCode() instead
     */
    @Deprecated
    public boolean updateDiscountPolicy(DTODiscountPolicy dto) {
        String sql = "UPDATE DiscountPolicy SET " +
                "PolicyName = ?, PromoCode = ?, Description = ?, " +
                "DiscountPercent = ?, DiscountAmount = ?, " +
                "MinPurchaseAmount = ?, MaxDiscountAmount = ?, " +
                "UsageLimit = ?, UsedCount = ?, ApplicableToModels = ?, " +
                "StartDate = ?, EndDate = ?, Status = ? " +
                "WHERE PolicyID = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, dto.getPolicyName());
            ps.setString(2, dto.getPromoCode());
            ps.setString(3, dto.getDescription());
            ps.setBigDecimal(4, dto.getDiscountPercent());
            ps.setBigDecimal(5, dto.getDiscountAmount());
            ps.setBigDecimal(6, dto.getMinPurchaseAmount());
            ps.setBigDecimal(7, dto.getMaxDiscountAmount());
            ps.setObject(8, dto.getUsageLimit(), Types.INTEGER);
            ps.setInt(9, dto.getUsedCount() != null ? dto.getUsedCount() : 0);
            ps.setString(10, dto.getApplicableToModels());
            ps.setDate(11, dto.getStartDate() != null ? Date.valueOf(dto.getStartDate()) : null);
            ps.setDate(12, dto.getEndDate() != null ? Date.valueOf(dto.getEndDate()) : null);
            ps.setString(13, dto.getStatus() != null ? dto.getStatus().toString() : "ACTIVE");
            ps.setInt(14, dto.getPolicyID());

            int rows = ps.executeUpdate();
            if (rows > 0) {
                log.info("Updated policy ID: {}", dto.getPolicyID());
                return true;
            }

        } catch (SQLException e) {
            log.error("Error updating policy", e);
        }
        return false;
    }

    /**
     * Delete policy (DEPRECATED)
     * @deprecated Use DAOPromoCode.deletePromoCode() instead
     */
    @Deprecated
    public boolean deleteDiscountPolicy(int policyId) {
        String sql = "DELETE FROM DiscountPolicy WHERE PolicyID = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, policyId);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                log.info("Deleted policy ID: {}", policyId);
                return true;
            }

        } catch (SQLException e) {
            log.error("Error deleting policy ID: {}", policyId, e);
        }
        return false;
    }

    /**
     * Get policies by dealer ID (DEPRECATED - Old structure)
     * @deprecated Old structure, not recommended
     */
    @Deprecated
    public List<DTODiscountPolicy> getPoliciesByDealerId(int dealerId) {
        List<DTODiscountPolicy> list = new ArrayList<>();
        String sql = "SELECT * FROM DiscountPolicy WHERE DealerID = ? ORDER BY CreatedAt DESC";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, dealerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToDTO(rs));
                }
            }

        } catch (SQLException e) {
            log.error("Error fetching policies by dealer ID: {}", dealerId, e);
        }
        return list;
    }

    /**
     * Search by name and dealer (DEPRECATED - Old structure)
     * @deprecated Old structure, not recommended
     */
    @Deprecated
    public List<DTODiscountPolicy> searchPolicyByNameAndDealer(String keyword, int dealerId) {
        List<DTODiscountPolicy> list = new ArrayList<>();
        String sql = "SELECT * FROM DiscountPolicy WHERE (PolicyName LIKE ? OR PromoCode LIKE ?) AND DealerID = ? ORDER BY CreatedAt DESC";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String pattern = "%" + keyword + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setInt(3, dealerId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToDTO(rs));
                }
            }

        } catch (SQLException e) {
            log.error("Error searching policies by name and dealer", e);
        }
        return list;
    }

    // ===== MAPPER =====

    /**
     * Map ResultSet to DTODiscountPolicy
     */
    private DTODiscountPolicy mapResultSetToDTO(ResultSet rs) throws SQLException {
        DTODiscountPolicy dto = new DTODiscountPolicy();

        // Core fields
        dto.setPolicyID(rs.getInt("PolicyID"));
        dto.setPolicyName(getStringSafely(rs, "PolicyName"));
        dto.setPromoCode(getStringSafely(rs, "PromoCode"));
        dto.setDescription(getStringSafely(rs, "Description"));
        dto.setDiscountPercent(getBigDecimalSafely(rs, "DiscountPercent"));
        dto.setDiscountAmount(getBigDecimalSafely(rs, "DiscountAmount"));
        dto.setMinPurchaseAmount(getBigDecimalSafely(rs, "MinPurchaseAmount"));
        dto.setMaxDiscountAmount(getBigDecimalSafely(rs, "MaxDiscountAmount"));
        dto.setUsageLimit(getIntSafely(rs, "UsageLimit"));
        dto.setUsedCount(getIntSafely(rs, "UsedCount"));
        dto.setApplicableToModels(getStringSafely(rs, "ApplicableToModels"));

        // Dates
        Date startDate = rs.getDate("StartDate");
        if (startDate != null) {
            dto.setStartDate(startDate.toLocalDate());
        }

        Date endDate = rs.getDate("EndDate");
        if (endDate != null) {
            dto.setEndDate(endDate.toLocalDate());
        }

        // Status
        String status = getStringSafely(rs, "Status");
        if (status != null) {
            try {
                dto.setStatus(DiscountPolicyStatus.valueOf(status.toUpperCase()));
            } catch (IllegalArgumentException e) {
                dto.setStatus(DiscountPolicyStatus.ACTIVE);
            }
        }

        dto.setCreationDate(rs.getTimestamp("CreatedAt"));
        dto.setCreatedBy(getStringSafely(rs, "CreatedBy"));

        // Deprecated fields (keep for backward compatibility)
        dto.setHangPercent(getBigDecimalSafely(rs, "HangPercent"));
        dto.setDailyPercent(getBigDecimalSafely(rs, "DailyPercent"));
        dto.setDealerID(getIntSafely(rs, "DealerID"));
        dto.setLevelID(getIntSafely(rs, "LevelID"));

        return dto;
    }
}

