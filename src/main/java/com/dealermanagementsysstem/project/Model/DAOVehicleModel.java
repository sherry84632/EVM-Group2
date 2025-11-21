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
        String sql = "SELECT ModelID, ModelName, Brand, Year, BodyType, BasePrice, Description, ModelImage FROM VehicleModel";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                DTOVehicleModel m = new DTOVehicleModel();
                m.setModelID(rs.getInt("ModelID"));
                m.setModelName(rs.getString("ModelName"));
                m.setBrand(rs.getString("Brand"));
                int yearVal = rs.getInt("Year");
                if (!rs.wasNull()) m.setYear(yearVal);
                m.setBodyType(rs.getString("BodyType"));
                m.setBasePrice(rs.getBigDecimal("BasePrice"));
                m.setDescription(rs.getString("Description"));
                byte[] img = rs.getBytes("ModelImage");
                if (img != null && img.length > 0) m.setModelImage(img);
                list.add(m);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public DTOVehicleModel getModelById(int modelId) {
        DTOVehicleModel model = null;
        String sql = "SELECT ModelID, ModelName, Brand, Year, BodyType, BasePrice, Description, ModelImage FROM VehicleModel WHERE ModelID = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, modelId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    model = new DTOVehicleModel();
                    model.setModelID(rs.getInt("ModelID"));
                    model.setModelName(rs.getString("ModelName"));
                    model.setBrand(rs.getString("Brand"));
                    model.setYear(rs.getInt("Year"));
                    model.setBodyType(rs.getString("BodyType"));
                    model.setBasePrice(rs.getBigDecimal("BasePrice"));
                    model.setDescription(rs.getString("Description"));

                    // Get image as byte array
                    byte[] imageData = rs.getBytes("ModelImage");
                    if (imageData != null) {
                        model.setModelImage(imageData);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return model;
    }
}
