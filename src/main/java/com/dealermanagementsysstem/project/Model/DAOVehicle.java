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

}
