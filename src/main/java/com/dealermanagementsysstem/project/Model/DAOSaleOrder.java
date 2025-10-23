package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;
import java.sql.*;
import java.util.*;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DAOSaleOrder {

    private static final Logger log = LoggerFactory.getLogger(DAOSaleOrder.class);

    // ======================================================
    // 1️⃣  TẠO SALE ORDER MỚI (thêm validation)
    // ======================================================
    public boolean createSaleOrder(DTOSaleOrder saleOrder) {
        if (saleOrder == null
                || saleOrder.getCustomer() == null
                || saleOrder.getDealer() == null
                || saleOrder.getStaff() == null
                || saleOrder.getStaff().getStaffID() <= 0
                || saleOrder.getDetail() == null
                || saleOrder.getDetail().isEmpty()) {
            log.warn("createSaleOrder validation failed (null or missing mandatory fields)");
            return false;
        }

        // Check every detail has VIN, price, qty
        for (DTOSaleOrderDetail d : saleOrder.getDetail()) {
            if (d.getVehicle() == null || d.getVehicle().getVIN() == null) {
                log.warn("Detail missing VIN -> abort");
                return false;
            }
            if (d.getPrice() == null) {
                log.warn("Detail missing price -> abort");
                return false;
            }
            if (d.getQuantity() <= 0) {
                log.warn("Detail invalid quantity -> abort");
                return false;
            }
        }

        String sqlOrder = "INSERT INTO SaleOrder (CustomerID, DealerID, StaffID, CreatedAt, Status, TotalQuantity, TotalAmount) "
                + "VALUES (?, ?, ?, GETDATE(), ?, ?, ?)";
        String sqlDetail = "INSERT INTO SaleOrderDetail (SaleOrderID, VIN, Price, PolicyID, Quantity) "
                + "VALUES (?, ?, ?, ?, ?)"; // Không có ColorID ở đây

        Connection conn = null;
        PreparedStatement psOrder = null;
        PreparedStatement psDetail = null;
        ResultSet rs = null;

        try {
            conn = DBUtils.getConnection();
            conn.setAutoCommit(false);

            int totalQuantity = saleOrder.getDetail().stream().mapToInt(DTOSaleOrderDetail::getQuantity).sum();
            BigDecimal totalAmount = saleOrder.getDetail().stream()
                    .map(d -> d.getPrice().multiply(BigDecimal.valueOf(d.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            psOrder = conn.prepareStatement(sqlOrder, Statement.RETURN_GENERATED_KEYS);
            psOrder.setInt(1, saleOrder.getCustomer().getCustomerID());
            psOrder.setInt(2, saleOrder.getDealer().getDealerID());
            psOrder.setInt(3, saleOrder.getStaff().getStaffID());
            psOrder.setString(4, saleOrder.getStatus() == null ? "Pending" : saleOrder.getStatus());
            psOrder.setInt(5, totalQuantity);
            psOrder.setBigDecimal(6, totalAmount);
            int affectedMain = psOrder.executeUpdate();
            if (affectedMain != 1) {
                log.error("Insert SaleOrder failed (affected={}) -> rollback", affectedMain);
                conn.rollback();
                return false;
            }

            rs = psOrder.getGeneratedKeys();
            int saleOrderID = 0;
            if (rs.next()) {
                saleOrderID = rs.getInt(1);
            } else {
                log.error("No generated key returned -> rollback");
                conn.rollback();
                return false;
            }

            saleOrder.setSaleOrderID(saleOrderID);
            saleOrder.setTotalQuantity(totalQuantity);
            saleOrder.setTotalAmount(totalAmount);

            psDetail = conn.prepareStatement(sqlDetail);
            for (DTOSaleOrderDetail detail : saleOrder.getDetail()) {
                psDetail.setInt(1, saleOrderID);
                psDetail.setString(2, detail.getVehicle().getVIN());
                psDetail.setBigDecimal(3, detail.getPrice());
                psDetail.setInt(4, saleOrder.getDealer().getPolicyID());
                psDetail.setInt(5, detail.getQuantity());
                psDetail.addBatch();
            }
            psDetail.executeBatch();

            conn.commit();
            log.info("SaleOrder created id={}", saleOrderID);
            return true;

        } catch (SQLException e) {
            log.error("Error creating SaleOrder - rollback", e);
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { log.error("Rollback failed", ex); }
        } finally {
            try {
                if (rs != null) rs.close();
                if (psOrder != null) psOrder.close();
                if (psDetail != null) psDetail.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) { log.error("Close resource failed", ex); }
        }
        return false;
    }

    // (Các hàm getAllSaleOrders, getSaleOrderById, getSaleOrderDetails giữ nguyên...)

    // ======================================================
    // 5️⃣  CẬP NHẬT FULL SALE ORDER (header + thay toàn bộ detail)
    // ======================================================
    public boolean updateSaleOrder(DTOSaleOrder saleOrder) {
        if (saleOrder == null
                || saleOrder.getSaleOrderID() <= 0
                || saleOrder.getCustomer() == null
                || saleOrder.getDealer() == null
                || saleOrder.getStaff() == null
                || saleOrder.getStaff().getStaffID() <= 0
                || saleOrder.getDetail() == null
                || saleOrder.getDetail().isEmpty()) {
            log.warn("updateSaleOrder validation failed");
            return false;
        }
        for (DTOSaleOrderDetail d : saleOrder.getDetail()) {
            if (d.getVehicle() == null || d.getVehicle().getVIN() == null) {
                log.warn("Detail missing VIN");
                return false;
            }
            if (d.getPrice() == null || d.getQuantity() <= 0) {
                log.warn("Detail invalid price/qty");
                return false;
            }
        }

        int totalQty = saleOrder.getDetail().stream().mapToInt(DTOSaleOrderDetail::getQuantity).sum();
        BigDecimal totalAmt = saleOrder.getDetail().stream()
                .map(d -> d.getPrice().multiply(BigDecimal.valueOf(d.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String sqlUpdate = "UPDATE SaleOrder SET CustomerID=?, DealerID=?, StaffID=?, Status=?, TotalQuantity=?, TotalAmount=? WHERE SaleOrderID=?";
        String sqlDeleteDetails = "DELETE FROM SaleOrderDetail WHERE SaleOrderID=?";
        String sqlInsertDetail = "INSERT INTO SaleOrderDetail (SaleOrderID, VIN, Price, PolicyID, Quantity) VALUES (?,?,?,?,?)";

        Connection conn = null;
        PreparedStatement psUp = null;
        PreparedStatement psDel = null;
        PreparedStatement psIns = null;

        try {
            conn = DBUtils.getConnection();
            conn.setAutoCommit(false);

            psUp = conn.prepareStatement(sqlUpdate);
            psUp.setInt(1, saleOrder.getCustomer().getCustomerID());
            psUp.setInt(2, saleOrder.getDealer().getDealerID());
            psUp.setInt(3, saleOrder.getStaff().getStaffID());
            psUp.setString(4, saleOrder.getStatus() == null ? "Pending" : saleOrder.getStatus());
            psUp.setInt(5, totalQty);
            psUp.setBigDecimal(6, totalAmt);
            psUp.setInt(7, saleOrder.getSaleOrderID());
            if (psUp.executeUpdate() != 1) {
                log.warn("updateSaleOrder header not affected -> rollback");
                conn.rollback();
                return false;
            }

            psDel = conn.prepareStatement(sqlDeleteDetails);
            psDel.setInt(1, saleOrder.getSaleOrderID());
            psDel.executeUpdate();

            psIns = conn.prepareStatement(sqlInsertDetail);
            for (DTOSaleOrderDetail d : saleOrder.getDetail()) {
                psIns.setInt(1, saleOrder.getSaleOrderID());
                psIns.setString(2, d.getVehicle().getVIN());
                psIns.setBigDecimal(3, d.getPrice());
                psIns.setInt(4, saleOrder.getDealer().getPolicyID());
                psIns.setInt(5, d.getQuantity());
                psIns.addBatch();
            }
            psIns.executeBatch();

            saleOrder.setTotalQuantity(totalQty);
            saleOrder.setTotalAmount(totalAmt);

            conn.commit();
            log.info("SaleOrder updated id={}", saleOrder.getSaleOrderID());
            return true;

        } catch (SQLException e) {
            log.error("Error updating SaleOrder id={}, rollback", saleOrder.getSaleOrderID(), e);
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { log.error("Rollback failed", ex); }
        } finally {
            try {
                if (psIns != null) psIns.close();
                if (psDel != null) psDel.close();
                if (psUp != null) psUp.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) { log.error("Close resource failed", ex); }
        }
        return false;
    }

    // ======================================================
    // 6️⃣  CHỈ CẬP NHẬT STATUS
    // ======================================================
    public boolean updateSaleOrderStatus(int saleOrderID, String newStatus) {
        if (saleOrderID <= 0 || newStatus == null || newStatus.isBlank()) {
            return false;
        }
        String sql = "UPDATE SaleOrder SET Status=? WHERE SaleOrderID=?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, saleOrderID);
            boolean ok = ps.executeUpdate() == 1;
            if (!ok) log.warn("Status update not affected id={}", saleOrderID);
            return ok;
        } catch (SQLException e) {
            log.error("Error updating status id={}", saleOrderID, e);
            return false;
        }
    }

    // (Giữ nguyên các hàm getAllSaleOrders, getSaleOrderById, getSaleOrderDetails hiện tại)
}
