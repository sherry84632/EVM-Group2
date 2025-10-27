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
        try (Connection con = DBUtils.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (v.getColor() != null && v.getColor().getColorID() > 0) ps.setInt(1, v.getColor().getColorID()); else ps.setNull(1, Types.INTEGER);
            if (v.getVersion() != null && v.getVersion().getVersionID() > 0) ps.setInt(2, v.getVersion().getVersionID()); else ps.setNull(2, Types.INTEGER);
            ps.setInt(3, v.getManufactureYear());
            ps.setString(4, v.getEngineNumber());
            ps.setString(5, v.getStatus().toString());
            ps.setString(6, v.getDescription());
            ps.setTimestamp(7, v.getUpdatedAt());
            ps.setInt(8, v.getVehicleID());
            boolean ok = ps.executeUpdate() > 0;
            if (ok) log.info("Vehicle updated ID={}", v.getVehicleID());
            else log.warn("Vehicle update affected 0 rows ID={}", v.getVehicleID());
            return ok;
        } catch (SQLException e) {
            log.error("Error updating vehicle ID={}", v.getVehicleID(), e);
        }
        return false;
    }

    public boolean deleteVehicle(Integer id) {
        String sql = "DELETE FROM Vehicle WHERE VehicleID = ?";
        try (Connection con = DBUtils.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) { log.info("Vehicle deleted ID={}", id); }
            else { log.warn("No vehicle deleted ID={}", id); }
            return ok;
        } catch (SQLException e) {
            log.error("Error deleting vehicle ID={}", id, e);
        }
        return false;
    }

    public List<DTOVehicle> getAllVehicles() { return getVehicles(); }

    public List<DTOVehicle> getVehiclesByStatus(VehicleStatus status) {
        String sql = BASE_SELECT + " WHERE v.Status = ? ORDER BY v.CreatedAt DESC";
        List<DTOVehicle> list = new ArrayList<>();
        try (Connection con = DBUtils.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status.toString()); try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapVehicle(rs)); }
        } catch (SQLException e) { log.error("Error fetching vehicles by status {}", status, e); }
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
}
