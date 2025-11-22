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

    //  Lấy VehicleVersion theo tên
    public DTOVehicleVersion getVersionByName(String versionName) {
        String sql = """
                    SELECT vv.VersionID, vv.VersionName, vv.ModelID,
                           vm.ModelID, vm.ModelName, vm.BasePrice
                    FROM VehicleVersion vv
                    LEFT JOIN VehicleModel vm ON vv.ModelID = vm.ModelID
                    WHERE vv.VersionName = ?
                """;
        
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, versionName);
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

    //  Tạo VehicleVersion mới
    public boolean createVersion(DTOVehicleVersion version) {
        String sql = "INSERT INTO VehicleVersion (VersionName, ModelID) VALUES (?, ?)";
        
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, version.getVersionName());
            ps.setInt(2, version.getModel() != null ? version.getModel().getModelID() : 0);
            
            int rows = ps.executeUpdate();
            return rows > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //  Cập nhật VehicleVersion
    public boolean updateVersion(DTOVehicleVersion version) {
        String sql = "UPDATE VehicleVersion SET VersionName = ?, ModelID = ? WHERE VersionID = ?";
        
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, version.getVersionName());
            ps.setInt(2, version.getModel() != null ? version.getModel().getModelID() : 0);
            ps.setInt(3, version.getVersionID());
            
            int rows = ps.executeUpdate();
            return rows > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //  Xóa VehicleVersion
    public boolean deleteVersion(int versionID) {
        String sql = "DELETE FROM VehicleVersion WHERE VersionID = ?";
        
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, versionID);
            
            int rows = ps.executeUpdate();
            return rows > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //  Lấy VehicleVersion theo ModelID
    public List<DTOVehicleVersion> getVersionsByModel(int modelID) {
        List<DTOVehicleVersion> list = new ArrayList<>();
        String sql = """
                    SELECT vv.VersionID, vv.VersionName, vv.ModelID,
                           vm.ModelID, vm.ModelName, vm.BasePrice
                    FROM VehicleVersion vv
                    LEFT JOIN VehicleModel vm ON vv.ModelID = vm.ModelID
                    WHERE vv.ModelID = ?
                    ORDER BY vv.VersionName
                """;
        
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, modelID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
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
                    
                    list.add(version);
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
