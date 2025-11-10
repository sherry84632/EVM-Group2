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
        String sql = "SELECT LevelID, LevelName, MinOrderValue, MaxOrderValue FROM DealerLevel ORDER BY LevelID";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                DTODealerLevel level = new DTODealerLevel();
                level.setLevelID(rs.getInt("LevelID"));
                level.setLevelName(rs.getString("LevelName"));
                level.setMinOrderValue(rs.getBigDecimal("MinOrderValue"));
                level.setMaxOrderValue(rs.getBigDecimal("MaxOrderValue"));
                list.add(level);
            }

        } catch (SQLException e) {
            System.out.println("❌ Error fetching dealer levels: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Get dealer level by ID
     */
    public DTODealerLevel getDealerLevelById(int levelID) {
        String sql = "SELECT LevelID, LevelName, MinOrderValue, MaxOrderValue FROM DealerLevel WHERE LevelID = ?";

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
                    return level;
                }
            }

        } catch (SQLException e) {
            System.out.println("❌ Error fetching dealer level ID " + levelID + ": " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}

