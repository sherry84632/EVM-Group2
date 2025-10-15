package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DAOVehicle {

    public List<DTOVehicle> getVehicles() {
        List<DTOVehicle> vehicles = new ArrayList<>();

        // ✅ JOIN cả 3 bảng Vehicle, VehicleModel, VehicleColor
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

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                DTOVehicle vehicle = new DTOVehicle();

                vehicle.setVIN(rs.getString("VIN"));
                vehicle.setManufactureYear(rs.getInt("ManufactureYear"));
                vehicle.setColorID(rs.getInt("ColorID"));
                vehicle.setColorName(rs.getString("ColorName"));
                vehicle.setEngineNumber(rs.getString("EngineNumber"));
                vehicle.setCurrentOwner(rs.getString("CurrentOwner"));
                vehicle.setStatus(rs.getString("Status"));
                vehicle.setModelID(rs.getInt("ModelID"));
                vehicle.setModelName(rs.getString("ModelName"));
                vehicle.setBasePrice(rs.getBigDecimal("BasePrice"));

                vehicles.add(vehicle);
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
        WHERE vm.ModelName LIKE ?
    """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DTOVehicle vehicle = new DTOVehicle();

                    vehicle.setVIN(rs.getString("VIN"));
                    vehicle.setManufactureYear(rs.getInt("ManufactureYear"));
                    vehicle.setColorID(rs.getInt("ColorID"));
                    vehicle.setColorName(rs.getString("ColorName"));
                    vehicle.setEngineNumber(rs.getString("EngineNumber"));
                    vehicle.setCurrentOwner(rs.getString("CurrentOwner"));
                    vehicle.setStatus(rs.getString("Status"));
                    vehicle.setModelID(rs.getInt("ModelID"));
                    vehicle.setModelName(rs.getString("ModelName"));
                    vehicle.setBasePrice(rs.getBigDecimal("BasePrice"));

                    vehicles.add(vehicle);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return vehicles;
    }

    public void insertVehicle(DTOVehicle vehicle) {
        String sql = "INSERT INTO Vehicle (VIN, ColorID, ManufactureYear, EngineNumber, CurrentOwner, Status, ModelID) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, vehicle.getVIN());
            ps.setInt(2, vehicle.getColorID());
            ps.setInt(3, vehicle.getManufactureYear());
            ps.setString(4, vehicle.getEngineNumber());
            ps.setString(5, vehicle.getCurrentOwner());
            ps.setString(6, vehicle.getStatus());
            ps.setInt(7, vehicle.getModelID());

            ps.executeUpdate();
            System.out.println("✅ Vehicle inserted successfully!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Integer getModelIdByName(String modelName) {
        String sql = "SELECT ModelID FROM VehicleModel WHERE ModelName = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, modelName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("ModelID");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Integer getColorIdByName(String colorName) {
        String sql = "SELECT ColorID FROM VehicleColor WHERE ColorName = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, colorName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("ColorID");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public DTOVehicle getVehicleByVIN(String vin) {
        DTOVehicle vehicle = null;
        try (Connection conn = DBUtils.getConnection()) {
            String sql = "SELECT * FROM Vehicle WHERE VIN = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, vin);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                vehicle = new DTOVehicle();
                vehicle.setVIN(rs.getString("VIN"));
                vehicle.setColorID(rs.getInt("ColorID"));
                vehicle.setManufactureYear(rs.getInt("ManufactureYear"));
                vehicle.setEngineNumber(rs.getString("EngineNumber"));
                vehicle.setCurrentOwner(rs.getString("CurrentOwner"));
                vehicle.setStatus(rs.getString("Status"));
                vehicle.setModelID(rs.getInt("ModelID"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return vehicle;
    }

    public boolean updateVehicle(DTOVehicle vehicle) {
        try (Connection conn = DBUtils.getConnection()) {
            String sql = "UPDATE Vehicle SET ColorID = ?, ManufactureYear = ?, EngineNumber = ?, " +
                    "CurrentOwner = ?, Status = ?, ModelID = ? WHERE VIN = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, vehicle.getColorID());
            stmt.setInt(2, vehicle.getManufactureYear());
            stmt.setString(3, vehicle.getEngineNumber());
            stmt.setString(4, vehicle.getCurrentOwner());
            stmt.setString(5, vehicle.getStatus());
            stmt.setInt(6, vehicle.getModelID());
            stmt.setString(7, vehicle.getVIN());

            int rowsUpdated = stmt.executeUpdate();
            return rowsUpdated > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
