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

        // ✅ VALIDATION: Kiểm tra ModelID và ColorID tồn tại
        if (!validateModelAndColor(modelID, colorID)) {
            System.out.println("   ❌ VALIDATION FAILED: ModelID hoặc ColorID không tồn tại!");
            return false;
        }

        // Lấy VersionID mặc định (giả sử VersionID = 3 là standard version)
        int versionID = 3;

        // Insert theo thứ tự: EVM_Vehicle → Vehicle → DealerInventory
        String sqlInsertEVMVehicle = "INSERT INTO EVM_Vehicle (VIN, ModelID, ColorID, VersionID, Status) VALUES (?, ?, ?, ?, 'InStock')";
        String sqlInsertVehicle = "INSERT INTO Vehicle (VIN, ModelID, ColorID, ManufactureYear) VALUES (?, ?, ?, YEAR(GETDATE()))";
        String sqlInsertInventory = "INSERT INTO DealerInventory (DealerID, VIN, ReceivedDate, Status, Amount) VALUES (?, ?, GETDATE(), 'IN_STOCK', 0)";

        Connection conn = null;
        PreparedStatement psEVMVehicle = null;
        PreparedStatement psVehicle = null;
        PreparedStatement psInventory = null;

        try {
            conn = DBUtils.getConnection();
            System.out.println("   ✅ Database connection established");
            conn.setAutoCommit(false);
            System.out.println("   ✅ Transaction started");

            psEVMVehicle = conn.prepareStatement(sqlInsertEVMVehicle);
            psVehicle = conn.prepareStatement(sqlInsertVehicle);
            psInventory = conn.prepareStatement(sqlInsertInventory);

            for (int i = 0; i < quantity; i++) {
                // Tạo VIN unique (dùng timestamp + random)
                String vin = generateVIN(modelID, colorID);
                System.out.println("   🔑 Generated VIN #" + (i+1) + ": " + vin);

                // 1. Insert vào EVM_Vehicle (PHẢI TRƯỚC)
                psEVMVehicle.setString(1, vin);
                psEVMVehicle.setInt(2, modelID);
                psEVMVehicle.setInt(3, colorID);
                psEVMVehicle.setInt(4, versionID);
                int rowsEVM = psEVMVehicle.executeUpdate();
                System.out.println("      ➤ Insert EVM_Vehicle: " + (rowsEVM > 0 ? "SUCCESS" : "FAILED"));

                // 2. Insert vào Vehicle
                psVehicle.setString(1, vin);
                psVehicle.setInt(2, modelID);
                psVehicle.setInt(3, colorID);
                int rowsVehicle = psVehicle.executeUpdate();
                System.out.println("      ➤ Insert Vehicle: " + (rowsVehicle > 0 ? "SUCCESS" : "FAILED"));

                // 3. Insert vào DealerInventory
                psInventory.setInt(1, dealerID);
                psInventory.setString(2, vin);
                int rowsInventory = psInventory.executeUpdate();
                System.out.println("      ➤ Insert Inventory: " + (rowsInventory > 0 ? "SUCCESS" : "FAILED"));
            }

            conn.commit();
            System.out.println("   ✅ Transaction committed successfully!");
            System.out.println("   🎉 Added " + quantity + " vehicles to inventory");
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
                if (psEVMVehicle != null) psEVMVehicle.close();
                if (psVehicle != null) psVehicle.close();
                if (psInventory != null) psInventory.close();
                if (conn != null) conn.close();
                System.out.println("   🔒 Resources closed");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // ✅ Validate ModelID và ColorID tồn tại trong database
    private boolean validateModelAndColor(int modelID, int colorID) {
        String sqlCheckModel = "SELECT COUNT(*) FROM VehicleModel WHERE ModelID = ?";
        String sqlCheckColor = "SELECT COUNT(*) FROM VehicleColor WHERE ColorID = ?";

        try (Connection conn = DBUtils.getConnection()) {
            // Kiểm tra ModelID
            try (PreparedStatement ps = conn.prepareStatement(sqlCheckModel)) {
                ps.setInt(1, modelID);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        System.out.println("   ❌ ModelID " + modelID + " không tồn tại trong VehicleModel");
                        return false;
                    }
                }
            }

            // Kiểm tra ColorID
            try (PreparedStatement ps = conn.prepareStatement(sqlCheckColor)) {
                ps.setInt(1, colorID);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        System.out.println("   ❌ ColorID " + colorID + " không tồn tại trong VehicleColor");
                        return false;
                    }
                }
            }

            System.out.println("   ✅ Validation passed - ModelID và ColorID hợp lệ");
            return true;

        } catch (SQLException e) {
            System.out.println("   ❌ Lỗi khi validate ModelID/ColorID:");
            e.printStackTrace();
            return false;
        }
    }


    // ✅ Tạo VIN unique theo format: VINM{ModelID}C{ColorID}-{timestamp}
    private String generateVIN(int modelID, int colorID) {
        long timestamp = System.currentTimeMillis();
        // Thêm số ngẫu nhiên để đảm bảo unique khi tạo nhiều VIN cùng lúc
        int random = (int)(Math.random() * 1000);
        return String.format("VINM%dC%d-%d%03d", modelID, colorID, timestamp % 100000000, random);
    }
}
