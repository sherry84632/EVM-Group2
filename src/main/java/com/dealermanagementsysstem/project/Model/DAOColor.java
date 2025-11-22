// DAOColor.java
package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository

public class DAOColor {



    public DTOVehicleColor getColorByColorName(String colorName) {
        String sql = """
            SELECT vc.ColorID, vc.ColorName, vc.ModelID,
                   vm.ModelID, vm.ModelName
            FROM VehicleColor vc
            LEFT JOIN VehicleModel vm ON vc.ModelID = vm.ModelID
            WHERE vc.ColorName = ?
        """;
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, colorName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DTOVehicleColor color = new DTOVehicleColor();
                    color.setColorID(rs.getInt("ColorID"));
                    color.setColorName(rs.getString("ColorName"));
                    
                    // Set model relationship if available
                    if (rs.getString("ModelName") != null) {
                        DTOVehicleModel model = new DTOVehicleModel();
                        model.setModelID(rs.getInt("ModelID"));
                        model.setModelName(rs.getString("ModelName"));
                        color.setModel(model);
                    }
                    
                    return color;
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

}
