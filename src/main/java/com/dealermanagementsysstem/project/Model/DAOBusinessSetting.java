package com.dealermanagementsysstem.project.Model;

import org.springframework.stereotype.Repository;
import utils.DBUtils;
import java.sql.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Repository
public class DAOBusinessSetting {
    private static final AtomicBoolean tableChecked = new AtomicBoolean(false);

    private void ensureTable() {
        if (tableChecked.get()) return;
        String checkSql = "SELECT 1 FROM sys.tables WHERE name = 'BusinessSetting'";
        String createSql = "CREATE TABLE BusinessSetting (SettingKey VARCHAR(100) NOT NULL PRIMARY KEY, SettingValue DECIMAL(18,4) NULL, UpdatedAt DATETIME2 NOT NULL DEFAULT GETDATE())";
        try (Connection conn = DBUtils.getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(checkSql)) {
            if (!rs.next()) {
                st.execute(createSql);
                System.out.println("[DAOBusinessSetting] Auto-created BusinessSetting table.");
                st.executeUpdate("INSERT INTO BusinessSetting(SettingKey, SettingValue) VALUES('VAT_RATE', 10.00)");
            }
            tableChecked.set(true);
        } catch (SQLException e) {
            System.out.println("[DAOBusinessSetting] Failed table check/create: " + e.getMessage());
        }
    }

    public Double getDecimalSetting(String key) {
        ensureTable();
        String sql = "SELECT SettingValue FROM BusinessSetting WHERE SettingKey=?";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("SettingValue");
                }
            }
        } catch (SQLException e) {
            System.out.println("Failed to load business setting key=" + key + " : " + e.getMessage());
        }
        return null;
    }

    public boolean upsertDecimalSetting(String key, Double value) {
        ensureTable();
        String updateSql = "UPDATE BusinessSetting SET SettingValue=?, UpdatedAt=GETDATE() WHERE SettingKey=?";
        String insertSql = "INSERT INTO BusinessSetting(SettingKey, SettingValue) VALUES(?,?)";
        try (Connection conn = DBUtils.getConnection()) {
            try (PreparedStatement up = conn.prepareStatement(updateSql)) {
                up.setDouble(1, value);
                up.setString(2, key);
                int rows = up.executeUpdate();
                if (rows > 0) return true;
            }
            try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
                ins.setString(1, key);
                ins.setDouble(2, value);
                return ins.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.out.println("Failed to upsert business setting key=" + key + " : " + e.getMessage());
        }
        return false;
    }
}
