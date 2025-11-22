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
 * EN: Data Access Object for managing customer promotional discount codes ("Promo Codes").
 * <p>
 * Provides CRUD operations, search, usage counting, and business validation logic for promo
 * codes stored in the {@code DiscountPolicy} table. This class supersedes the legacy usage of
 * DiscountPolicy entries for manufacturer/dealer commission splits; here the records strictly
 * represent customer-facing discount instruments (e.g. "SUMMER2024", "NEWCAR15").
 * </p>
 * <p>
 * Typical usage flow:
 * 1. {@link #findByPromoCode(String)} to load the code the customer entered.
 * 2. {@link #validatePromoCode(String, int, BigDecimal)} to check applicability & business rules.
 * 3. Apply discount (percent or fixed) obeying max discount cap & purchase constraints.
 * 4. Persist order; only then call {@link #incrementUsageCount(int)} to avoid counting failed attempts.
 * </p>
 * <p>
 * Thread-safety: Methods obtain fresh JDBC connections via {@link DBUtils#getConnection()} and do
 * not share mutable state. Each individual call is safe in a Spring singleton context. Note that
 * validation + increment is not atomic: concurrent checkouts could pass validation simultaneously
 * and exceed intended {@code UsageLimit}. To harden against race conditions you may later adopt a
 * single UPDATE with predicate (e.g. WHERE UsedCount < UsageLimit) or a transaction with locking.
 * </p>
 * <p>
 * Glossary (EN -> VI):
 * - Promo Code: Customer discount code -> Mã giảm giá khách hàng.
 * - Usage Limit (nullable): Max allowed uses; NULL means unlimited -> Giới hạn sử dụng (NULL = không giới hạn).
 * - Used Count: Current number of successful applications -> Số lượt đã dùng.
 * - Applicable Models: Comma-separated model IDs -> Danh sách ID mẫu xe áp dụng (phân tách bằng dấu phẩy).
 * - Discount Percent / Amount: Percentage vs fixed absolute discount -> Phần trăm / số tiền giảm cố định.
 * - Max Discount Amount: Cap for computed discount -> Giới hạn số tiền giảm tối đa.
 * - Active window: StartDate .. EndDate (EndDate NULL = open-ended) -> Khoảng thời gian hiệu lực.
 * </p>
 * <p>
 * VI: Lớp DAO quản lý các mã giảm giá cho khách hàng. Cung cấp các chức năng tạo, sửa, xóa,
 * tìm kiếm, kiểm tra điều kiện áp dụng và tăng số lượt sử dụng. Thay thế logic cũ dùng bảng
 * DiscountPolicy cho việc chia hoa hồng giữa hãng và đại lý; hiện các bản ghi chỉ đại diện
 * cho công cụ giảm giá dành cho khách hàng.
 * </p>
 * <p>
 * Quy trình sử dụng điển hình:
 * 1. Gọi {@link #findByPromoCode(String)} để lấy thông tin mã nhập vào.
 * 2. Gọi {@link #validatePromoCode(String, int, BigDecimal)} để kiểm tra hợp lệ.
 * 3. Áp dụng giảm giá theo phần trăm hoặc số tiền, tuân thủ giới hạn tối đa.
 * 4. Sau khi đơn hàng thành công mới gọi {@link #incrementUsageCount(int)} để tránh tăng sai.
 * </p>
 */
@Repository
@SuppressWarnings({"SqlDialectInspection", "SqlNoDataSourceInspection", "unused"})
public class DAOPromoCode {

    private static final Logger log = LoggerFactory.getLogger(DAOPromoCode.class);

    /**
     * EN: Create a new promo code record.
     * Persist all supplied business fields; {@code CreatedAt} is assigned by DB server time (GETDATE()).
     * Returns generated primary key (PolicyID) or -1 on failure.
     * VI: Tạo mới một mã giảm giá. Trả về ID sinh ra hoặc -1 nếu lỗi.
     *
     * @param promo DTO containing promo code details (validated by caller layer)
     * @return generated PolicyID or -1 if insert failed
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
            ps.setObject(8, promo.getUsageLimit(), Types.INTEGER); // NULL => unlimited usage
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
                        log.info("Created promo code: {} (ID={})", promo.getPromoCode(), promoId);
                        return promoId;
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Error creating promo code: {}", promo.getPromoCode(), e);
        }
        return -1;
    }

    /**
     * EN: Retrieve all promo codes currently active & usable.
     * Filters by Status=ACTIVE, within date window, usage limit not exceeded.
     * VI: Lấy danh sách tất cả mã giảm giá đang hoạt động và còn lượt dùng.
     *
     * @return list of active promo code DTOs (empty if none or error)
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
        """; // Date comparisons done in DB (assumes server timezone authoritative)

        try (Connection conn = DBUtils.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapPromoCode(rs));
            }
            log.info("Loaded {} active promo codes", list.size());
        } catch (SQLException e) {
            log.error("Error fetching active promo codes", e);
        }
        return list;
    }

    /**
     * EN: Retrieve all promo codes (administrative view). No filtering.
     * VI: Lấy toàn bộ mã giảm giá (cho quản trị), không lọc.
     *
     * @return list of all promo codes
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
            log.info("Loaded {} total promo codes", list.size());
        } catch (SQLException e) {
            log.error("Error fetching all promo codes", e);
        }
        return list;
    }

    /**
     * EN: Find a promo code by its string value (exact match).
     * Returns null if not found or on error.
     * VI: Tìm mã giảm giá theo chuỗi (khớp chính xác). Trả về null nếu không thấy.
     *
     * @param promoCode raw code entered by customer
     * @return DTO or null
     */
    public DTODiscountPolicy findByPromoCode(String promoCode) {
        String sql = "SELECT * FROM DiscountPolicy WHERE PromoCode = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, promoCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DTODiscountPolicy promo = mapPromoCode(rs);
                    log.info("Found promo code: {}", promoCode);
                    return promo;
                }
            }
        } catch (SQLException e) {
            log.error("Error finding promo code: {}", promoCode, e);
        }

        log.warn("Promo code not found: {}", promoCode);
        return null;
    }

    /**
     * EN: Fetch promo code by primary key ID.
     * VI: Lấy mã giảm giá theo ID khóa chính.
     *
     * @param policyId database primary key
     * @return DTO or null if not found
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
            log.error("Error fetching promo code ID={}", policyId, e);
        }
        return null;
    }

    /**
     * EN: Update all editable fields of an existing promo code.
     * VI: Cập nhật toàn bộ trường cho một mã giảm giá đã tồn tại.
     *
     * @param promo DTO with updated values (must include PolicyID)
     * @return true on success, false otherwise
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
            ps.setObject(8, promo.getUsageLimit(), Types.INTEGER); // keep NULL if unlimited
            ps.setInt(9, promo.getUsedCount() != null ? promo.getUsedCount() : 0);
            ps.setString(10, promo.getApplicableToModels());
            ps.setDate(11, Date.valueOf(promo.getStartDate()));
            ps.setDate(12, promo.getEndDate() != null ? Date.valueOf(promo.getEndDate()) : null);
            ps.setString(13, promo.getStatus().toString());
            ps.setInt(14, promo.getPolicyID());

            int rows = ps.executeUpdate();
            if (rows > 0) {
                log.info("Updated promo code ID={}", promo.getPolicyID());
                return true;
            }
        } catch (SQLException e) {
            log.error("Error updating promo code ID={}", promo.getPolicyID(), e);
        }
        return false;
    }

    /**
     * EN: Delete a promo code by ID.
     * VI: Xóa mã giảm giá theo ID.
     *
     * @param policyId primary key
     * @return true if a record was removed
     */
    public boolean deletePromoCode(int policyId) {
        String sql = "DELETE FROM DiscountPolicy WHERE PolicyID = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, policyId);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                log.info("Deleted promo code ID={}", policyId);
                return true;
            }
        } catch (SQLException e) {
            log.error("Error deleting promo code ID={}", policyId, e);
        }
        return false;
    }

    /**
     * EN: Increment the UsedCount field by 1 after a successful application.
     * NOT atomic with validation; race conditions possible under high concurrency.
     * VI: Tăng số lượt sử dụng sau khi áp dụng thành công (không bảo đảm tránh race condition).
     *
     * @param policyId promo code primary key
     * @return true if update affected a row
     */
    public boolean incrementUsageCount(int policyId) {
        String sql = "UPDATE DiscountPolicy SET UsedCount = UsedCount + 1 WHERE PolicyID = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, policyId);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                log.info("Incremented usage count for promo code ID={}", policyId);
                return true;
            }
        } catch (SQLException e) {
            log.error("Error incrementing usage count for ID={}", policyId, e);
        }
        return false;
    }

    /**
     * EN: Validate a promo code for a specific purchase context.
     * Returns a localized Vietnamese error message if invalid, or null if valid.
     * Validation order: existence -> status/date window/usage limit -> model applicability -> min purchase.
     * VI: Kiểm tra mã giảm giá trong ngữ cảnh đơn hàng. Trả về thông báo lỗi (tiếng Việt) hoặc null nếu hợp lệ.
     *
     * @param promoCode code entered
     * @param modelId vehicle model ID the customer is purchasing
     * @param purchaseAmount gross purchase amount before discount
     * @return Vietnamese error message, or null if valid
     */
    public String validatePromoCode(String promoCode, int modelId, BigDecimal purchaseAmount) {
        DTODiscountPolicy promo = findByPromoCode(promoCode);

        if (promo == null) {
            return "Mã giảm giá không tồn tại";
        }

        // Delegate deeper validity checks to DTO's isValid but provide detailed messages for UI.
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
     * EN: Search promo codes by partial match on PolicyName or PromoCode.
     * VI: Tìm kiếm mã giảm giá theo chuỗi (LIKE) trên tên chính sách hoặc mã.
     *
     * @param keyword substring to search
     * @return list of matching promo codes
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
            log.info("Found {} promo codes matching '{}'", list.size(), keyword);
        } catch (SQLException e) {
            log.error("Error searching promo codes: {}", keyword, e);
        }
        return list;
    }

    /**
     * EN: Map a JDBC {@link ResultSet} row into a {@link DTODiscountPolicy} DTO.
     * Handles nullable date & integer fields, converts SQL Date to LocalDate.
     * VI: Ánh xạ một dòng {@link ResultSet} sang đối tượng DTO. Xử lý giá trị null cho ngày & giới hạn.
     *
     * @param rs positioned ResultSet
     * @return populated DTO
     * @throws SQLException if column access fails
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
        promo.setUsageLimit((Integer) rs.getObject("UsageLimit")); // NULL preserved
        promo.setUsedCount(rs.getInt("UsedCount")); // SQL getInt returns 0 if NULL; business expects 0 default
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
