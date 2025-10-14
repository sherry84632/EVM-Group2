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
}
