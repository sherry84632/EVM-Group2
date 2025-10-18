// DAOColor.java
package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository

public class DAOColor {
    public List<DTOColor> getAllColors() {
        List<DTOColor> list = new ArrayList<>();
        String sql = "SELECT ColorID, ColorName FROM Color";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                DTOColor c = new DTOColor();
                c.setColorID(rs.getInt("ColorID"));
                c.setColorName(rs.getString("ColorName"));
                list.add(c);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
