package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Repository
public class DAODealerInventory {

    private static final Logger log = LoggerFactory.getLogger(DAODealerInventory.class);

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
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DTODealerInventory dto = new DTODealerInventory();
                    dto.setDealerInventoryID(rs.getInt("DealerInventoryID"));
                    dto.setReceivedDate(rs.getDate("ReceivedDate"));
                    dto.setStatus(DealerInventoryStatus.valueOf(rs.getString("Status")));

                    DTODealer dealer = new DTODealer();
                    dealer.setDealerID(rs.getInt("DealerID"));
                    dto.setDealer(dealer);

                    DTOVehicle vehicle = new DTOVehicle();
                    vehicle.setVIN(rs.getString("VIN"));
                    vehicle.setManufactureYear(rs.getInt("ManufactureYear"));
                    vehicle.setEngineNumber(rs.getString("EngineNumber"));
                    vehicle.setStatus(VehicleStatus.valueOf(rs.getString("VehicleStatus")));
                    dto.setVehicle(vehicle);

                    if (rs.getString("ColorName") != null) {
                        DTOVehicleColor color = new DTOVehicleColor();
                        color.setColorID(rs.getInt("ColorID"));
                        color.setColorName(rs.getString("ColorName"));
                        vehicle.setColor(color);
                    }
                    if (rs.getString("VersionName") != null) {
                        DTOVehicleVersion version = new DTOVehicleVersion();
                        version.setVersionID(rs.getInt("VersionID"));
                        version.setVersionName(rs.getString("VersionName"));
                        vehicle.setVersion(version);
                    }
                    list.add(dto);
                }
            }
        } catch (SQLException e) {
            log.error("Error fetching inventory for dealerID={}", dealerID, e);
        }
        return list;
    }

    // ✅ Xóa xe khỏi Inventory theo VIN (khi SaleOrder được Confirmed)
    public boolean removeVehicleByVIN(String vin) {
        String sql = "DELETE FROM DealerInventory WHERE VIN = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, vin);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                log.info("Removed vehicle from inventory VIN={}", vin);
            } else {
                log.warn("No inventory record removed VIN={}", vin);
            }
            return ok;
        } catch (SQLException e) {
            log.error("Error removing vehicle from inventory VIN={}", vin, e);
            return false;
        }
    }

    // ✅ Thêm xe vào Inventory (khi PurchaseOrder được Approved)
    public boolean addVehiclesToInventory(int dealerID, int colorID, int versionID, int quantity) {
        log.info("Adding {} vehicles to inventory dealerID={}, colorID={}, versionID={}", quantity, dealerID, colorID, versionID);
        if (!validateColorAndVersion(colorID, versionID)) {
            log.warn("Validation failed for colorID={} versionID={}", colorID, versionID);
            return false;
        }
        String sqlInsertVehicle = """
                    INSERT INTO Vehicle (VIN, ColorID, VersionID, ManufactureYear, EngineNumber, Status, CreatedAt, UpdatedAt) 
                    VALUES (?, ?, ?, YEAR(GETDATE()), ?, 'IN_STOCK', GETDATE(), GETDATE())
                """;
        String sqlInsertInventory = """
                    INSERT INTO DealerInventory (DealerID, VIN, ReceivedDate, Status) 
                    VALUES (?, ?, GETDATE(), 'AVAILABLE')
                """;
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement psVehicle = conn.prepareStatement(sqlInsertVehicle);
             PreparedStatement psInventory = conn.prepareStatement(sqlInsertInventory)) {
            conn.setAutoCommit(false);
            for (int i = 0; i < quantity; i++) {
                String vin = generateVIN(colorID, versionID);
                psVehicle.setString(1, vin);
                psVehicle.setInt(2, colorID);
                psVehicle.setInt(3, versionID);
                psVehicle.setString(4, "ENG" + System.currentTimeMillis() + i);
                psVehicle.executeUpdate();
                psInventory.setInt(1, dealerID);
                psInventory.setString(2, vin);
                psInventory.executeUpdate();
            }
            conn.commit();
            log.info("Successfully added {} vehicles to inventory dealerID={}", quantity, dealerID);
            return true;
        } catch (SQLException e) {
            log.error("Error adding vehicles to inventory dealerID={}", dealerID, e);
            return false;
        }
    }

    // ✅ Validate ColorID và VersionID tồn tại trong database
    private boolean validateColorAndVersion(int colorID, int versionID) {
        String sqlCheckColor = "SELECT COUNT(*) FROM VehicleColor WHERE ColorID = ?";
        String sqlCheckVersion = "SELECT COUNT(*) FROM VehicleVersion WHERE VersionID = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement psColor = conn.prepareStatement(sqlCheckColor);
             PreparedStatement psVersion = conn.prepareStatement(sqlCheckVersion)) {
            psColor.setInt(1, colorID);
            try (ResultSet rs = psColor.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    return false;
                }
            }
            psVersion.setInt(1, versionID);
            try (ResultSet rs = psVersion.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    return false;
                }
            }
            return true;
        } catch (SQLException e) {
            log.error("Error validating ColorID={} VersionID={}", colorID, versionID, e);
            return false;
        }
    }

    // ✅ Tạo VIN unique theo format: VIN{ColorID}V{VersionID}-{timestamp}
    private String generateVIN(int colorID, int versionID) {
        long timestamp = System.currentTimeMillis();
        int random = (int) (Math.random() * 1000);
        return String.format("VIN%dV%d-%d%03d", colorID, versionID, timestamp % 100000000, random);
    }
}
