package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class DAOVehicleVersion {


    //  Lấy VehicleVersion theo ID
    public DTOVehicleVersion getVersionById(int versionID) {
        String sql = """
                    SELECT vv.VersionID, vv.VersionName, vv.ModelID,
                           vm.ModelID, vm.ModelName, vm.BasePrice
                    FROM VehicleVersion vv
                    LEFT JOIN VehicleModel vm ON vv.ModelID = vm.ModelID
                    WHERE vv.VersionID = ?
                """;
        
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, versionID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DTOVehicleVersion version = new DTOVehicleVersion();
                    version.setVersionID(rs.getInt("VersionID"));
                    version.setVersionName(rs.getString("VersionName"));
                    
                    // Set model relationship
                    if (rs.getString("ModelName") != null) {
                        DTOVehicleModel model = new DTOVehicleModel();
                        model.setModelID(rs.getInt("ModelID"));
                        model.setModelName(rs.getString("ModelName"));
                        model.setBasePrice(rs.getBigDecimal("BasePrice"));
                        version.setModel(model);
                    }
                    
                    return version;
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


}
