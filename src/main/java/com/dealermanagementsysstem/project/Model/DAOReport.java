package com.dealermanagementsysstem.project.Model;

import org.springframework.stereotype.Repository;
import utils.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Repository
public class DAOReport {

    private static final Logger log = LoggerFactory.getLogger(DAOReport.class);

    public List<Map<String, Object>> getDealers() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT DealerID, DealerName FROM Dealer ORDER BY DealerName";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("dealerId", rs.getInt("DealerID"));
                row.put("dealerName", rs.getString("DealerName"));
                list.add(row);
            }
        } catch (SQLException e) {
            log.error("[DAOReport] getDealers SQL error", e);
        }
        return list;
    }

    // KPI: vehicles sold, inventory available, total revenue
    public Map<String, Object> getKpis(java.sql.Date fromDate, java.sql.Date toDate, Integer dealerId) {
        Map<String, Object> k = new HashMap<>();
        String sqlVehiclesSold = """
            SELECT COALESCE(SUM(CAST(Quantity AS INT)),0) AS VehiclesSold
            FROM SaleOrder
            WHERE Status = 'COMPLETED'
              AND (? IS NULL OR DealerID = ?)
              AND (? IS NULL OR ? IS NULL OR CreatedAt BETWEEN ? AND ?)
        """;
        String sqlTotalInventory = """
            SELECT COUNT(*) AS TotalInventory
            FROM DealerInventory di
            WHERE di.Status = 'AVAILABLE'
              AND (? IS NULL OR di.DealerID = ?)
              AND (? IS NULL OR ? IS NULL OR di.ReceivedDate BETWEEN ? AND ?)
        """;
        String sqlRevenue = """
            SELECT COALESCE(SUM(CAST(TotalAmount AS DECIMAL(18,2))),0) AS TotalRevenue
            FROM SaleOrder
            WHERE Status IN ('CONTRACT_SIGNED','PROCESSING','SHIPPED','COMPLETED')
              AND (? IS NULL OR DealerID = ?)
              AND (? IS NULL OR ? IS NULL OR CreatedAt BETWEEN ? AND ?)
        """;
        String sqlGrossRevenue = """
            SELECT COALESCE(SUM(CAST(COALESCE(sod.GrossUnitPrice, sod.Price, 0) AS DECIMAL(18,2)) * CAST(ISNULL(sod.Quantity,1) AS INT)),0) AS GrossRevenue
            FROM SaleOrder so
            JOIN SaleOrderDetail sod ON sod.SaleOrderID = so.SaleOrderID
            WHERE so.Status IN ('CONTRACT_SIGNED','PROCESSING','SHIPPED','COMPLETED')
              AND (? IS NULL OR so.DealerID = ?)
              AND (? IS NULL OR ? IS NULL OR so.CreatedAt BETWEEN ? AND ?)
        """;
        String sqlManufacturerDiscount = """
            SELECT COALESCE(SUM(
                     CASE 
                       WHEN sod.PromoDiscountAmount IS NOT NULL AND sod.PromoDiscountAmount > 0 THEN CAST(sod.PromoDiscountAmount AS DECIMAL(18,2)) * CAST(ISNULL(sod.Quantity,1) AS INT)
                       WHEN sod.PromoDiscountPercent IS NOT NULL AND sod.PromoDiscountPercent > 0 THEN CAST(sod.GrossUnitPrice AS DECIMAL(18,2)) * (sod.PromoDiscountPercent/100.0) * CAST(ISNULL(sod.Quantity,1) AS INT)
                       WHEN dp.DiscountPercent IS NOT NULL AND dp.DiscountPercent > 0 THEN CAST(sod.GrossUnitPrice AS DECIMAL(18,2)) * (dp.DiscountPercent/100.0) * CAST(ISNULL(sod.Quantity,1) AS INT)
                       WHEN dp.DiscountAmount IS NOT NULL AND dp.DiscountAmount > 0 THEN CAST(dp.DiscountAmount AS DECIMAL(18,2)) * CAST(ISNULL(sod.Quantity,1) AS INT)
                       ELSE 0 END
                   ),0) AS ManufDiscount
            FROM SaleOrder so
            JOIN SaleOrderDetail sod ON sod.SaleOrderID = so.SaleOrderID
            LEFT JOIN DiscountPolicy dp ON sod.PolicyID = dp.PolicyID
            WHERE so.Status IN ('CONTRACT_SIGNED','PROCESSING','SHIPPED','COMPLETED')
              AND (? IS NULL OR so.DealerID = ?)
              AND (? IS NULL OR ? IS NULL OR so.CreatedAt BETWEEN ? AND ?)
        """;
        try (Connection conn = DBUtils.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sqlVehiclesSold)) {
                if (dealerId == null) { ps.setNull(1, java.sql.Types.INTEGER); ps.setNull(2, java.sql.Types.INTEGER); } else { ps.setInt(1, dealerId); ps.setInt(2, dealerId); }
                if (fromDate == null || toDate == null) { ps.setNull(3, java.sql.Types.DATE); ps.setNull(4, java.sql.Types.DATE); ps.setNull(5, java.sql.Types.DATE); ps.setNull(6, java.sql.Types.DATE); }
                else { ps.setDate(3, fromDate); ps.setDate(4, toDate); ps.setDate(5, fromDate); ps.setDate(6, toDate); }
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) k.put("vehiclesSold", rs.getInt("VehiclesSold")); }
            }
            try (PreparedStatement ps = conn.prepareStatement(sqlTotalInventory)) {
                if (dealerId == null) { ps.setNull(1, java.sql.Types.INTEGER); ps.setNull(2, java.sql.Types.INTEGER); } else { ps.setInt(1, dealerId); ps.setInt(2, dealerId); }
                if (fromDate == null || toDate == null) { ps.setNull(3, java.sql.Types.DATE); ps.setNull(4, java.sql.Types.DATE); ps.setNull(5, java.sql.Types.DATE); ps.setNull(6, java.sql.Types.DATE); }
                else { ps.setDate(3, fromDate); ps.setDate(4, toDate); ps.setDate(5, fromDate); ps.setDate(6, toDate); }
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) k.put("totalInventory", rs.getInt("TotalInventory")); }
            }
            try (PreparedStatement ps = conn.prepareStatement(sqlRevenue)) {
                if (dealerId == null) { ps.setNull(1, java.sql.Types.INTEGER); ps.setNull(2, java.sql.Types.INTEGER); } else { ps.setInt(1, dealerId); ps.setInt(2, dealerId); }
                if (fromDate == null || toDate == null) { ps.setNull(3, java.sql.Types.DATE); ps.setNull(4, java.sql.Types.DATE); ps.setNull(5, java.sql.Types.DATE); ps.setNull(6, java.sql.Types.DATE); }
                else { ps.setDate(3, fromDate); ps.setDate(4, toDate); ps.setDate(5, fromDate); ps.setDate(6, toDate); }
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) k.put("totalRevenue", rs.getBigDecimal("TotalRevenue")); }
            }
            try (PreparedStatement ps = conn.prepareStatement(sqlGrossRevenue)) {
                if (dealerId == null) { ps.setNull(1, java.sql.Types.INTEGER); ps.setNull(2, java.sql.Types.INTEGER); } else { ps.setInt(1, dealerId); ps.setInt(2, dealerId); }
                if (fromDate == null || toDate == null) { ps.setNull(3, java.sql.Types.DATE); ps.setNull(4, java.sql.Types.DATE); ps.setNull(5, java.sql.Types.DATE); ps.setNull(6, java.sql.Types.DATE); }
                else { ps.setDate(3, fromDate); ps.setDate(4, toDate); ps.setDate(5, fromDate); ps.setDate(6, toDate); }
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) k.put("grossRevenue", rs.getBigDecimal("GrossRevenue")); }
            }
            try (PreparedStatement ps = conn.prepareStatement(sqlManufacturerDiscount)) {
                if (dealerId == null) { ps.setNull(1, java.sql.Types.INTEGER); ps.setNull(2, java.sql.Types.INTEGER); } else { ps.setInt(1, dealerId); ps.setInt(2, dealerId); }
                if (fromDate == null || toDate == null) { ps.setNull(3, java.sql.Types.DATE); ps.setNull(4, java.sql.Types.DATE); ps.setNull(5, java.sql.Types.DATE); ps.setNull(6, java.sql.Types.DATE); }
                else { ps.setDate(3, fromDate); ps.setDate(4, toDate); ps.setDate(5, fromDate); ps.setDate(6, toDate); }
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        java.math.BigDecimal manufDiscount = rs.getBigDecimal("ManufDiscount");
                        if (manufDiscount == null) manufDiscount = java.math.BigDecimal.ZERO;
                        k.put("manufacturerDiscountTotal", manufDiscount);
                        java.math.BigDecimal totalRev = (java.math.BigDecimal) k.getOrDefault("totalRevenue", java.math.BigDecimal.ZERO);
                        java.math.BigDecimal grossRev = (java.math.BigDecimal) k.getOrDefault("grossRevenue", java.math.BigDecimal.ZERO);
                        k.put("netRevenueAfterDiscount", totalRev); // net already after discount
                        k.put("discountSavings", grossRev.subtract(totalRev));
                    }
                }
            }
        } catch (SQLException e) {
            log.error("[DAOReport] getKpis SQL error", e);
        }
        return k;
    }

    public List<Map<String, Object>> getDealerAggregates(java.sql.Date fromDate, java.sql.Date toDate, Integer dealerId) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
            WITH po AS (
                SELECT DealerID, COUNT(*) AS PurchaseOrders
                FROM PurchaseOrder
                WHERE (? IS NULL OR DealerID = ?)
                  AND (? IS NULL OR ? IS NULL OR CreatedAt BETWEEN ? AND ?)
                GROUP BY DealerID
            ), so AS (
                SELECT DealerID, COUNT(*) AS SaleOrders, COALESCE(SUM(CAST(Quantity AS INT)),0) AS VehiclesSold,
                       COALESCE(SUM(CAST(TotalAmount AS DECIMAL(18,2))),0) AS TotalRevenue
                FROM SaleOrder
                WHERE (? IS NULL OR DealerID = ?)
                  AND (? IS NULL OR ? IS NULL OR CreatedAt BETWEEN ? AND ?)
                GROUP BY DealerID
            ), inv AS (
                SELECT DealerID, COUNT(*) AS Inventory
                FROM DealerInventory
                WHERE Status = 'AVAILABLE'
                  AND (? IS NULL OR DealerID = ?)
                  AND (? IS NULL OR ? IS NULL OR ReceivedDate BETWEEN ? AND ?)
                GROUP BY DealerID
            ), gross AS (
                SELECT so.DealerID, COALESCE(SUM(CAST(COALESCE(sod.GrossUnitPrice, sod.Price, 0) AS DECIMAL(18,2)) * CAST(ISNULL(sod.Quantity,1) AS INT)),0) AS GrossRevenue
                FROM SaleOrderDetail sod JOIN SaleOrder so ON sod.SaleOrderID = so.SaleOrderID
                WHERE so.Status IN ('CONTRACT_SIGNED','PROCESSING','SHIPPED','COMPLETED')
                  AND (? IS NULL OR so.DealerID = ?)
                  AND (? IS NULL OR ? IS NULL OR so.CreatedAt BETWEEN ? AND ?)
                GROUP BY so.DealerID
            ), manuf AS (
                SELECT so.DealerID, COALESCE(SUM(
                         CASE 
                           WHEN sod.PromoDiscountAmount IS NOT NULL AND sod.PromoDiscountAmount > 0 THEN CAST(sod.PromoDiscountAmount AS DECIMAL(18,2)) * CAST(ISNULL(sod.Quantity,1) AS INT)
                           WHEN sod.PromoDiscountPercent IS NOT NULL AND sod.PromoDiscountPercent > 0 THEN CAST(sod.GrossUnitPrice AS DECIMAL(18,2)) * (sod.PromoDiscountPercent/100.0) * CAST(ISNULL(sod.Quantity,1) AS INT)
                           WHEN dp.DiscountPercent IS NOT NULL AND dp.DiscountPercent > 0 THEN CAST(sod.GrossUnitPrice AS DECIMAL(18,2)) * (dp.DiscountPercent/100.0) * CAST(ISNULL(sod.Quantity,1) AS INT)
                           WHEN dp.DiscountAmount IS NOT NULL AND dp.DiscountAmount > 0 THEN CAST(dp.DiscountAmount AS DECIMAL(18,2)) * CAST(ISNULL(sod.Quantity,1) AS INT)
                           ELSE 0 END
                       ),0) AS ManufDiscount
                FROM SaleOrderDetail sod JOIN SaleOrder so ON sod.SaleOrderID = so.SaleOrderID
                LEFT JOIN DiscountPolicy dp ON sod.PolicyID = dp.PolicyID
                WHERE so.Status IN ('CONTRACT_SIGNED','PROCESSING','SHIPPED','COMPLETED')
                  AND (? IS NULL OR so.DealerID = ?)
                  AND (? IS NULL OR ? IS NULL OR so.CreatedAt BETWEEN ? AND ?)
                GROUP BY so.DealerID
            )
            SELECT d.DealerID, d.DealerName,
                   COALESCE(po.PurchaseOrders,0) AS PurchaseOrders,
                   COALESCE(so.SaleOrders,0) AS SaleOrders,
                   COALESCE(so.VehiclesSold,0) AS VehiclesSold,
                   COALESCE(gross.GrossRevenue,0) AS GrossRevenue,
                   COALESCE(so.TotalRevenue,0) AS TotalRevenue,
                   COALESCE(inv.Inventory,0) AS Inventory,
                   COALESCE(manuf.ManufDiscount,0) AS ManufacturerDiscount,
                   COALESCE(so.TotalRevenue,0) AS NetRevenueAfterDiscount
            FROM Dealer d
            LEFT JOIN po ON po.DealerID = d.DealerID
            LEFT JOIN so ON so.DealerID = d.DealerID
            LEFT JOIN inv ON inv.DealerID = d.DealerID
            LEFT JOIN gross ON gross.DealerID = d.DealerID
            LEFT JOIN manuf ON manuf.DealerID = d.DealerID
            WHERE (? IS NULL OR d.DealerID = ?)
            ORDER BY d.DealerName
        """;
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            // po block (6 params)
            if (dealerId == null) { ps.setNull(idx++, java.sql.Types.INTEGER); ps.setNull(idx++, java.sql.Types.INTEGER); } else { ps.setInt(idx++, dealerId); ps.setInt(idx++, dealerId); }
            if (fromDate == null || toDate == null) { ps.setNull(idx++, java.sql.Types.DATE); ps.setNull(idx++, java.sql.Types.DATE); ps.setNull(idx++, java.sql.Types.DATE); ps.setNull(idx++, java.sql.Types.DATE); } else { ps.setDate(idx++, fromDate); ps.setDate(idx++, toDate); ps.setDate(idx++, fromDate); ps.setDate(idx++, toDate); }
            // so block
            if (dealerId == null) { ps.setNull(idx++, java.sql.Types.INTEGER); ps.setNull(idx++, java.sql.Types.INTEGER); } else { ps.setInt(idx++, dealerId); ps.setInt(idx++, dealerId); }
            if (fromDate == null || toDate == null) { ps.setNull(idx++, java.sql.Types.DATE); ps.setNull(idx++, java.sql.Types.DATE); ps.setNull(idx++, java.sql.Types.DATE); ps.setNull(idx++, java.sql.Types.DATE); } else { ps.setDate(idx++, fromDate); ps.setDate(idx++, toDate); ps.setDate(idx++, fromDate); ps.setDate(idx++, toDate); }
            // inv block
            if (dealerId == null) { ps.setNull(idx++, java.sql.Types.INTEGER); ps.setNull(idx++, java.sql.Types.INTEGER); } else { ps.setInt(idx++, dealerId); ps.setInt(idx++, dealerId); }
            if (fromDate == null || toDate == null) { ps.setNull(idx++, java.sql.Types.DATE); ps.setNull(idx++, java.sql.Types.DATE); ps.setNull(idx++, java.sql.Types.DATE); ps.setNull(idx++, java.sql.Types.DATE); } else { ps.setDate(idx++, fromDate); ps.setDate(idx++, toDate); ps.setDate(idx++, fromDate); ps.setDate(idx++, toDate); }
            // gross block (MISSING BEFORE - now added)
            if (dealerId == null) { ps.setNull(idx++, java.sql.Types.INTEGER); ps.setNull(idx++, java.sql.Types.INTEGER); } else { ps.setInt(idx++, dealerId); ps.setInt(idx++, dealerId); }
            if (fromDate == null || toDate == null) { ps.setNull(idx++, java.sql.Types.DATE); ps.setNull(idx++, java.sql.Types.DATE); ps.setNull(idx++, java.sql.Types.DATE); ps.setNull(idx++, java.sql.Types.DATE); } else { ps.setDate(idx++, fromDate); ps.setDate(idx++, toDate); ps.setDate(idx++, fromDate); ps.setDate(idx++, toDate); }
            // manuf block
            if (dealerId == null) { ps.setNull(idx++, java.sql.Types.INTEGER); ps.setNull(idx++, java.sql.Types.INTEGER); } else { ps.setInt(idx++, dealerId); ps.setInt(idx++, dealerId); }
            if (fromDate == null || toDate == null) { ps.setNull(idx++, java.sql.Types.DATE); ps.setNull(idx++, java.sql.Types.DATE); ps.setNull(idx++, java.sql.Types.DATE); ps.setNull(idx++, java.sql.Types.DATE); } else { ps.setDate(idx++, fromDate); ps.setDate(idx++, toDate); ps.setDate(idx++, fromDate); ps.setDate(idx++, toDate); }
            // final filter
            if (dealerId == null) { ps.setNull(idx++, java.sql.Types.INTEGER); ps.setNull(idx++, java.sql.Types.INTEGER); } else { ps.setInt(idx++, dealerId); ps.setInt(idx++, dealerId); }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("dealerId", rs.getInt("DealerID"));
                    row.put("dealerName", rs.getString("DealerName"));
                    row.put("purchaseOrders", rs.getInt("PurchaseOrders"));
                    row.put("saleOrders", rs.getInt("SaleOrders"));
                    row.put("vehiclesSold", rs.getInt("VehiclesSold"));
                    row.put("totalRevenue", rs.getBigDecimal("TotalRevenue"));
                    row.put("inventory", rs.getInt("Inventory"));
                    row.put("manufacturerDiscount", rs.getBigDecimal("ManufacturerDiscount"));
                    row.put("netRevenueAfterDiscount", rs.getBigDecimal("NetRevenueAfterDiscount"));
                    row.put("grossRevenue", rs.getBigDecimal("GrossRevenue"));
                    list.add(row);
                }
            }
            log.debug("[DAOReport] dealerAggregates rows={} dealerId={} from={} to={}", list.size(), dealerId, fromDate, toDate);
        } catch (SQLException e) {
            log.error("[DAOReport] getDealerAggregates SQL error dealerId={} from={} to={}", dealerId, fromDate, toDate, e);
        }
        return list;
    }

    public int getDealerCount() {
        String sql = "SELECT COUNT(*) AS Cnt FROM Dealer";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt("Cnt");
        } catch (SQLException e) {
            log.error("[DAOReport] getDealerCount SQL error", e);
        }
        return 0;
    }

    public Map<String, Integer> getInventoryTotals() {
        Map<String, Integer> m = new HashMap<>();
        String sql = """
            SELECT 
              COUNT(*) AS Total,
              SUM(CASE WHEN Status='AVAILABLE' THEN 1 ELSE 0 END) AS AvailableCnt,
              SUM(CASE WHEN Status='SOLD' THEN 1 ELSE 0 END) AS SoldCnt,
              SUM(CASE WHEN Status='TRANSFERRED' THEN 1 ELSE 0 END) AS TransferredCnt
            FROM DealerInventory
        """;
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                m.put("total", rs.getInt("Total"));
                m.put("available", rs.getInt("AvailableCnt"));
                m.put("sold", rs.getInt("SoldCnt"));
                m.put("transferred", rs.getInt("TransferredCnt"));
            }
        } catch (SQLException e) {
            log.error("[DAOReport] getInventoryTotals SQL error", e);
        }
        return m;
    }

    public Map<String, Integer> getPurchaseOrderStats() {
        Map<String, Integer> m = new HashMap<>();
        String sql = "SELECT Status, COUNT(*) AS Cnt FROM PurchaseOrder GROUP BY Status";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) { m.put(rs.getString("Status"), rs.getInt("Cnt")); }
        } catch (SQLException e) {
            log.error("[DAOReport] getPurchaseOrderStats SQL error", e);
        }
        return m;
    }

    public Map<String, Integer> getSaleOrderStats() {
        Map<String, Integer> m = new HashMap<>();
        String sql = "SELECT Status, COUNT(*) AS Cnt FROM SaleOrder GROUP BY Status";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) { m.put(rs.getString("Status"), rs.getInt("Cnt")); }
        } catch (SQLException e) {
            log.error("[DAOReport] getSaleOrderStats SQL error", e);
        }
        return m;
    }

    public List<Map<String, Object>> getTopModelsByPOQuantity() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
            SELECT TOP 5 vm.ModelName, SUM(pod.Quantity) AS Qty
            FROM PurchaseOrderDetail pod
            LEFT JOIN VehicleVersion vv ON pod.VersionID = vv.VersionID
            LEFT JOIN VehicleModel vm ON vv.ModelID = vm.ModelID
            GROUP BY vm.ModelName
            ORDER BY Qty DESC
        """;
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("modelName", rs.getString("ModelName"));
                row.put("qty", rs.getInt("Qty"));
                list.add(row);
            }
        } catch (SQLException e) {
            log.error("[DAOReport] getTopModelsByPOQuantity SQL error", e);
        }
        return list;
    }

    public List<Map<String, Object>> getTopDealersByRevenue(java.sql.Date fromDate, java.sql.Date toDate, Integer dealerId) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
            SELECT TOP 5 d.DealerName, SUM(CAST(so.TotalAmount AS DECIMAL(18,2))) AS Revenue
            FROM SaleOrder so
            JOIN Dealer d ON d.DealerID = so.DealerID
            WHERE so.Status IN ('CONTRACT_SIGNED','PROCESSING','SHIPPED','COMPLETED')
              AND (? IS NULL OR so.DealerID = ?)
              AND (? IS NULL OR ? IS NULL OR so.CreatedAt BETWEEN ? AND ?)
            GROUP BY d.DealerName
            ORDER BY Revenue DESC
        """;
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (dealerId == null) { ps.setNull(1, java.sql.Types.INTEGER); ps.setNull(2, java.sql.Types.INTEGER); } else { ps.setInt(1, dealerId); ps.setInt(2, dealerId); }
            if (fromDate == null || toDate == null) { ps.setNull(3, java.sql.Types.DATE); ps.setNull(4, java.sql.Types.DATE); ps.setNull(5, java.sql.Types.DATE); ps.setNull(6, java.sql.Types.DATE); }
            else { ps.setDate(3, fromDate); ps.setDate(4, toDate); ps.setDate(5, fromDate); ps.setDate(6, toDate); }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("dealerName", rs.getString("DealerName"));
                    row.put("revenue", rs.getBigDecimal("Revenue"));
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            log.error("[DAOReport] getTopDealersByRevenue SQL error dealerId={} from={} to={}", dealerId, fromDate, toDate, e);
        }
        return list;
    }

    public List<Map<String, Object>> getRevenueShareByModel(java.sql.Date fromDate, java.sql.Date toDate, Integer dealerId) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
            SELECT vm.ModelName, SUM(CAST(pod.Subtotal AS DECIMAL(18,2))) AS Revenue
            FROM PurchaseOrderDetail pod
            JOIN PurchaseOrder po ON po.PurchaseOrderID = pod.PurchaseOrderID
            LEFT JOIN VehicleVersion vv ON pod.VersionID = vv.VersionID
            LEFT JOIN VehicleModel vm ON vv.ModelID = vm.ModelID
            WHERE (? IS NULL OR po.DealerID = ?)
              AND (? IS NULL OR ? IS NULL OR po.CreatedAt BETWEEN ? AND ?)
            GROUP BY vm.ModelName
            HAVING SUM(CAST(pod.Subtotal AS DECIMAL(18,2))) IS NOT NULL
            ORDER BY Revenue DESC
        """;
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (dealerId == null) { ps.setNull(1, java.sql.Types.INTEGER); ps.setNull(2, java.sql.Types.INTEGER); } else { ps.setInt(1, dealerId); ps.setInt(2, dealerId); }
            if (fromDate == null || toDate == null) { ps.setNull(3, java.sql.Types.DATE); ps.setNull(4, java.sql.Types.DATE); ps.setNull(5, java.sql.Types.DATE); ps.setNull(6, java.sql.Types.DATE); }
            else { ps.setDate(3, fromDate); ps.setDate(4, toDate); ps.setDate(5, fromDate); ps.setDate(6, toDate); }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("modelName", rs.getString("ModelName"));
                    row.put("revenue", rs.getBigDecimal("Revenue"));
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            log.error("[DAOReport] getRevenueShareByModel SQL error dealerId={} from={} to={}", dealerId, fromDate, toDate, e);
        }
        return list;
    }
}

