package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Repository
public class DAOVehicle {

    private static final Logger log = LoggerFactory.getLogger(DAOVehicle.class);

    private static final String BASE_SELECT = """
            SELECT 
                v.VehicleID,
                v.ManufactureYear,
                v.EngineNumber,
                v.Status,
                v.Description,
                v.CreatedAt,
                v.UpdatedAt,
                vc.ColorID,
                vc.ColorName,
                vv.VersionID,
                vv.VersionName,
                vv.Engine,
                vv.Transmission,
                vm.ModelID,
                vm.ModelName,
                vm.Brand,
                vm.Year,
                vm.BasePrice,
                vm.BodyType,
                vm.Description AS ModelDescription
            FROM Vehicle v
            LEFT JOIN VehicleColor vc ON v.ColorID = vc.ColorID
            LEFT JOIN VehicleVersion vv ON v.VersionID = vv.VersionID
            LEFT JOIN VehicleModel vm ON vv.ModelID = vm.ModelID
            """;

    public List<DTOVehicle> getVehicles() {
        String sql = BASE_SELECT + " ORDER BY v.CreatedAt DESC";
        List<DTOVehicle> vehicles = new ArrayList<>();
        try (Connection con = DBUtils.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) vehicles.add(mapVehicle(rs));
        } catch (SQLException e) { log.error("Error fetching vehicles", e); }
        return vehicles;
    }

    public List<DTOVehicle> searchVehiclesByModelName(String keyword) {
        String sql = BASE_SELECT + " WHERE vm.ModelName LIKE ? ORDER BY v.CreatedAt DESC";
        List<DTOVehicle> vehicles = new ArrayList<>();
        try (Connection con = DBUtils.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) vehicles.add(mapVehicle(rs)); }
        } catch (SQLException e) { log.error("Error searching vehicles by model name: {}", keyword, e); }
        return vehicles;
    }

    public void insertVehicle(DTOVehicle v) {
        String sql = "INSERT INTO Vehicle (ColorID, VersionID, ManufactureYear, EngineNumber, Status, Description, CreatedAt, UpdatedAt) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (v.getColor() != null && v.getColor().getColorID() > 0) {
                ps.setInt(1, v.getColor().getColorID());
            } else { ps.setNull(1, Types.INTEGER); }
            if (v.getVersion() != null && v.getVersion().getVersionID() > 0) {
                ps.setInt(2, v.getVersion().getVersionID());
            } else { ps.setNull(2, Types.INTEGER); }
            ps.setInt(3, v.getManufactureYear());
            ps.setString(4, v.getEngineNumber());
            ps.setString(5, v.getStatus().toString());
            ps.setString(6, v.getDescription());
            ps.setTimestamp(7, v.getCreatedAt());
            ps.setTimestamp(8, v.getUpdatedAt());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        v.setVehicleID(rs.getInt(1));
                        log.info("Vehicle inserted successfully ID={}", v.getVehicleID());
                    }
                }
            } else {
                log.warn("No vehicle row inserted");
            }
        } catch (SQLException e) {
            log.error("Error inserting vehicle", e);
        }
    }

    public Integer getModelIdByName(String modelName) {
        String sql = "SELECT ModelID FROM VehicleModel WHERE ModelName = ?";
        try (Connection con = DBUtils.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, modelName); try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt("ModelID"); }
        } catch (SQLException e) { log.error("Error getting ModelID by name {}", modelName, e); }
        return null;
    }

    public Integer getColorIdByName(String colorName) {
        String sql = "SELECT ColorID FROM VehicleColor WHERE ColorName = ?";
        try (Connection con = DBUtils.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, colorName); try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt("ColorID"); }
        } catch (SQLException e) { log.error("Error getting ColorID by name {}", colorName, e); }
        return null;
    }

    /**
     * Tìm VehicleID available theo tên model (để dùng cho test drive)
     * Trả về VehicleID đầu tiên tìm thấy có status Available
     */
    public Integer findAvailableVehicleByModelName(String modelName) {
        if (modelName == null || modelName.trim().isEmpty()) {
            return null;
        }

        String sql = """
            SELECT TOP 1 v.VehicleID 
            FROM Vehicle v
            INNER JOIN VehicleVersion vv ON v.VersionID = vv.VersionID
            INNER JOIN VehicleModel vm ON vv.ModelID = vm.ModelID
            WHERE vm.ModelName LIKE ? AND v.Status = 'Available'
            ORDER BY v.CreatedAt DESC
        """;

        try (Connection con = DBUtils.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + modelName.trim() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int vehicleID = rs.getInt("VehicleID");
                    log.info("Found available vehicle ID={} for model name: {}", vehicleID, modelName);
                    return vehicleID;
                }
            }
        } catch (SQLException e) {
            log.error("Error finding available vehicle by model name: {}", modelName, e);
        }

        log.warn("No available vehicle found for model name: {}", modelName);
        return null;
    }

    public Integer findAvailableVehicleByVersionAndColor(Integer versionId, Integer colorId) {
        if (versionId == null || colorId == null) return null;
        String sql = "SELECT TOP 1 VehicleID FROM Vehicle WHERE VersionID=? AND ColorID=? AND Status='Available' ORDER BY CreatedAt ASC";
        try (Connection con = DBUtils.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, versionId);
            ps.setInt(2, colorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            log.error("Error finding available vehicle by version {} color {}", versionId, colorId, e);
        }
        return null;
    }

    public List<Integer> findAvailableVehicleIdsByVersionAndColor(int versionId, int colorId, int limit) {
        List<Integer> ids = new ArrayList<>();
        if (limit <= 0) return ids;
        String sql = "SELECT TOP " + limit + " VehicleID FROM Vehicle WHERE VersionID=? AND ColorID=? AND Status='Available' ORDER BY CreatedAt ASC";
        try (Connection con = DBUtils.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, versionId);
            ps.setInt(2, colorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            log.error("Error fetching multiple available vehicles version {} color {} limit {}", versionId, colorId, limit, e);
        }
        return ids;
    }

    public DTOVehicle getVehicleById(Integer id) {
        String sql = BASE_SELECT + " WHERE v.VehicleID = ?";
        try (Connection con = DBUtils.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapVehicle(rs);
            }
        } catch (SQLException e) {
            log.error("Error fetching vehicle by ID {}", id, e);
        }
        return null;
    }

    public boolean updateVehicle(DTOVehicle v) {
        String sql = "UPDATE Vehicle SET ColorID=?, VersionID=?, ManufactureYear=?, EngineNumber=?, Status=?, Description=?, UpdatedAt=? WHERE VehicleID=?";

        log.debug("Updating vehicle ID={} with ColorID={}, VersionID={}, Year={}, Status={}",
                v.getVehicleID(),
                v.getColor() != null ? v.getColor().getColorID() : "null",
                v.getVersion() != null ? v.getVersion().getVersionID() : "null",
                v.getManufactureYear(),
                v.getStatus());

        try (Connection con = DBUtils.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            // ColorID
            if (v.getColor() != null && v.getColor().getColorID() > 0) {
                ps.setInt(1, v.getColor().getColorID());
                log.debug("Setting ColorID={}", v.getColor().getColorID());
            } else {
                ps.setNull(1, Types.INTEGER);
                log.debug("Setting ColorID=NULL");
            }

            // VersionID
            if (v.getVersion() != null && v.getVersion().getVersionID() > 0) {
                ps.setInt(2, v.getVersion().getVersionID());
                log.debug("Setting VersionID={}", v.getVersion().getVersionID());
            } else {
                ps.setNull(2, Types.INTEGER);
                log.debug("Setting VersionID=NULL");
            }

            // Other fields
            ps.setInt(3, v.getManufactureYear());
            ps.setString(4, v.getEngineNumber());
            ps.setString(5, v.getStatus().toString());
            ps.setString(6, v.getDescription());
            ps.setTimestamp(7, v.getUpdatedAt());
            ps.setInt(8, v.getVehicleID());

            log.debug("Executing SQL: {}", sql);
            int rowsAffected = ps.executeUpdate();
            log.info("Update affected {} rows for VehicleID={}", rowsAffected, v.getVehicleID());

            boolean ok = rowsAffected > 0;
            if (ok) {
                log.info("✅ Vehicle updated successfully ID={}", v.getVehicleID());
            } else {
                log.warn("⚠️ Vehicle update affected 0 rows ID={} - vehicle may not exist", v.getVehicleID());
            }
            return ok;
        } catch (SQLException e) {
            log.error("❌ Error updating vehicle ID={}: {}", v.getVehicleID(), e.getMessage(), e);
        }
        return false;
    }

    public boolean deleteVehicle(Integer id) {
        Connection con = null;
        try {
            con = DBUtils.getConnection();
            con.setAutoCommit(false);

            log.info("Attempting to delete vehicle ID={}", id);

            // 1. Xóa các bản ghi trong DealerInventory
            String deleteDealerInventory = "DELETE FROM DealerInventory WHERE VehicleID = ?";
            try (PreparedStatement ps = con.prepareStatement(deleteDealerInventory)) {
                ps.setInt(1, id);
                int deletedInventory = ps.executeUpdate();
                log.debug("Deleted {} records from DealerInventory", deletedInventory);
            }

            // 2. Xóa các bản ghi trong SaleOrderDetail
            String deleteSaleOrderDetail = "DELETE FROM SaleOrderDetail WHERE VehicleID = ?";
            try (PreparedStatement ps = con.prepareStatement(deleteSaleOrderDetail)) {
                ps.setInt(1, id);
                int deletedSaleDetails = ps.executeUpdate();
                log.debug("Deleted {} records from SaleOrderDetail", deletedSaleDetails);
            }

            // 3. Xóa các bản ghi trong DeliveryDetail
            String deleteDeliveryDetail = "DELETE FROM DeliveryDetail WHERE VehicleID = ?";
            try (PreparedStatement ps = con.prepareStatement(deleteDeliveryDetail)) {
                ps.setInt(1, id);
                int deletedDeliveryDetails = ps.executeUpdate();
                log.debug("Deleted {} records from DeliveryDetail", deletedDeliveryDetails);
            }

            // 4. Xóa các bản ghi trong TestDrive (nếu có)
            String deleteTestDrive = "DELETE FROM TestDrive WHERE VehicleID = ?";
            try (PreparedStatement ps = con.prepareStatement(deleteTestDrive)) {
                ps.setInt(1, id);
                int deletedTestDrives = ps.executeUpdate();
                log.debug("Deleted {} records from TestDrive", deletedTestDrives);
            }

            // 5. Cuối cùng xóa Vehicle
            String deleteVehicle = "DELETE FROM Vehicle WHERE VehicleID = ?";
            try (PreparedStatement ps = con.prepareStatement(deleteVehicle)) {
                ps.setInt(1, id);
                int deletedVehicle = ps.executeUpdate();

                if (deletedVehicle > 0) {
                    con.commit();
                    log.info("✅ Vehicle deleted successfully ID={}", id);
                    return true;
                } else {
                    con.rollback();
                    log.warn("⚠️ No vehicle found to delete ID={}", id);
                    return false;
                }
            }

        } catch (SQLException e) {
            log.error("❌ Error deleting vehicle ID={}", id, e);
            if (con != null) {
                try {
                    con.rollback();
                    log.debug("Transaction rolled back");
                } catch (SQLException ex) {
                    log.error("Error during rollback", ex);
                }
            }
            return false;
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException e) {
                    log.error("Error closing connection", e);
                }
            }
        }
    }

    public List<DTOVehicle> getAllVehicles() { return getVehicles(); }

    public List<DTOVehicle> getVehiclesByStatus(VehicleStatus status) {
        // Use UPPER() for case-insensitive comparison
        String sql = BASE_SELECT + " WHERE UPPER(v.Status) = UPPER(?) ORDER BY v.CreatedAt DESC";
        List<DTOVehicle> list = new ArrayList<>();

        log.debug("Querying vehicles with status: {}", status);

        try (Connection con = DBUtils.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status.toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DTOVehicle vehicle = mapVehicle(rs);
                    list.add(vehicle);
                }
            }

            log.info("Found {} vehicles with status {}", list.size(), status);
        } catch (SQLException e) {
            log.error("Error fetching vehicles by status {}", status, e);
        }

        return list;
    }

    public List<DTOVehicle> getVehiclesByDealer(int dealerID) {
        String sql = BASE_SELECT + " WHERE v.CurrentDealerID = ? ORDER BY v.CreatedAt DESC";
        List<DTOVehicle> list = new ArrayList<>();
        try (Connection con = DBUtils.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, dealerID); try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapVehicle(rs)); }
        } catch (SQLException e) { log.error("Error fetching vehicles by dealerID {}", dealerID, e); }
        return list;
    }

    // =====================
    // Helper to map ResultSet -> DTOVehicle
    // =====================
    private DTOVehicle mapVehicle(ResultSet rs) throws SQLException {
        DTOVehicle v = new DTOVehicle();
        v.setVehicleID(rs.getInt("VehicleID"));
        v.setManufactureYear(rs.getInt("ManufactureYear"));
        v.setEngineNumber(rs.getString("EngineNumber"));
        v.setStatus(VehicleStatus.valueOf(rs.getString("Status")));
        v.setDescription(rs.getString("Description"));
        v.setCreatedAt(rs.getTimestamp("CreatedAt"));
        v.setUpdatedAt(rs.getTimestamp("UpdatedAt"));

        if (rs.getString("ColorName") != null) {
            DTOVehicleColor color = new DTOVehicleColor();
            color.setColorID(rs.getInt("ColorID"));
            color.setColorName(rs.getString("ColorName"));
            v.setColor(color);
        }
        if (rs.getString("VersionName") != null) {
            DTOVehicleVersion version = new DTOVehicleVersion();
            version.setVersionID(rs.getInt("VersionID"));
            version.setVersionName(rs.getString("VersionName"));
            version.setEngine(rs.getString("Engine"));
            version.setTransmission(rs.getString("Transmission"));
            if (rs.getString("ModelName") != null) {
                DTOVehicleModel model = new DTOVehicleModel();
                model.setModelID(rs.getInt("ModelID"));
                model.setModelName(rs.getString("ModelName"));
                model.setBrand(rs.getString("Brand"));
                model.setYear(rs.getInt("Year"));
                model.setBasePrice(rs.getBigDecimal("BasePrice"));
                model.setBodyType(rs.getString("BodyType"));
                model.setDescription(rs.getString("ModelDescription"));
                version.setModel(model);
            }
            v.setVersion(version);
        }
        return v;
    }

    // =====================
    // Auto-Create Methods
    // =====================

    /**
     * Get or Create VehicleModel
     * Returns existing ModelID or creates new one
     */
    public Integer getOrCreateModel(String modelName, String brand, String bodyType,
                                    int year, java.math.BigDecimal basePrice, String description) {
        Integer modelID = getModelIdByName(modelName);
        if (modelID != null) {
            // ensure EvmID=1
            String check = "SELECT EvmID FROM VehicleModel WHERE ModelID=?";
            try (Connection c=DBUtils.getConnection(); PreparedStatement ps=c.prepareStatement(check)) {
                ps.setInt(1, modelID);
                try(ResultSet rs=ps.executeQuery()){ if(rs.next()){ int evm=rs.getInt(1); if(evm==0){ try(PreparedStatement ups=c.prepareStatement("UPDATE VehicleModel SET EvmID=1 WHERE ModelID=?")){ ups.setInt(1, modelID); ups.executeUpdate(); log.info("Patched EvmID=1 for existing modelID={}", modelID);} } } }
            } catch(SQLException ex){ log.warn("Could not patch EvmID for modelID={}", modelID, ex); }
            log.info("Model already exists: {} (ID={})", modelName, modelID);
            return modelID;
        }
        String sql = "INSERT INTO VehicleModel (ModelName, Brand, BodyType, Year, BasePrice, Description, EvmID) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, modelName);
            ps.setString(2, brand);
            ps.setString(3, bodyType);
            ps.setInt(4, year);
            ps.setBigDecimal(5, basePrice);
            ps.setString(6, description);
            ps.setInt(7, 1); // force EvmID = 1
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        modelID = rs.getInt(1);
                        log.info("Created new VehicleModel with EvmID=1: {} (ID={})", modelName, modelID);
                        return modelID;
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Error creating VehicleModel: {}", modelName, e);
        }
        return null;
    }

    /**
     * Get or Create VehicleVersion
     * Returns existing VersionID or creates new one
     */
    public Integer getOrCreateVersion(Integer modelID, String versionName,
                                      String engine, String transmission) {
        if (modelID == null) {
            log.warn("Cannot create version without modelID");
            return null;
        }

        // Check if exists
        String checkSql = "SELECT VersionID FROM VehicleVersion WHERE ModelID = ? AND VersionName = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setInt(1, modelID);
            ps.setString(2, versionName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Integer versionID = rs.getInt("VersionID");
                    log.info("Version already exists: {} (ID={})", versionName, versionID);
                    return versionID;
                }
            }
        } catch (SQLException e) {
            log.error("Error checking version existence", e);
        }

        // Create new
        String insertSql = "INSERT INTO VehicleVersion (ModelID, VersionName, Engine, Transmission) " +
                          "VALUES (?, ?, ?, ?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, modelID);
            ps.setString(2, versionName);
            ps.setString(3, engine);
            ps.setString(4, transmission);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        Integer versionID = rs.getInt(1);
                        log.info("Created new VehicleVersion: {} (ID={})", versionName, versionID);
                        return versionID;
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Error creating VehicleVersion: {}", versionName, e);
        }
        return null;
    }

    /**
     * Get or Create VehicleColor
     * Returns existing ColorID or creates new one
     */
    public Integer getOrCreateColor(String colorName) {
        // Check if exists
        Integer colorID = getColorIdByName(colorName);
        if (colorID != null) {
            log.info("Color already exists: {} (ID={})", colorName, colorID);
            return colorID;
        }

        // Create new
        String sql = "INSERT INTO VehicleColor (ColorName) VALUES (?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, colorName);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        colorID = rs.getInt(1);
                        log.info("Created new VehicleColor: {} (ID={})", colorName, colorID);
                        return colorID;
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Error creating VehicleColor: {}", colorName, e);
        }
        return null;
    }

    /**
     * Update VehicleModel fields (brand, bodyType, year, basePrice, description)
     */
    public boolean updateModel(Integer modelID, String brand, String bodyType,
                               int year, java.math.BigDecimal basePrice, String description) {
        if (modelID == null || modelID <= 0) {
            log.warn("Cannot update model with invalid modelID");
            return false;
        }

        String sql = "UPDATE VehicleModel SET Brand=?, BodyType=?, Year=?, BasePrice=?, Description=? WHERE ModelID=?";

        log.debug("Updating VehicleModel ID={} with Brand={}, BodyType={}, Year={}, BasePrice={}",
                modelID, brand, bodyType, year, basePrice);

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, brand);
            ps.setString(2, bodyType);
            ps.setInt(3, year);
            ps.setBigDecimal(4, basePrice);
            ps.setString(5, description);
            ps.setInt(6, modelID);

            int rowsAffected = ps.executeUpdate();
            log.info("Update VehicleModel affected {} rows for ModelID={}", rowsAffected, modelID);

            return rowsAffected > 0;
        } catch (SQLException e) {
            log.error("Error updating VehicleModel ID={}: {}", modelID, e.getMessage(), e);
        }
        return false;
    }

    /**
     * Update VehicleVersion fields (engine, transmission)
     */
    public boolean updateVersion(Integer versionID, String engine, String transmission) {
        if (versionID == null || versionID <= 0) {
            log.warn("Cannot update version with invalid versionID");
            return false;
        }

        String sql = "UPDATE VehicleVersion SET Engine=?, Transmission=? WHERE VersionID=?";

        log.debug("Updating VehicleVersion ID={} with Engine={}, Transmission={}",
                versionID, engine, transmission);

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, engine);
            ps.setString(2, transmission);
            ps.setInt(3, versionID);

            int rowsAffected = ps.executeUpdate();
            log.info("Update VehicleVersion affected {} rows for VersionID={}", rowsAffected, versionID);

            return rowsAffected > 0;
        } catch (SQLException e) {
            log.error("Error updating VehicleVersion ID={}: {}", versionID, e.getMessage(), e);
        }
        return false;
    }

    /**
     * Update ModelImage cho VehicleModel
     * @param modelID ID của model
     * @param imageBytes Byte array của ảnh
     * @return true nếu update thành công
     */
    public boolean updateModelImage(Integer modelID, byte[] imageBytes) {
        if (modelID == null || modelID <= 0) {
            log.warn("Cannot update model image with invalid modelID");
            return false;
        }

        String sql = "UPDATE VehicleModel SET ModelImage=? WHERE ModelID=?";

        log.debug("Updating ModelImage for ModelID={}", modelID);

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            if (imageBytes != null && imageBytes.length > 0) {
                ps.setBytes(1, imageBytes);
            } else {
                ps.setNull(1, java.sql.Types.VARBINARY);
            }
            ps.setInt(2, modelID);

            int rowsAffected = ps.executeUpdate();
            log.info("✅ Updated ModelImage for ModelID={}, affected {} rows", modelID, rowsAffected);

            return rowsAffected > 0;
        } catch (SQLException e) {
            log.error("❌ Error updating ModelImage for ModelID={}: {}", modelID, e.getMessage(), e);
        }
        return false;
    }

    /**
     * Lấy ModelImage từ database theo ModelID
     * @param modelID ID của model
     * @return byte array của ảnh, hoặc null nếu không có
     */
    public byte[] getModelImage(Integer modelID) {
        if (modelID == null || modelID <= 0) {
            log.warn("Cannot get model image with invalid modelID");
            return null;
        }

        String sql = "SELECT ModelImage FROM VehicleModel WHERE ModelID=?";

        log.debug("Getting ModelImage for ModelID={}", modelID);

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, modelID);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    byte[] imageBytes = rs.getBytes("ModelImage");
                    if (imageBytes != null) {
                        log.info("✅ Retrieved ModelImage for ModelID={}, size={} bytes", modelID, imageBytes.length);
                        return imageBytes;
                    } else {
                        log.info("ℹ️ No image found for ModelID={}", modelID);
                        return null;
                    }
                }
            }
        } catch (SQLException e) {
            log.error("❌ Error getting ModelImage for ModelID={}: {}", modelID, e.getMessage(), e);
        }
        return null;
    }

    public List<Integer> findVehicleIdsByVersionAndColorAllStatuses(int versionId, int colorId, int limit) {
        List<Integer> ids = new ArrayList<>();
        if (limit <= 0) return ids;
        String sql = "SELECT TOP " + limit + " VehicleID FROM Vehicle WHERE VersionID=? AND ColorID=? ORDER BY CreatedAt ASC";
        try (Connection con = DBUtils.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, versionId); ps.setInt(2, colorId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) ids.add(rs.getInt(1)); }
        } catch (SQLException e) { log.error("Error fetching ANY status vehicles version {} color {} limit {}", versionId, colorId, limit, e); }
        return ids;
    }
}
