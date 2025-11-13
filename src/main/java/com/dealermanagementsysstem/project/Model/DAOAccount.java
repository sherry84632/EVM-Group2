package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.springframework.stereotype.Repository;

@Repository
public class DAOAccount {


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

    // ===========================
    // 🔥 CRUD OPERATIONS FOR ACCOUNT MANAGEMENT
    // ===========================

    /**
     * Get all accounts (for account management page)
     */
    public java.util.List<DTOAccount> getAllAccounts() {
        java.util.List<DTOAccount> list = new java.util.ArrayList<>();
        String sql = "SELECT AccountID, Username, Password, Role, IsActive, Email, Phone, CreatedAt, UpdatedAt FROM Account ORDER BY CreatedAt DESC";

        try (Connection con = DBUtils.getConnection();
             java.sql.Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                DTOAccount account = new DTOAccount();
                account.setAccountId(rs.getInt("AccountID"));
                account.setUsername(rs.getString("Username"));
                account.setPassword(rs.getString("Password"));
                account.setRole(Role.valueOf(rs.getString("Role")));
                account.setActive(rs.getBoolean("IsActive"));
                account.setEmail(rs.getString("Email"));
                account.setPhone(rs.getString("Phone"));
                account.setCreatedAt(rs.getTimestamp("CreatedAt"));
                account.setUpdatedAt(rs.getTimestamp("UpdatedAt"));
                list.add(account);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Get account by ID with DealerStaff information
     */
    public DTOAccount getAccountById(int accountId) {
        String sql = """
            SELECT a.AccountID, a.Username, a.Password, a.Role, a.IsActive, a.Email, a.Phone, a.CreatedAt, a.UpdatedAt,
                   ds.StaffID, ds.FullName, ds.Position, ds.Phone as StaffPhone, ds.Email as StaffEmail,
                   d.DealerID, d.DealerName, d.Address, d.Phone as DealerPhone, d.Email as DealerEmail
            FROM Account a
            LEFT JOIN DealerStaff ds ON ds.AccountID = a.AccountID
            LEFT JOIN Dealer d ON d.DealerID = ds.DealerID
            WHERE a.AccountID = ?
        """;

        try (Connection con = DBUtils.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, accountId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DTOAccount account = new DTOAccount();
                    account.setAccountId(rs.getInt("AccountID"));
                    account.setUsername(rs.getString("Username"));
                    account.setPassword(rs.getString("Password"));
                    account.setRole(Role.valueOf(rs.getString("Role")));
                    account.setActive(rs.getBoolean("IsActive"));
                    account.setEmail(rs.getString("Email"));
                    account.setPhone(rs.getString("Phone"));
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
                            dealer.setPhone(rs.getString("DealerPhone"));
                            dealer.setEmail(rs.getString("DealerEmail"));
                            dealerStaff.setDealer(dealer);
                        }

                        account.setDealerStaff(dealerStaff);
                    }

                    return account;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Insert new account (AccountID auto-generated)
     * Returns the generated AccountID
     */
    public int insertAccount(DTOAccount account) {
        String sql = "INSERT INTO Account (Username, Password, Role, IsActive, Email, Phone, CreatedAt, UpdatedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = DBUtils.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, account.getUsername());
            ps.setString(2, account.getPassword()); // Should be hashed before calling this
            ps.setString(3, account.getRole().name());
            ps.setBoolean(4, account.isActive());
            ps.setString(5, account.getEmail());
            ps.setString(6, account.getPhone());

            java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());
            ps.setTimestamp(7, now);
            ps.setTimestamp(8, now);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int newAccountId = generatedKeys.getInt(1);
                        System.out.println("✅ Account created successfully: " + account.getEmail() + " (ID: " + newAccountId + ")");
                        return newAccountId;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Failed to insert account!");
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Update existing account
     */
    public boolean updateAccount(DTOAccount account) {
        String sql = "UPDATE Account SET Username=?, Role=?, IsActive=?, Email=?, Phone=?, UpdatedAt=? WHERE AccountID=?";

        try (Connection con = DBUtils.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, account.getUsername());
            ps.setString(2, account.getRole().name());
            ps.setBoolean(3, account.isActive());
            ps.setString(4, account.getEmail());
            ps.setString(5, account.getPhone());
            ps.setTimestamp(6, new java.sql.Timestamp(System.currentTimeMillis()));
            ps.setInt(7, account.getAccountId());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("✅ Account updated successfully: " + account.getEmail());
                return true;
            }
        } catch (Exception e) {
            System.out.println("❌ Failed to update account!");
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Delete account (cascade delete will be handled by database constraints)
     */
    public boolean deleteAccount(int accountId) {
        Connection con = null;
        try {
            con = DBUtils.getConnection();
            con.setAutoCommit(false);

            // 1. Delete DealerStaff records first (if any)
            String deleteDealerStaff = "DELETE FROM DealerStaff WHERE AccountID = ?";
            try (PreparedStatement ps = con.prepareStatement(deleteDealerStaff)) {
                ps.setInt(1, accountId);
                int deletedStaff = ps.executeUpdate();
                System.out.println("🗑️ Deleted " + deletedStaff + " dealer staff record(s) for Account ID: " + accountId);
            }

            // 2. Delete Account
            String deleteAccount = "DELETE FROM Account WHERE AccountID = ?";
            try (PreparedStatement ps = con.prepareStatement(deleteAccount)) {
                ps.setInt(1, accountId);
                int deleted = ps.executeUpdate();

                if (deleted > 0) {
                    con.commit();
                    System.out.println("🗑️ Account deleted successfully (ID: " + accountId + ")");
                    return true;
                } else {
                    con.rollback();
                    System.out.println("⚠️ Account not found (ID: " + accountId + ")");
                    return false;
                }
            }

        } catch (Exception e) {
            System.out.println("❌ Failed to delete account!");
            e.printStackTrace();
            if (con != null) {
                try {
                    con.rollback();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Search accounts by username or email
     */
    public java.util.List<DTOAccount> searchAccounts(String keyword) {
        java.util.List<DTOAccount> list = new java.util.ArrayList<>();
        String sql = "SELECT AccountID, Username, Password, Role, IsActive, Email, Phone, CreatedAt, UpdatedAt FROM Account WHERE Username LIKE ? OR Email LIKE ? ORDER BY CreatedAt DESC";

        try (Connection con = DBUtils.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, "%" + keyword + "%");
            ps.setString(2, "%" + keyword + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DTOAccount account = new DTOAccount();
                    account.setAccountId(rs.getInt("AccountID"));
                    account.setUsername(rs.getString("Username"));
                    account.setPassword(rs.getString("Password"));
                    account.setRole(Role.valueOf(rs.getString("Role")));
                    account.setActive(rs.getBoolean("IsActive"));
                    account.setEmail(rs.getString("Email"));
                    account.setPhone(rs.getString("Phone"));
                    account.setCreatedAt(rs.getTimestamp("CreatedAt"));
                    account.setUpdatedAt(rs.getTimestamp("UpdatedAt"));
                    list.add(account);
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Failed to search accounts!");
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Check if email already exists (for validation)
     */
    public boolean emailExists(String email, Integer excludeAccountId) {
        String sql = excludeAccountId != null
            ? "SELECT COUNT(*) as cnt FROM Account WHERE Email = ? AND AccountID != ?"
            : "SELECT COUNT(*) as cnt FROM Account WHERE Email = ?";

        try (Connection con = DBUtils.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email);
            if (excludeAccountId != null) {
                ps.setInt(2, excludeAccountId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cnt") > 0;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
