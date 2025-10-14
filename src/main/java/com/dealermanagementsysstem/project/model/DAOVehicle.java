package com.dealermanagementsysstem.project.Model;
import com.dealermanagementsysstem.project.Model.DTOVehicle;

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
        String sql = "SELECT VIN, ColorID, ManufactureYear, EngineNumber, CurrentOwner, Status FROM Vehicle";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery();
        ) {
            while (rs.next()) {
                DTOVehicle v = new DTOVehicle();
                v.setVIN(rs.getString("VIN"));
                v.setColorID(rs.getInt("ColorID"));
                v.setManufactureYear(rs.getInt("ManufactureYear"));
                v.setEngineNumber(rs.getString("EngineNumber"));
                v.setCurrentOwner(rs.getString("CurrentOwner"));
                v.setStatus(rs.getString("Status"));
                vehicles.add(v);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return vehicles;
    }
}
