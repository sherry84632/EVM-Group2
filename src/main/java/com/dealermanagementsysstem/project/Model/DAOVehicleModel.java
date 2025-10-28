// DAOVehicleModel.java
package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository

public class DAOVehicleModel {
    public List<DTOVehicleModel> getAllModels() {
        List<DTOVehicleModel> list = new ArrayList<>();
        String sql = "SELECT ModelID, ModelName FROM VehicleModel";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                DTOVehicleModel m = new DTOVehicleModel();
                m.setModelID(Integer.parseInt(String.valueOf(rs.getInt("ModelID"))));
                m.setModelName(rs.getString("ModelName"));
                list.add(m);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
