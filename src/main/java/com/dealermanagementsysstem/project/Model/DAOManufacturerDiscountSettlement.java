package com.dealermanagementsysstem.project.Model;

import org.springframework.stereotype.Repository;
import utils.DBUtils;
import java.sql.*;
import java.util.*;
import java.math.BigDecimal;

@Repository
public class DAOManufacturerDiscountSettlement {

    public DTOManufacturerDiscountSettlement getBySaleOrderId(int saleOrderId) {
        String sql = "SELECT * FROM ManufacturerDiscountSettlement WHERE SaleOrderID=?";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, saleOrderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRowDto(rs);
            }
        } catch (SQLException e) { System.out.println("[SettlementDAO] getBySaleOrderId error " + e.getMessage()); }
        return null;
    }

    public DTOManufacturerDiscountSettlement create(int saleOrderId, int dealerId, BigDecimal totalManufacturerDiscount) {
        String sql = "INSERT INTO ManufacturerDiscountSettlement(SaleOrderID,DealerID,TotalManufacturerDiscount,ReimbursedAmount,Status) VALUES(?,?,?,?,?)";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, saleOrderId);
            ps.setInt(2, dealerId);
            ps.setBigDecimal(3, totalManufacturerDiscount!=null? totalManufacturerDiscount : BigDecimal.ZERO);
            ps.setBigDecimal(4, BigDecimal.ZERO);
            ps.setString(5, "PENDING");
            int rows = ps.executeUpdate();
            if (rows>0) {
                try (ResultSet rs = ps.getGeneratedKeys()) { if (rs.next()) {
                    int id = rs.getInt(1);
                    return getById(id);
                }}
            }
        } catch (SQLException e) { System.out.println("[SettlementDAO] create error " + e.getMessage()); }
        return null;
    }

    public DTOManufacturerDiscountSettlement getById(int settlementId) {
        String sql = "SELECT * FROM ManufacturerDiscountSettlement WHERE SettlementID=?";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, settlementId);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return mapRowDto(rs); }
        } catch (SQLException e) { System.out.println("[SettlementDAO] getById error " + e.getMessage()); }
        return null;
    }

    public DTOManufacturerDiscountSettlement updateStatus(int settlementId, String status, BigDecimal newReimbursedAmount, String notes) {
        // Read current values from DB
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal oldReimbursed = BigDecimal.ZERO;
        try (Connection c = DBUtils.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT TotalManufacturerDiscount, ReimbursedAmount FROM ManufacturerDiscountSettlement WHERE SettlementID=?")) {
            ps.setInt(1, settlementId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    total = rs.getBigDecimal(1);
                    oldReimbursed = rs.getBigDecimal(2);
                }
            }
        } catch (SQLException ignore) {}
        if (total == null) total = BigDecimal.ZERO;
        if (oldReimbursed == null) oldReimbursed = BigDecimal.ZERO;
        if (newReimbursedAmount == null) newReimbursedAmount = BigDecimal.ZERO;

        // IMPORTANT: newReimbursedAmount is the TOTAL reimbursed (not incremental)
        // It should represent the cumulative amount paid
        BigDecimal finalReimbursed = newReimbursedAmount;

        System.out.println("[SettlementDAO] updateStatus ID=" + settlementId +
                         " | Total=" + total +
                         " | OldReimbursed=" + oldReimbursed +
                         " | NewReimbursed=" + finalReimbursed +
                         " | Status=" + status);

        // Clamp to valid range
        if (finalReimbursed.compareTo(BigDecimal.ZERO) < 0) finalReimbursed = BigDecimal.ZERO;
        if (finalReimbursed.compareTo(total) > 0) finalReimbursed = total;

        // Canonical status determination based on reimbursed amount
        String canonical;
        if (finalReimbursed.compareTo(BigDecimal.ZERO) == 0) {
            canonical = "PENDING";
        } else if (finalReimbursed.compareTo(total) == 0) {
            canonical = "PAID";
        } else {
            canonical = "PARTIAL";
        }

        // If user manually chose a status, validate it matches the amount
        if (status != null && !status.isBlank()) {
            String upper = status.toUpperCase(java.util.Locale.ROOT);
            if (upper.equals("PAID") && finalReimbursed.compareTo(total) == 0) canonical = "PAID";
            else if (upper.equals("PENDING") && finalReimbursed.compareTo(BigDecimal.ZERO)==0) canonical = "PENDING";
            else if (upper.equals("PARTIAL") && finalReimbursed.compareTo(BigDecimal.ZERO)>0 && finalReimbursed.compareTo(total)<0) canonical = "PARTIAL";
        }

        String sql = "UPDATE ManufacturerDiscountSettlement SET Status=?, ReimbursedAmount=?, UpdatedAt=GETDATE(), PaidDate=CASE WHEN ? >= TotalManufacturerDiscount THEN GETDATE() ELSE NULL END, Notes=? WHERE SettlementID=?";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, canonical);
            ps.setBigDecimal(2, finalReimbursed);
            ps.setBigDecimal(3, finalReimbursed);
            ps.setString(4, notes);
            ps.setInt(5, settlementId);
            int ok = ps.executeUpdate();
            if (ok>0) return getById(settlementId);
        } catch (SQLException e) { System.out.println("[SettlementDAO] updateStatus error " + e.getMessage()); }
        return null;
    }

    /**
     * Add an incremental payment to the existing reimbursed amount
     * @param settlementId The settlement ID
     * @param payAmount The additional amount to pay (will be added to existing)
     * @param notes Optional notes
     * @return Updated settlement or null if failed
     */
    public DTOManufacturerDiscountSettlement partialPay(int settlementId, BigDecimal payAmount, String notes) {
        if (payAmount == null || payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return getById(settlementId);
        }

        DTOManufacturerDiscountSettlement before = getById(settlementId);
        if (before == null) return null;

        // Check if already fully paid
        BigDecimal total = before.getTotalManufacturerDiscount() != null ? before.getTotalManufacturerDiscount() : BigDecimal.ZERO;
        BigDecimal currentPaid = before.getReimbursedAmount() != null ? before.getReimbursedAmount() : BigDecimal.ZERO;
        if (currentPaid.compareTo(total) >= 0) {
            System.out.println("[ManufacturerSettlement] Already fully paid");
            return before;
        }

        String sql = "UPDATE ManufacturerDiscountSettlement SET " +
                "ReimbursedAmount = CASE WHEN ISNULL(ReimbursedAmount,0) + ? >= TotalManufacturerDiscount THEN TotalManufacturerDiscount ELSE ISNULL(ReimbursedAmount,0) + ? END, " +
                "Status = CASE WHEN ISNULL(ReimbursedAmount,0) + ? >= TotalManufacturerDiscount THEN 'PAID' " +
                "             WHEN ISNULL(ReimbursedAmount,0) + ? > 0 THEN 'PARTIAL' ELSE 'PENDING' END, " +
                "UpdatedAt = GETDATE(), " +
                "PaidDate = CASE WHEN ISNULL(ReimbursedAmount,0) + ? >= TotalManufacturerDiscount THEN GETDATE() ELSE PaidDate END, " +
                "Notes = ISNULL(?, Notes) " +
                "WHERE SettlementID = ?";

        try (Connection c = DBUtils.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBigDecimal(1, payAmount);
            ps.setBigDecimal(2, payAmount);
            ps.setBigDecimal(3, payAmount);
            ps.setBigDecimal(4, payAmount);
            ps.setBigDecimal(5, payAmount);
            ps.setString(6, notes);
            ps.setInt(7, settlementId);

            int ok = ps.executeUpdate();
            if (ok > 0) {
                DTOManufacturerDiscountSettlement after = getById(settlementId);
                System.out.println("[ManufacturerSettlement] partialPay ID=" + settlementId +
                                 " beforePaid=" + currentPaid +
                                 " +pay=" + payAmount +
                                 " => afterPaid=" + after.getReimbursedAmount() +
                                 " status=" + after.getStatus());
                return after;
            }
        } catch (SQLException e) {
            System.out.println("[ManufacturerSettlement] partialPay error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Pay the full remaining amount
     * @param settlementId The settlement ID
     * @param notes Optional notes
     * @return Updated settlement or null if failed
     */
    public DTOManufacturerDiscountSettlement payAll(int settlementId, String notes) {
        System.out.println("[ManufacturerSettlement] payAll starting - ID: " + settlementId);
        DTOManufacturerDiscountSettlement current = getById(settlementId);
        if (current == null) {
            System.out.println("[ManufacturerSettlement] Settlement not found");
            return null;
        }

        BigDecimal remaining = current.getOutstanding();
        if (remaining == null || remaining.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("[ManufacturerSettlement] Already fully paid");
            return current;
        }

        System.out.println("[ManufacturerSettlement] Outstanding: " + remaining +
                         ", Total: " + current.getTotalManufacturerDiscount() +
                         ", Currently Paid: " + current.getReimbursedAmount());

        DTOManufacturerDiscountSettlement result = partialPay(settlementId, remaining, notes);
        System.out.println("[ManufacturerSettlement] payAll completed");
        return result;
    }

    private DTOManufacturerDiscountSettlement mapRowDto(ResultSet rs) throws SQLException {
        DTOManufacturerDiscountSettlement dto = new DTOManufacturerDiscountSettlement();
        dto.setSettlementID(rs.getInt("SettlementID"));
        dto.setSaleOrderID(rs.getInt("SaleOrderID"));
        dto.setDealerID(rs.getInt("DealerID"));
        dto.setTotalManufacturerDiscount(rs.getBigDecimal("TotalManufacturerDiscount"));
        dto.setReimbursedAmount(rs.getBigDecimal("ReimbursedAmount"));
        dto.setStatus(rs.getString("Status"));
        dto.setCreatedAt(rs.getTimestamp("CreatedAt"));
        dto.setUpdatedAt(rs.getTimestamp("UpdatedAt"));
        dto.setPaidDate(rs.getTimestamp("PaidDate"));
        dto.setNotes(rs.getString("Notes"));
        return dto;
    }
}
