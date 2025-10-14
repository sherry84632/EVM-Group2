package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DAOVehicle {
    public List<DTOVehicle> getVehicles() {
        List<DTOVehicle> vehicles = new ArrayList<>();

        String sql = """
            SELECT v.VIN,
                   v.ColorID,
                   c.ColorName,
                   v.ManufactureYear,
                   v.EngineNumber,
                   v.CurrentOwner,
                   v.Status,
                   m.ModelID,
                   m.ModelName,
                   m.BasePrice
            FROM Vehicle v
            LEFT JOIN VehicleColor c ON v.ColorID = c.ColorID
            LEFT JOIN VehicleModel m ON v.ModelID = m.ModelID
        """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                DTOVehicle v = new DTOVehicle();
                v.setVIN(rs.getString("VIN"));
                v.setColorID(rs.getInt("ColorID"));
                v.setColorName(rs.getString("ColorName")); // thêm field mới
                v.setManufactureYear(rs.getInt("ManufactureYear"));
                v.setEngineNumber(rs.getString("EngineNumber"));
                v.setCurrentOwner(rs.getString("CurrentOwner"));
                v.setStatus(rs.getString("Status"));
                v.setModelID(rs.getInt("ModelID")); // thêm field mới
                v.setModelName(rs.getString("ModelName"));
                v.setBasePrice(rs.getBigDecimal("BasePrice"));
                vehicles.add(v);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
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

}
