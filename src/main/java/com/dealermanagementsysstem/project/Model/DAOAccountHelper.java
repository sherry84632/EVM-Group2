package com.dealermanagementsysstem.project.Model;

import org.springframework.stereotype.Repository;
import utils.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Repository
public class DAOAccountHelper {

    public Integer getDealerIdByEmail(String email) {
        String sql = "SELECT DealerID FROM Dealer WHERE email = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("DealerID");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
