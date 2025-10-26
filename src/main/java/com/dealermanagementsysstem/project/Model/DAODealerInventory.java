package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DAODealerInventory {

    // ✅ Lấy danh sách xe theo DealerID
    public List<DTODealerInventory> getVehiclesByDealerID(int dealerID) {
        List<DTODealerInventory> list = new ArrayList<>();
        String sql = "SELECT DealerID, VIN, ReceivedDate, Status, Amount FROM DealerInventory WHERE DealerID = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, dealerID);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                DTODealerInventory dto = new DTODealerInventory(
                        rs.getInt("DealerID"),
                        rs.getString("VIN"),
                        rs.getDate("ReceivedDate"),
                        rs.getString("Status"),
                        rs.getDouble("Amount") // 💰 thêm lấy Amount
                );
                list.add(dto);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // ✅ Xóa xe khỏi Inventory theo VIN (khi SaleOrder được Confirmed)
    public boolean removeVehicleByVIN(String vin) {
        String sql = "DELETE FROM DealerInventory WHERE VIN = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, vin);
            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ✅ Thêm xe vào Inventory (khi PurchaseOrder được Approved)
    public boolean addVehiclesToInventory(int dealerID, int modelID, int colorID, int quantity) {
        System.out.println("🔧 [DAODealerInventory] addVehiclesToInventory called:");
        System.out.println("   DealerID: " + dealerID);
        System.out.println("   ModelID: " + modelID);
        System.out.println("   ColorID: " + colorID);
        System.out.println("   Quantity: " + quantity);

        // ✅ Lấy xe có sẵn từ EVM_Vehicle (Status='InStock')
        String sqlGetAvailableVehicles = "SELECT TOP (?) VIN FROM EVM_Vehicle " +
                                         "WHERE ModelID = ? AND ColorID = ? AND Status = 'InStock' " +
                                         "ORDER BY ManufactureDate";

        // ✅ Chỉ INSERT vào DealerInventory (không tạo xe mới)
        String sqlInsertInventory = "INSERT INTO DealerInventory (DealerID, VIN, ReceivedDate, Status, Amount) " +
                                   "VALUES (?, ?, GETDATE(), 'IN_STOCK', 1)";

        Connection conn = null;
        PreparedStatement psGetVehicles = null;
        PreparedStatement psInsertInventory = null;
        ResultSet rs = null;

        try {
            conn = DBUtils.getConnection();
            System.out.println("   ✅ Database connection established");
            conn.setAutoCommit(false);
            System.out.println("   ✅ Transaction started");

            // 1️⃣ Lấy danh sách VIN có sẵn từ EVM_Vehicle
            psGetVehicles = conn.prepareStatement(sqlGetAvailableVehicles);
            psGetVehicles.setInt(1, quantity);
            psGetVehicles.setInt(2, modelID);
            psGetVehicles.setInt(3, colorID);
            rs = psGetVehicles.executeQuery();

            List<String> availableVINs = new ArrayList<>();
            while (rs.next()) {
                availableVINs.add(rs.getString("VIN"));
            }

            System.out.println("   🔍 Found " + availableVINs.size() + " available vehicles in EVM stock");

            if (availableVINs.size() < quantity) {
                System.out.println("   ⚠️  WARNING: Not enough vehicles in stock!");
                System.out.println("   📦 Required: " + quantity + ", Available: " + availableVINs.size());
                conn.rollback();
                return false;
            }

            // 2️⃣ Thêm các xe vào DealerInventory
            psInsertInventory = conn.prepareStatement(sqlInsertInventory);

            for (int i = 0; i < quantity; i++) {
                String vin = availableVINs.get(i);
                System.out.println("   🚗 Adding vehicle #" + (i+1) + ": " + vin);

                psInsertInventory.setInt(1, dealerID);
                psInsertInventory.setString(2, vin);

                int rowsInserted = psInsertInventory.executeUpdate();
                System.out.println("      ➤ Insert DealerInventory: " + (rowsInserted > 0 ? "✅ SUCCESS" : "❌ FAILED"));
            }

            conn.commit();
            System.out.println("   ✅ Transaction committed successfully!");
            System.out.println("   🎉 Added " + quantity + " vehicles to dealer inventory");
            return true;

        } catch (SQLException e) {
            System.out.println("   ❌ SQL ERROR occurred!");
            System.out.println("   ❌ Error message: " + e.getMessage());
            System.out.println("   ❌ SQL State: " + e.getSQLState());
            System.out.println("   ❌ Error Code: " + e.getErrorCode());
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                    System.out.println("   ↩️  Transaction rolled back");
                } catch (SQLException ex) {
                    System.out.println("   ❌ Rollback failed!");
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            try {
                if (rs != null) rs.close();
                if (psGetVehicles != null) psGetVehicles.close();
                if (psInsertInventory != null) psInsertInventory.close();
                if (conn != null) conn.close();
                System.out.println("   🔒 Resources closed");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
