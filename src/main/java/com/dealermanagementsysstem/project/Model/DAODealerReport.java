package com.dealermanagementsysstem.project.Model;

import org.springframework.stereotype.Repository;
import utils.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Repository
public class DAODealerReport {

    public Map<String, Object> getSalesKpis(Integer dealerId, java.sql.Date fromDate, java.sql.Date toDate) {
        Map<String, Object> kpi = new HashMap<>();
        String sqlNet = """
            SELECT COUNT(*) AS TotalOrders,
                   COALESCE(SUM(CAST(Quantity AS INT)),0) AS TotalVehicles,
                   COALESCE(SUM(CAST(TotalAmount AS DECIMAL(18,2))),0) AS NetRevenue
            FROM SaleOrder
            WHERE (? IS NULL OR DealerID = ?)
              AND (? IS NULL OR ? IS NULL OR CreatedAt BETWEEN ? AND ?)
        """;
        String sqlGross = """
            SELECT COALESCE(SUM(CAST(sod.GrossUnitPrice AS DECIMAL(18,2)) * CAST(ISNULL(sod.Quantity,1) AS INT)),0) AS GrossRevenue
            FROM SaleOrder so
            JOIN SaleOrderDetail sod ON sod.SaleOrderID = so.SaleOrderID
            WHERE (? IS NULL OR so.DealerID = ?)
              AND (? IS NULL OR ? IS NULL OR so.CreatedAt BETWEEN ? AND ?)
        """;
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement psNet = conn.prepareStatement(sqlNet);
             PreparedStatement psGross = conn.prepareStatement(sqlGross)) {
            // Net params
            psNet.setObject(1, dealerId); psNet.setObject(2, dealerId);
            psNet.setObject(3, fromDate); psNet.setObject(4, toDate); psNet.setObject(5, fromDate); psNet.setObject(6, toDate);
            java.math.BigDecimal netRevenue = java.math.BigDecimal.ZERO; int totalOrders = 0; int totalVehicles = 0;
            try (ResultSet rs = psNet.executeQuery()) {
                if (rs.next()) {
                    totalOrders = rs.getInt("TotalOrders");
                    totalVehicles = rs.getInt("TotalVehicles");
                    netRevenue = rs.getBigDecimal("NetRevenue");
                    if (netRevenue == null) netRevenue = java.math.BigDecimal.ZERO;
                }
            }
            // Gross params
            psGross.setObject(1, dealerId); psGross.setObject(2, dealerId);
            psGross.setObject(3, fromDate); psGross.setObject(4, toDate); psGross.setObject(5, fromDate); psGross.setObject(6, toDate);
            java.math.BigDecimal grossRevenue = java.math.BigDecimal.ZERO;
            try (ResultSet rs2 = psGross.executeQuery()) {
                if (rs2.next()) {
                    grossRevenue = rs2.getBigDecimal("GrossRevenue");
                    if (grossRevenue == null) grossRevenue = java.math.BigDecimal.ZERO;
                }
            }
            kpi.put("totalOrders", totalOrders);
            kpi.put("totalVehicles", totalVehicles);
            kpi.put("grossRevenue", grossRevenue); // BEFORE discounts
            kpi.put("netRevenue", netRevenue);     // AFTER discounts (current sale order total)
            kpi.put("discountSavings", grossRevenue.subtract(netRevenue));
            java.math.BigDecimal savings = grossRevenue.subtract(netRevenue);
            double savingsPct = (grossRevenue.compareTo(java.math.BigDecimal.ZERO) > 0)
                    ? savings.divide(grossRevenue, 6, java.math.RoundingMode.HALF_UP)
                    .multiply(java.math.BigDecimal.valueOf(100)).doubleValue() : 0.0;
            kpi.put("discountSavingsPercent", savingsPct);
        } catch (SQLException ignored) { }
        return kpi;
    }

    public List<Map<String, Object>> getSalesByMonth(Integer dealerId, java.sql.Date fromDate, java.sql.Date toDate) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
            SELECT FORMAT(CreatedAt, 'yyyy-MM') AS YearMonth,
                   COALESCE(SUM(CAST(TotalAmount AS DECIMAL(18,2))),0) AS Revenue,
                   COALESCE(SUM(CAST(Quantity AS INT)),0) AS Vehicles
            FROM SaleOrder
            WHERE (? IS NULL OR DealerID = ?)
              AND (? IS NULL OR ? IS NULL OR CreatedAt BETWEEN ? AND ?)
            GROUP BY FORMAT(CreatedAt, 'yyyy-MM')
            ORDER BY YearMonth
        """;
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, dealerId);
            ps.setObject(2, dealerId);
            ps.setObject(3, fromDate);
            ps.setObject(4, toDate);
            ps.setObject(5, fromDate);
            ps.setObject(6, toDate);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("yearMonth", rs.getString("YearMonth"));
                    row.put("revenue", rs.getBigDecimal("Revenue"));
                    row.put("vehicles", rs.getInt("Vehicles"));
                    list.add(row);
                }
            }
        } catch (SQLException ignored) { }
        return list;
    }

    public Map<String, Integer> getOrderStatusDistribution(Integer dealerId, java.sql.Date fromDate, java.sql.Date toDate) {
        Map<String, Integer> m = new HashMap<>();
        String sql = """
            SELECT Status, COUNT(*) AS Cnt
            FROM SaleOrder
            WHERE (? IS NULL OR DealerID = ?)
              AND (? IS NULL OR ? IS NULL OR CreatedAt BETWEEN ? AND ?)
            GROUP BY Status
        """;
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, dealerId);
            ps.setObject(2, dealerId);
            ps.setObject(3, fromDate);
            ps.setObject(4, toDate);
            ps.setObject(5, fromDate);
            ps.setObject(6, toDate);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) m.put(rs.getString("Status"), rs.getInt("Cnt"));
            }
        } catch (SQLException ignored) { }
        return m;
    }

    public List<Map<String, Object>> getTopVehicleColorsSold(Integer dealerId, java.sql.Date fromDate, java.sql.Date toDate, int limit) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
            SELECT TOP (?) vc.ColorName, COUNT(*) AS Cnt
            FROM SaleOrderDetail sod
            JOIN SaleOrder so ON sod.SaleOrderID = so.SaleOrderID
            JOIN Vehicle v ON sod.VehicleID = v.VehicleID
            LEFT JOIN VehicleColor vc ON v.ColorID = vc.ColorID
            WHERE (? IS NULL OR so.DealerID = ?)
              AND (? IS NULL OR ? IS NULL OR so.CreatedAt BETWEEN ? AND ?)
            GROUP BY vc.ColorName
            ORDER BY Cnt DESC
        """;
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Math.max(1, limit));
            ps.setObject(2, dealerId);
            ps.setObject(3, dealerId);
            ps.setObject(4, fromDate);
            ps.setObject(5, toDate);
            ps.setObject(6, fromDate);
            ps.setObject(7, toDate);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("color", rs.getString("ColorName"));
                    row.put("count", rs.getInt("Cnt"));
                    list.add(row);
                }
            }
        } catch (SQLException ignored) { }
        return list;
    }

    public List<Map<String, Object>> getSalesTable(Integer dealerId, java.sql.Date fromDate, java.sql.Date toDate, String status) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
            SELECT so.SaleOrderID, so.CreatedAt, so.Status, so.TotalAmount, so.Quantity,
                   c.FullName AS CustomerName
            FROM SaleOrder so
            JOIN Customer c ON so.CustomerID = c.CustomerID
            WHERE (? IS NULL OR so.DealerID = ?)
              AND (? IS NULL OR ? IS NULL OR so.CreatedAt BETWEEN ? AND ?)
              AND (? IS NULL OR so.Status = ?)
            ORDER BY so.SaleOrderID DESC
        """;
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, dealerId);
            ps.setObject(2, dealerId);
            ps.setObject(3, fromDate);
            ps.setObject(4, toDate);
            ps.setObject(5, fromDate);
            ps.setObject(6, toDate);
            ps.setObject(7, status);
            ps.setObject(8, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("saleOrderId", rs.getInt("SaleOrderID"));
                    row.put("createdAt", rs.getTimestamp("CreatedAt"));
                    row.put("status", rs.getString("Status"));
                    row.put("totalAmount", rs.getBigDecimal("TotalAmount"));
                    row.put("quantity", rs.getInt("Quantity"));
                    row.put("customerName", rs.getString("CustomerName"));
                    list.add(row);
                }
            }
        } catch (SQLException ignored) { }
        return list;
    }

    public Map<String, Integer> getInventoryKpis(Integer dealerId, java.sql.Date fromDate, java.sql.Date toDate) {
        Map<String, Integer> m = new HashMap<>();
        String sql = """
            SELECT 
              COUNT(*) AS Total,
              SUM(CASE WHEN Status='AVAILABLE' THEN 1 ELSE 0 END) AS AvailableCnt,
              SUM(CASE WHEN Status='SOLD' THEN 1 ELSE 0 END) AS SoldCnt,
              SUM(CASE WHEN Status='TRANSFERRED' THEN 1 ELSE 0 END) AS TransferredCnt
            FROM DealerInventory di
            WHERE (? IS NULL OR di.DealerID = ?)
              AND (? IS NULL OR ? IS NULL OR di.ReceivedDate BETWEEN ? AND ?)
        """;
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, dealerId);
            ps.setObject(2, dealerId);
            ps.setObject(3, fromDate);
            ps.setObject(4, toDate);
            ps.setObject(5, fromDate);
            ps.setObject(6, toDate);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    m.put("total", rs.getInt("Total"));
                    m.put("available", rs.getInt("AvailableCnt"));
                    m.put("sold", rs.getInt("SoldCnt"));
                    m.put("transferred", rs.getInt("TransferredCnt"));
                }
            }
        } catch (SQLException ignored) { }
        return m;
    }

    public List<Map<String, Object>> getInventoryByColor(Integer dealerId) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
            SELECT vc.ColorName, COUNT(*) AS Cnt
            FROM DealerInventory di
            LEFT JOIN Vehicle v ON di.VehicleID = v.VehicleID
            LEFT JOIN VehicleColor vc ON v.ColorID = vc.ColorID
            WHERE (? IS NULL OR di.DealerID = ?)
            GROUP BY vc.ColorName
            ORDER BY Cnt DESC
        """;
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, dealerId);
            ps.setObject(2, dealerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("color", rs.getString("ColorName"));
                    row.put("count", rs.getInt("Cnt"));
                    list.add(row);
                }
            }
        } catch (SQLException ignored) { }
        return list;
    }

    public List<Map<String, Object>> getInventoryByVersion(Integer dealerId) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
            SELECT vv.VersionName, COUNT(*) AS Cnt
            FROM DealerInventory di
            LEFT JOIN Vehicle v ON di.VehicleID = v.VehicleID
            LEFT JOIN VehicleVersion vv ON v.VersionID = vv.VersionID
            WHERE (? IS NULL OR di.DealerID = ?)
            GROUP BY vv.VersionName
            ORDER BY Cnt DESC
        """;
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, dealerId);
            ps.setObject(2, dealerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("version", rs.getString("VersionName"));
                    row.put("count", rs.getInt("Cnt"));
                    list.add(row);
                }
            }
        } catch (SQLException ignored) { }
        return list;
    }

    public Map<String, Object> getPurchaseOrderKpis(Integer dealerId, java.sql.Date fromDate, java.sql.Date toDate) {
        Map<String, Object> m = new HashMap<>();
        String sql = """
            SELECT COUNT(*) AS TotalPO,
                   COALESCE(SUM(CAST(po.TotalAmount AS DECIMAL(18,2))),0) AS TotalAmount
            FROM PurchaseOrder po
            WHERE (? IS NULL OR po.DealerID = ?)
              AND (? IS NULL OR ? IS NULL OR po.CreatedAt BETWEEN ? AND ?)
        """;
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, dealerId);
            ps.setObject(2, dealerId);
            ps.setObject(3, fromDate);
            ps.setObject(4, toDate);
            ps.setObject(5, fromDate);
            ps.setObject(6, toDate);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    m.put("totalPO", rs.getInt("TotalPO"));
                    m.put("totalAmount", rs.getBigDecimal("TotalAmount"));
                }
            }
        } catch (SQLException ignored) { }
        return m;
    }

    public Map<String, Integer> getPurchaseOrderStatusDistribution(Integer dealerId, java.sql.Date fromDate, java.sql.Date toDate) {
        Map<String, Integer> m = new HashMap<>();
        String sql = """
            SELECT Status, COUNT(*) AS Cnt
            FROM PurchaseOrder
            WHERE (? IS NULL OR DealerID = ?)
              AND (? IS NULL OR ? IS NULL OR CreatedAt BETWEEN ? AND ?)
            GROUP BY Status
        """;
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, dealerId);
            ps.setObject(2, dealerId);
            ps.setObject(3, fromDate);
            ps.setObject(4, toDate);
            ps.setObject(5, fromDate);
            ps.setObject(6, toDate);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) m.put(rs.getString("Status"), rs.getInt("Cnt"));
            }
        } catch (SQLException ignored) { }
        return m;
    }

    public Map<String, Object> getDiscountEffectiveness(Integer dealerId, java.sql.Date fromDate, java.sql.Date toDate) {
        Map<String, Object> m = new HashMap<>();
        String sqlApplied = """
            SELECT COUNT(*) AS Applied
            FROM SaleOrderDetail sod
            JOIN SaleOrder so ON sod.SaleOrderID = so.SaleOrderID
            WHERE sod.PolicyID IS NOT NULL
              AND (? IS NULL OR so.DealerID = ?)
              AND (? IS NULL OR ? IS NULL OR so.CreatedAt BETWEEN ? AND ?)
        """;
        String sqlByPolicy = """
            SELECT dp.PolicyName,
                   COUNT(*) AS Orders,
                   COALESCE(SUM(CAST(so.TotalAmount AS DECIMAL(18,2))),0) AS Revenue
            FROM SaleOrder so
            JOIN SaleOrderDetail sod ON sod.SaleOrderID = so.SaleOrderID
            JOIN DiscountPolicy dp ON sod.PolicyID = dp.PolicyID
            WHERE (? IS NULL OR so.DealerID = ?)
              AND (? IS NULL OR ? IS NULL OR so.CreatedAt BETWEEN ? AND ?)
            GROUP BY dp.PolicyName
            ORDER BY Revenue DESC
        """;
        try (Connection conn = DBUtils.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sqlApplied)) {
                ps.setObject(1, dealerId);
                ps.setObject(2, dealerId);
                ps.setObject(3, fromDate);
                ps.setObject(4, toDate);
                ps.setObject(5, fromDate);
                ps.setObject(6, toDate);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) m.put("appliedCount", rs.getInt("Applied"));
                }
            }
            List<Map<String, Object>> byPolicy = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(sqlByPolicy)) {
                ps.setObject(1, dealerId);
                ps.setObject(2, dealerId);
                ps.setObject(3, fromDate);
                ps.setObject(4, toDate);
                ps.setObject(5, fromDate);
                ps.setObject(6, toDate);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        row.put("policyName", rs.getString("PolicyName"));
                        row.put("orders", rs.getInt("Orders"));
                        row.put("revenue", rs.getBigDecimal("Revenue"));
                        byPolicy.add(row);
                    }
                }
            }
            m.put("byPolicy", byPolicy);
        } catch (SQLException ignored) { }
        return m;
    }
}
