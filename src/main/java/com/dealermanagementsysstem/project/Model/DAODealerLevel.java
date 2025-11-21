package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class DAODealerLevel {

    /**
     * Get all dealer levels
     */
    public List<DTODealerLevel> getAllDealerLevels() {
        List<DTODealerLevel> list = new ArrayList<>();
        String sql = "SELECT LevelID, LevelName, MinOrderValue, MaxOrderValue, VehiclesRequired, SharePercent FROM DealerLevel ORDER BY LevelID";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                DTODealerLevel level = new DTODealerLevel();
                level.setLevelID(rs.getInt("LevelID"));
                level.setLevelName(rs.getString("LevelName"));
                level.setMinOrderValue(rs.getBigDecimal("MinOrderValue"));
                level.setMaxOrderValue(rs.getBigDecimal("MaxOrderValue"));
                level.setVehiclesRequired(rs.getInt("VehiclesRequired"));
                try { level.setSharePercent(rs.getBigDecimal("SharePercent")); } catch (Exception ignore) {}
                list.add(level);
            }

        } catch (SQLException e) {
            System.out.println(" Error fetching dealer levels: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Get dealer level by ID
     */
    public DTODealerLevel getDealerLevelById(int levelID) {
        String sql = "SELECT LevelID, LevelName, MinOrderValue, MaxOrderValue, VehiclesRequired, SharePercent FROM DealerLevel WHERE LevelID = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, levelID);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DTODealerLevel level = new DTODealerLevel();
                    level.setLevelID(rs.getInt("LevelID"));
                    level.setLevelName(rs.getString("LevelName"));
                    level.setMinOrderValue(rs.getBigDecimal("MinOrderValue"));
                    level.setMaxOrderValue(rs.getBigDecimal("MaxOrderValue"));
                    level.setVehiclesRequired(rs.getInt("VehiclesRequired"));
                    try { level.setSharePercent(rs.getBigDecimal("SharePercent")); } catch (Exception ignore) {}
                    return level;
                }
            }

        } catch (SQLException e) {
            System.out.println(" Error fetching dealer level ID " + levelID + ": " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Update dealer level
     */
    public boolean updateDealerLevel(DTODealerLevel level) {
        String sql = "UPDATE DealerLevel SET LevelName=?, VehiclesRequired=?, SharePercent=? WHERE LevelID=?";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, level.getLevelName());
            ps.setInt(2, level.getVehiclesRequired());
            ps.setBigDecimal(3, level.getSharePercent() != null ? level.getSharePercent() : java.math.BigDecimal.ZERO);
            ps.setInt(4, level.getLevelID());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ DealerLevel updated ID=" + level.getLevelID());
                return true;
            }
        } catch (SQLException e) {
            System.out.println("❌ Failed to update DealerLevel ID=" + level.getLevelID() + ": " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Create dealer level
     */
    public int createDealerLevel(DTODealerLevel level) {
        String sql = "INSERT INTO DealerLevel(LevelName, MinOrderValue, MaxOrderValue, VehiclesRequired, SharePercent) VALUES (?,?,?,?,?)";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, level.getLevelName());
            ps.setBigDecimal(2, java.math.BigDecimal.ZERO); // MinOrderValue deprecated -> store 0
            ps.setBigDecimal(3, java.math.BigDecimal.ZERO); // MaxOrderValue deprecated -> store 0
            ps.setInt(4, level.getVehiclesRequired());
            ps.setBigDecimal(5, level.getSharePercent() != null ? level.getSharePercent() : java.math.BigDecimal.ZERO);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        System.out.println("✅ DealerLevel created ID=" + id);
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Failed to create DealerLevel: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }
}
