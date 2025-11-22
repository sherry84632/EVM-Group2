package com.dealermanagementsysstem.project.Model;

import org.springframework.stereotype.Repository;
import utils.DBUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class DAOVehicleVersionLookup {

    public Integer getModelIdByVersionId(int versionId){
        String sql = "SELECT ModelID FROM VehicleVersion WHERE VersionID=?";
        try(Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setInt(1, versionId);
            try(ResultSet rs = ps.executeQuery()){
                if(rs.next()) return rs.getInt("ModelID");
            }
        }catch(SQLException e){ e.printStackTrace(); }
        return null;
    }
}

