package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.springframework.stereotype.Repository;

@Repository
public class DAOAccount {

    private static final String LOGIN_QUERY =
            "SELECT AccountID, Username, Password, Role, IsActive, Email, CreatedAt, UpdatedAt " +
                    "FROM Account WHERE Email = ? AND Password = ? AND IsActive = 1";

    public DTOAccount checkLogin(String email, String password) {
        DTOAccount account = null;

        try (Connection con = DBUtils.getConnection();
             PreparedStatement stm = con.prepareStatement(LOGIN_QUERY)) {

            stm.setString(1, email);
            stm.setString(2, password);

            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    account = new DTOAccount();
                    account.setAccountId(rs.getInt("AccountID"));
                    account.setUsername(rs.getString("Username"));
                    account.setPassword(rs.getString("Password"));
                    account.setRole(Role.valueOf(rs.getString("Role")));
                    account.setActive(rs.getBoolean("IsActive"));
                    account.setEmail(rs.getString("Email"));
                    account.setCreatedAt(rs.getTimestamp("CreatedAt"));
                    account.setUpdatedAt(rs.getTimestamp("UpdatedAt"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return account;
    }

    public DTOAccount findAccountByEmail(String email) {
        DTOAccount account = null;

        String query = """
            SELECT a.AccountID, a.Username, a.Password, a.Role, a.IsActive, a.Email, a.CreatedAt, a.UpdatedAt,
                   ds.StaffID, ds.FullName, ds.Position, ds.Phone as StaffPhone, ds.Email as StaffEmail,
                   d.DealerID, d.DealerName, d.Address, d.Phone, d.Email as DealerEmail
            FROM Account a
            LEFT JOIN DealerStaff ds ON ds.AccountID = a.AccountID
            LEFT JOIN Dealer d ON d.DealerID = ds.DealerID
            WHERE a.Email = ?
        """;

        try (Connection con = DBUtils.getConnection();
            PreparedStatement stm = con.prepareStatement(query)) {

            stm.setString(1, email);

            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    account = new DTOAccount();
                    account.setAccountId(rs.getInt("AccountID"));
                    account.setUsername(rs.getString("Username"));
                    account.setPassword(rs.getString("Password"));
                    account.setRole(Role.valueOf(rs.getString("Role")));
                    account.setActive(rs.getBoolean("IsActive"));
                    account.setEmail(rs.getString("Email"));
                    account.setCreatedAt(rs.getTimestamp("CreatedAt"));
                    account.setUpdatedAt(rs.getTimestamp("UpdatedAt"));

                    // Load DealerStaff relationship if exists
                    if (rs.getString("FullName") != null) {
                        DTODealerStaff dealerStaff = new DTODealerStaff();
                        dealerStaff.setStaffID(rs.getInt("StaffID"));
                        dealerStaff.setFullName(rs.getString("FullName"));
                        dealerStaff.setPosition(rs.getString("Position"));
                        dealerStaff.setPhone(rs.getString("StaffPhone"));
                        dealerStaff.setEmail(rs.getString("StaffEmail"));
                        
                        // Load Dealer relationship through DealerStaff if exists
                        if (rs.getString("DealerName") != null) {
                            DTODealer dealer = new DTODealer();
                            dealer.setDealerID(rs.getInt("DealerID"));
                            dealer.setDealerName(rs.getString("DealerName"));
                            dealer.setAddress(rs.getString("Address"));
                            dealer.setPhone(rs.getString("Phone"));
                            dealer.setEmail(rs.getString("DealerEmail"));
                            dealerStaff.setDealer(dealer);
                        }
                        
                        account.setDealerStaff(dealerStaff);
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return account;
    }

    public boolean updatePassword(int accountId, String hashedPassword) {
        String query = "UPDATE Account SET Password = ? WHERE AccountID = ?";
        
        try (Connection con = DBUtils.getConnection();
             PreparedStatement stm = con.prepareStatement(query)) {
            
            stm.setString(1, hashedPassword);
            stm.setInt(2, accountId);
            
            int rowsAffected = stm.executeUpdate();
            return rowsAffected > 0;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Integer getDealerIdByEmail(String email) {
        String sql = """
            SELECT d.DealerID 
            FROM Account a
            JOIN DealerStaff ds ON ds.AccountID = a.AccountID
            JOIN Dealer d ON d.DealerID = ds.DealerID
            WHERE a.Email = ?
            """;
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("DealerID");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
