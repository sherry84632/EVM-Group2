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

    public DTOManufacturerDiscountSettlement updateStatus(int settlementId, String status, BigDecimal reimbursedAmount, String notes) {
        BigDecimal total = BigDecimal.ZERO;
        try (Connection c = DBUtils.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT TotalManufacturerDiscount FROM ManufacturerDiscountSettlement WHERE SettlementID=?")) {
            ps.setInt(1, settlementId);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) total = rs.getBigDecimal(1); }
        } catch (SQLException ignore) {}
        if (total == null) total = BigDecimal.ZERO;
        if (reimbursedAmount == null) reimbursedAmount = BigDecimal.ZERO;
        if (reimbursedAmount.compareTo(BigDecimal.ZERO) < 0) reimbursedAmount = BigDecimal.ZERO;
        if (reimbursedAmount.compareTo(total) > 0) reimbursedAmount = total; // clamp
        // Canonical status determination (override manual inconsistencies)
        String canonical;
        if (reimbursedAmount.compareTo(BigDecimal.ZERO) == 0) {
            canonical = "PENDING";
        } else if (reimbursedAmount.compareTo(total) == 0) {
            canonical = "PAID";
        } else {
            canonical = "PARTIAL";
        }
        // If user manually chose a status, allow PAID only when fully reimbursed; ignore invalid combos
        if (status != null && !status.isBlank()) {
            String upper = status.toUpperCase(java.util.Locale.ROOT);
            if (upper.equals("PAID") && reimbursedAmount.compareTo(total) == 0) canonical = "PAID"; // valid
            else if (upper.equals("PENDING") && reimbursedAmount.compareTo(BigDecimal.ZERO)==0) canonical = "PENDING"; // valid zero
            else if (upper.equals("PARTIAL") && reimbursedAmount.compareTo(BigDecimal.ZERO)>0 && reimbursedAmount.compareTo(total)<0) canonical = "PARTIAL"; // valid partial
            // else canonical already selected
        }
        String sql = "UPDATE ManufacturerDiscountSettlement SET Status=?, ReimbursedAmount=?, UpdatedAt=GETDATE(), PaidDate=CASE WHEN ? >= TotalManufacturerDiscount THEN GETDATE() ELSE NULL END, Notes=? WHERE SettlementID=?";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, canonical);
            ps.setBigDecimal(2, reimbursedAmount);
            ps.setBigDecimal(3, reimbursedAmount);
            ps.setString(4, notes);
            ps.setInt(5, settlementId);
            int ok = ps.executeUpdate();
            if (ok>0) return getById(settlementId);
        } catch (SQLException e) { System.out.println("[SettlementDAO] updateStatus error " + e.getMessage()); }
        return null;
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
