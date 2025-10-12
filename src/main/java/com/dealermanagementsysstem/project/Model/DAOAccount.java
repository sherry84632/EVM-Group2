package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DAOAccount {

    private static final String LOGIN_QUERY =
            "SELECT AccountID, Username, Password, Role, Status, EvmStaffID, DealerID, DealerStaffID, Email " +
                    "FROM Account WHERE Email = ? AND Password = ? AND Status = 1";

    public DTOAccount checkLogin(String email, String password) {
        DTOAccount account = null;

        try (Connection con = DBUtils.getConnection();
             PreparedStatement stm = con.prepareStatement(LOGIN_QUERY)) {

            stm.setString(1, email);
            stm.setString(2, password);

            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    account = new DTOAccount(
                            rs.getInt("AccountID"),
                            rs.getString("Username"),
                            rs.getString("Password"),
                            rs.getString("Role"),
                            rs.getBoolean("Status"),
                            rs.getObject("EvmStaffID") != null ? rs.getInt("EvmStaffID") : null,
                            rs.getObject("DealerID") != null ? rs.getInt("DealerID") : null,
                            rs.getObject("DealerStaffID") != null ? rs.getInt("DealerStaffID") : null,
                            rs.getString("Email")
                    );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return account;
    }
}
