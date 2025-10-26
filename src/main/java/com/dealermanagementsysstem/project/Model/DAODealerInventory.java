package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DAODealerInventory {

    // ✅ Lấy danh sách xe theo DealerID
    public List<DTODealerInventory> getVehiclesByDealerID(int dealerID) {
        List<DTODealerInventory> list = new ArrayList<>();
        String sql = """
                    SELECT di.DealerInventoryID, di.DealerID, di.VIN, di.ReceivedDate, di.Status,
                           v.ManufactureYear, v.EngineNumber, v.Status AS VehicleStatus,
                           vc.ColorID, vc.ColorName,
                           vv.VersionID, vv.VersionName,
                           vm.ModelID, vm.ModelName
                    FROM DealerInventory di
                    LEFT JOIN Vehicle v ON di.VIN = v.VIN
                    LEFT JOIN VehicleColor vc ON v.ColorID = vc.ColorID
                    LEFT JOIN VehicleVersion vv ON v.VersionID = vv.VersionID
                    LEFT JOIN VehicleModel vm ON vv.ModelID = vm.ModelID
                    WHERE di.DealerID = ?
                    ORDER BY di.ReceivedDate DESC
                """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, dealerID);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                DTODealerInventory dto = new DTODealerInventory();
                dto.setDealerInventoryID(rs.getInt("DealerInventoryID"));
                dto.setReceivedDate(rs.getDate("ReceivedDate"));
                dto.setStatus(DealerInventoryStatus.valueOf(rs.getString("Status")));

                // Set dealer relationship
                DTODealer dealer = new DTODealer();
                dealer.setDealerID(rs.getInt("DealerID"));
                dto.setDealer(dealer);

                // Set vehicle relationship
                DTOVehicle vehicle = new DTOVehicle();
                vehicle.setVIN(rs.getString("VIN"));
                vehicle.setManufactureYear(rs.getInt("ManufactureYear"));
                vehicle.setEngineNumber(rs.getString("EngineNumber"));
                vehicle.setStatus(VehicleStatus.valueOf(rs.getString("VehicleStatus")));
                dto.setVehicle(vehicle);

                // Set color relationship
                if (rs.getString("ColorName") != null) {
                    DTOVehicleColor color = new DTOVehicleColor();
                    color.setColorID(rs.getInt("ColorID"));
                    color.setColorName(rs.getString("ColorName"));
                    vehicle.setColor(color);
                }

                // Set version relationship
                if (rs.getString("VersionName") != null) {
                    DTOVehicleVersion version = new DTOVehicleVersion();
                    version.setVersionID(rs.getInt("VersionID"));
                    version.setVersionName(rs.getString("VersionName"));
                    vehicle.setVersion(version);
                }

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
    public boolean addVehiclesToInventory(int dealerID, int colorID, int versionID, int quantity) {
        System.out.println("🔧 [DAODealerInventory] addVehiclesToInventory called:");
        System.out.println("   DealerID: " + dealerID);
        System.out.println("   ColorID: " + colorID);
        System.out.println("   VersionID: " + versionID);
        System.out.println("   Quantity: " + quantity);

        // ✅ VALIDATION: Kiểm tra ColorID và VersionID tồn tại
        if (!validateColorAndVersion(colorID, versionID)) {
            System.out.println("   ❌ VALIDATION FAILED: ColorID hoặc VersionID không tồn tại!");
            return false;
        }

        // Insert theo thứ tự: Vehicle → DealerInventory
        String sqlInsertVehicle = """
                    INSERT INTO Vehicle (VIN, ColorID, VersionID, ManufactureYear, EngineNumber, Status, CreatedAt, UpdatedAt) 
                    VALUES (?, ?, ?, YEAR(GETDATE()), ?, 'IN_STOCK', GETDATE(), GETDATE())
                """;
        String sqlInsertInventory = """
                    INSERT INTO DealerInventory (DealerID, VIN, ReceivedDate, Status) 
                    VALUES (?, ?, GETDATE(), 'AVAILABLE')
                """;

        Connection conn = null;
        PreparedStatement psVehicle = null;
        PreparedStatement psInventory = null;

        try {
            conn = DBUtils.getConnection();
            System.out.println("   ✅ Database connection established");
            conn.setAutoCommit(false);
            System.out.println("   ✅ Transaction started");

            psVehicle = conn.prepareStatement(sqlInsertVehicle);
            psInventory = conn.prepareStatement(sqlInsertInventory);

            for (int i = 0; i < quantity; i++) {
                // Tạo VIN unique (dùng timestamp + random)
                String vin = generateVIN(colorID, versionID);
                System.out.println("   🔑 Generated VIN #" + (i+1) + ": " + vin);

                // 1. Insert vào Vehicle
                psVehicle.setString(1, vin);
                psVehicle.setInt(2, colorID);
                psVehicle.setInt(3, versionID);
                psVehicle.setString(4, "ENG" + System.currentTimeMillis() + i); // Generate engine number
                int rowsVehicle = psVehicle.executeUpdate();
                System.out.println("      ➤ Insert Vehicle: " + (rowsVehicle > 0 ? "SUCCESS" : "FAILED"));

                // 2. Insert vào DealerInventory
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
                if (psVehicle != null) psVehicle.close();
                if (psInventory != null) psInventory.close();
                if (conn != null) conn.close();
                System.out.println("   🔒 Resources closed");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // ✅ Validate ColorID và VersionID tồn tại trong database
    private boolean validateColorAndVersion(int colorID, int versionID) {
        String sqlCheckColor = "SELECT COUNT(*) FROM VehicleColor WHERE ColorID = ?";
        String sqlCheckVersion = "SELECT COUNT(*) FROM VehicleVersion WHERE VersionID = ?";

        try (Connection conn = DBUtils.getConnection()) {
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

            // Kiểm tra VersionID
            try (PreparedStatement ps = conn.prepareStatement(sqlCheckVersion)) {
                ps.setInt(1, versionID);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        System.out.println("   ❌ VersionID " + versionID + " không tồn tại trong VehicleVersion");
                        return false;
                    }
                }
            }

            System.out.println("   ✅ Validation passed - ColorID và VersionID hợp lệ");
            return true;

        } catch (SQLException e) {
            System.out.println("   ❌ Lỗi khi validate ColorID/VersionID:");
            e.printStackTrace();
            return false;
        }
    }


    // ✅ Tạo VIN unique theo format: VIN{ColorID}V{VersionID}-{timestamp}
    private String generateVIN(int colorID, int versionID) {
        long timestamp = System.currentTimeMillis();
        // Thêm số ngẫu nhiên để đảm bảo unique khi tạo nhiều VIN cùng lúc
        int random = (int)(Math.random() * 1000);
        return String.format("VIN%dV%d-%d%03d", colorID, versionID, timestamp % 100000000, random);
    }
}
