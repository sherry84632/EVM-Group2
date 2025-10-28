package com.dealermanagementsysstem.project.Model;

import org.springframework.stereotype.Repository;
import utils.DBUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class DAOVehicleVersionLookup {

    public List<DTOVehicleVersion> getVersionsByModelId(int modelId){
        List<DTOVehicleVersion> list = new ArrayList<>();
        String sql = "SELECT VersionID, VersionName, Engine, Transmission FROM VehicleVersion WHERE ModelID=? ORDER BY VersionID";
        try(Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setInt(1, modelId);
            try(ResultSet rs = ps.executeQuery()){
                while(rs.next()){
                    DTOVehicleVersion v = new DTOVehicleVersion();
                    v.setVersionID(rs.getInt("VersionID"));
                    v.setVersionName(rs.getString("VersionName"));
                    v.setEngine(rs.getString("Engine"));
                    v.setTransmission(rs.getString("Transmission"));
                    list.add(v);
                }
            }
        }catch(SQLException e){ e.printStackTrace(); }
        return list;
    }

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

