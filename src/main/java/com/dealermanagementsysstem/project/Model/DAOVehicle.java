package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class DAOVehicle {

    public List<DTOVehicle> getVehicles() {
        List<DTOVehicle> vehicles = new ArrayList<>();

        String sql = """
            SELECT 
                v.VIN,
                v.ManufactureYear,
                v.ColorID,
                vc.ColorName,
                v.EngineNumber,
                v.CurrentOwner,
                v.Status,
                vm.ModelID,
                vm.ModelName,
                vm.BasePrice
            FROM Vehicle v
            LEFT JOIN VehicleModel vm ON v.ModelID = vm.ModelID
            LEFT JOIN VehicleColor vc ON v.ColorID = vc.ColorID
        """;

        try (PreparedStatement ps = DBUtils.createPreparedStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                DTOVehicle v = new DTOVehicle();
                v.setVIN(rs.getString("VIN"));
                v.setManufactureYear(rs.getInt("ManufactureYear"));
                v.setColorID(rs.getInt("ColorID"));
                v.setColorName(rs.getString("ColorName"));
                v.setEngineNumber(rs.getString("EngineNumber"));
                v.setCurrentOwner(rs.getString("CurrentOwner"));
                v.setStatus(rs.getString("Status"));
                v.setModelID(rs.getInt("ModelID"));
                v.setModelName(rs.getString("ModelName"));
                v.setBasePrice(rs.getBigDecimal("BasePrice"));
                vehicles.add(v);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return vehicles;
    }

    public List<DTOVehicle> searchVehiclesByModelName(String keyword) {
        List<DTOVehicle> vehicles = new ArrayList<>();
        String sql = """
            SELECT 
                v.VIN, v.ManufactureYear, v.ColorID, vc.ColorName, 
                v.EngineNumber, v.CurrentOwner, v.Status, 
                vm.ModelID, vm.ModelName, vm.BasePrice
            FROM Vehicle v
            LEFT JOIN VehicleModel vm ON v.ModelID = vm.ModelID
            LEFT JOIN VehicleColor vc ON v.ColorID = vc.ColorID
            WHERE vm.ModelName LIKE ?
        """;

        try (PreparedStatement ps = DBUtils.createPreparedStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DTOVehicle v = new DTOVehicle();
                    v.setVIN(rs.getString("VIN"));
                    v.setManufactureYear(rs.getInt("ManufactureYear"));
                    v.setColorID(rs.getInt("ColorID"));
                    v.setColorName(rs.getString("ColorName"));
                    v.setEngineNumber(rs.getString("EngineNumber"));
                    v.setCurrentOwner(rs.getString("CurrentOwner"));
                    v.setStatus(rs.getString("Status"));
                    v.setModelID(rs.getInt("ModelID"));
                    v.setModelName(rs.getString("ModelName"));
                    v.setBasePrice(rs.getBigDecimal("BasePrice"));
                    vehicles.add(v);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return vehicles;
    }

    public void insertVehicle(DTOVehicle v) {
        String sql = "INSERT INTO Vehicle (VIN, ColorID, ManufactureYear, EngineNumber, CurrentOwner, Status, ModelID) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = DBUtils.createPreparedStatement(sql)) {
            ps.setString(1, v.getVIN());
            ps.setInt(2, v.getColorID());
            ps.setInt(3, v.getManufactureYear());
            ps.setString(4, v.getEngineNumber());
            ps.setString(5, v.getCurrentOwner());
            ps.setString(6, v.getStatus());
            ps.setInt(7, v.getModelID());
            ps.executeUpdate();
            System.out.println("✅ Vehicle inserted successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Integer getModelIdByName(String modelName) {
        String sql = "SELECT ModelID FROM VehicleModel WHERE ModelName = ?";
        try (PreparedStatement ps = DBUtils.createPreparedStatement(sql)) {
            ps.setString(1, modelName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("ModelID");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Integer getColorIdByName(String colorName) {
        String sql = "SELECT ColorID FROM VehicleColor WHERE ColorName = ?";
        try (PreparedStatement ps = DBUtils.createPreparedStatement(sql)) {
            ps.setString(1, colorName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("ColorID");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public DTOVehicle getVehicleByVIN(String vin) {
        DTOVehicle v = null;
        String sql = "SELECT * FROM Vehicle WHERE VIN = ?";
        try (PreparedStatement ps = DBUtils.createPreparedStatement(sql)) {
            ps.setString(1, vin);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    v = new DTOVehicle();
                    v.setVIN(rs.getString("VIN"));
                    v.setColorID(rs.getInt("ColorID"));
                    v.setManufactureYear(rs.getInt("ManufactureYear"));
                    v.setEngineNumber(rs.getString("EngineNumber"));
                    v.setCurrentOwner(rs.getString("CurrentOwner"));
                    v.setStatus(rs.getString("Status"));
                    v.setModelID(rs.getInt("ModelID"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return v;
    }

    public boolean updateVehicle(DTOVehicle v) {
        String sql = "UPDATE Vehicle SET ColorID=?, ManufactureYear=?, EngineNumber=?, " +
                "CurrentOwner=?, Status=?, ModelID=? WHERE VIN=?";
        try (PreparedStatement ps = DBUtils.createPreparedStatement(sql)) {
            ps.setInt(1, v.getColorID());
            ps.setInt(2, v.getManufactureYear());
            ps.setString(3, v.getEngineNumber());
            ps.setString(4, v.getCurrentOwner());
            ps.setString(5, v.getStatus());
            ps.setInt(6, v.getModelID());
            ps.setString(7, v.getVIN());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteVehicle(String vin) {
        String sql = "DELETE FROM Vehicle WHERE VIN = ?";
        try (PreparedStatement ps = DBUtils.createPreparedStatement(sql)) {
            ps.setString(1, vin);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
