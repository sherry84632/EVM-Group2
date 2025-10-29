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

@Repository
public class DAOReport {

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
            // ignore for summary
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
        } catch (SQLException e) {
            // ignore for summary
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
            )
            SELECT d.DealerID, d.DealerName,
                   COALESCE(po.PurchaseOrders,0) AS PurchaseOrders,
                   COALESCE(so.SaleOrders,0) AS SaleOrders,
                   COALESCE(so.VehiclesSold,0) AS VehiclesSold,
                   COALESCE(so.TotalRevenue,0) AS TotalRevenue,
                   COALESCE(inv.Inventory,0) AS Inventory
            FROM Dealer d
            LEFT JOIN po ON po.DealerID = d.DealerID
            LEFT JOIN so ON so.DealerID = d.DealerID
            LEFT JOIN inv ON inv.DealerID = d.DealerID
            WHERE (? IS NULL OR d.DealerID = ?)
            ORDER BY d.DealerName
        """;
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            // po
            if (dealerId == null) { ps.setNull(1, java.sql.Types.INTEGER); ps.setNull(2, java.sql.Types.INTEGER); } else { ps.setInt(1, dealerId); ps.setInt(2, dealerId); }
            if (fromDate == null || toDate == null) { ps.setNull(3, java.sql.Types.DATE); ps.setNull(4, java.sql.Types.DATE); ps.setNull(5, java.sql.Types.DATE); ps.setNull(6, java.sql.Types.DATE); }
            else { ps.setDate(3, fromDate); ps.setDate(4, toDate); ps.setDate(5, fromDate); ps.setDate(6, toDate); }
            // so
            if (dealerId == null) { ps.setNull(7, java.sql.Types.INTEGER); ps.setNull(8, java.sql.Types.INTEGER); } else { ps.setInt(7, dealerId); ps.setInt(8, dealerId); }
            if (fromDate == null || toDate == null) { ps.setNull(9, java.sql.Types.DATE); ps.setNull(10, java.sql.Types.DATE); ps.setNull(11, java.sql.Types.DATE); ps.setNull(12, java.sql.Types.DATE); }
            else { ps.setDate(9, fromDate); ps.setDate(10, toDate); ps.setDate(11, fromDate); ps.setDate(12, toDate); }
            // inv
            if (dealerId == null) { ps.setNull(13, java.sql.Types.INTEGER); ps.setNull(14, java.sql.Types.INTEGER); } else { ps.setInt(13, dealerId); ps.setInt(14, dealerId); }
            if (fromDate == null || toDate == null) { ps.setNull(15, java.sql.Types.DATE); ps.setNull(16, java.sql.Types.DATE); ps.setNull(17, java.sql.Types.DATE); ps.setNull(18, java.sql.Types.DATE); }
            else { ps.setDate(15, fromDate); ps.setDate(16, toDate); ps.setDate(17, fromDate); ps.setDate(18, toDate); }
            // final filter
            if (dealerId == null) { ps.setNull(19, java.sql.Types.INTEGER); ps.setNull(20, java.sql.Types.INTEGER); } else { ps.setInt(19, dealerId); ps.setInt(20, dealerId); }

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
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            // ignore for summary
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
            // ignore for summary
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
            // ignore for summary
        }
        return m;
    }

    public Map<String, Integer> getPurchaseOrderStats() {
        Map<String, Integer> m = new HashMap<>();
        String sql = "SELECT Status, COUNT(*) AS Cnt FROM PurchaseOrder GROUP BY Status";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                m.put(rs.getString("Status"), rs.getInt("Cnt"));
            }
        } catch (SQLException e) {
            // ignore for summary
        }
        return m;
    }

    public Map<String, Integer> getSaleOrderStats() {
        Map<String, Integer> m = new HashMap<>();
        String sql = "SELECT Status, COUNT(*) AS Cnt FROM SaleOrder GROUP BY Status";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                m.put(rs.getString("Status"), rs.getInt("Cnt"));
            }
        } catch (SQLException e) {
            // ignore for summary
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
            // ignore for summary
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
            // ignore for summary
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
            // ignore for summary
        }
        return list;
    }
}


