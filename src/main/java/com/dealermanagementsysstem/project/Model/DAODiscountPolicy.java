package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DAODiscountPolicy {

    // Helper method to safely get DiscountPercent (may not exist in older databases)
    private BigDecimal getDiscountPercentSafely(ResultSet rs) {
        try {
            return rs.getBigDecimal("DiscountPercent");
        } catch (SQLException e) {
            // Column doesn't exist - migration not run yet, return null
            return null;
        }
    }

    //  Lấy LevelID theo DealerID
    private Integer getLevelIdByDealerId(int dealerId) {
        String sql = "SELECT LevelID FROM Dealer WHERE DealerID = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, dealerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("LevelID");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //  Create Discount Policy and auto-update Dealer.PolicyID
    public boolean createDiscountPolicy(DTODiscountPolicy dto) {
        // Try with DiscountPercent first (after migration)
        String sqlWithDiscount = "INSERT INTO DiscountPolicy " +
                "(DealerID, PolicyName, Description, DiscountPercent, HangPercent, DailyPercent, StartDate, EndDate, Status, CreatedAt, LevelID) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, GETDATE(), ?)";

        // Fallback without DiscountPercent (before migration)
        String sqlWithoutDiscount = "INSERT INTO DiscountPolicy " +
                "(DealerID, PolicyName, Description, HangPercent, DailyPercent, StartDate, EndDate, Status, CreatedAt, LevelID) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, GETDATE(), ?)";

        // SQL to update Dealer.PolicyID
        String sqlUpdateDealer = "UPDATE Dealer SET PolicyID = ? WHERE DealerID = ?";

        try (Connection conn = DBUtils.getConnection()) {

            Integer levelID = getLevelIdByDealerId(dto.getDealer().getDealerID());
            if (levelID == null) {
                System.out.println("⚠ Could not find LevelID for DealerID: " + dto.getDealer().getDealerID());
                return false;
            }

            int newPolicyId = -1;

            // Try with DiscountPercent first
            try (PreparedStatement ps = conn.prepareStatement(sqlWithDiscount, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, dto.getDealer().getDealerID());
                ps.setString(2, dto.getPolicyName());
                ps.setString(3, dto.getDescription());
                ps.setBigDecimal(4, dto.getDiscountPercent());
                ps.setBigDecimal(5, dto.getHangPercent());
                ps.setBigDecimal(6, dto.getDailyPercent());
                ps.setDate(7, Date.valueOf(dto.getStartDate()));
                ps.setDate(8, dto.getEndDate() != null ? Date.valueOf(dto.getEndDate()) : null);
                ps.setString(9, dto.getStatus().toString());
                ps.setInt(10, levelID);

                int rowsAffected = ps.executeUpdate();

                if (rowsAffected > 0) {
                    // Get generated PolicyID
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            newPolicyId = rs.getInt(1);
                            System.out.println(" Policy created with ID: " + newPolicyId);
                        }
                    }
                }

            } catch (SQLException e) {
                // If DiscountPercent column doesn't exist, try without it
                if (e.getMessage() != null && e.getMessage().contains("DiscountPercent")) {
                    System.out.println(" DiscountPercent column not found, using fallback SQL");

                    try (PreparedStatement ps = conn.prepareStatement(sqlWithoutDiscount, Statement.RETURN_GENERATED_KEYS)) {
                        ps.setInt(1, dto.getDealer().getDealerID());
                        ps.setString(2, dto.getPolicyName());
                        ps.setString(3, dto.getDescription());
                        ps.setBigDecimal(4, dto.getHangPercent());
                        ps.setBigDecimal(5, dto.getDailyPercent());
                        ps.setDate(6, Date.valueOf(dto.getStartDate()));
                        ps.setDate(7, dto.getEndDate() != null ? Date.valueOf(dto.getEndDate()) : null);
                        ps.setString(8, dto.getStatus().toString());
                        ps.setInt(9, levelID);

                        int rowsAffected = ps.executeUpdate();

                        if (rowsAffected > 0) {
                            // Get generated PolicyID
                            try (ResultSet rs = ps.getGeneratedKeys()) {
                                if (rs.next()) {
                                    newPolicyId = rs.getInt(1);
                                    System.out.println(" Policy created with ID: " + newPolicyId);
                                }
                            }
                        }
                    }
                } else {
                    throw e; // Re-throw if different error
                }
            }

            //  Auto-update Dealer.PolicyID if policy was created successfully
            if (newPolicyId > 0) {
                try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdateDealer)) {
                    psUpdate.setInt(1, newPolicyId);
                    psUpdate.setInt(2, dto.getDealer().getDealerID());
                    int updated = psUpdate.executeUpdate();

                    if (updated > 0) {
                        System.out.println(" Dealer PolicyID updated to: " + newPolicyId + " for DealerID: " + dto.getDealer().getDealerID());
                        return true;
                    } else {
                        System.out.println(" Policy created but Dealer.PolicyID update failed");
                        return true; // Policy still created successfully
                    }
                }
            }

        } catch (Exception e) {
            System.out.println(" Error creating Discount Policy: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // Lấy tất cả policy (không cần DealerID)
    public List<DTODiscountPolicy> getAllPolicies() {
        List<DTODiscountPolicy> list = new ArrayList<>();
        String sql = "SELECT * FROM DiscountPolicy ORDER BY CreatedAt DESC";

        DAODealer daoDealer = new DAODealer();

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                DTODiscountPolicy dto = new DTODiscountPolicy();
                dto.setPolicyID(rs.getInt("PolicyID"));
                dto.setDealer(daoDealer.getDealerById(rs.getInt("DealerID")));
                dto.setPolicyName(rs.getString("PolicyName"));
                dto.setDescription(rs.getString("Description"));
                dto.setDiscountPercent(getDiscountPercentSafely(rs)); //  Use helper
                dto.setHangPercent(rs.getBigDecimal("HangPercent"));
                dto.setDailyPercent(rs.getBigDecimal("DailyPercent"));
                dto.setStartDate(rs.getDate("StartDate").toLocalDate());
                dto.setEndDate(rs.getDate("EndDate") != null ? rs.getDate("EndDate").toLocalDate() : null);
                dto.setStatus(DiscountPolicyStatus.valueOf(rs.getString("Status").toUpperCase()));
                dto.setCreationDate(rs.getDate("CreatedAt"));
                dto.setLevelID(rs.getInt("LevelID"));
                list.add(dto);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    //  Search theo tên Policy
    public List<DTODiscountPolicy> searchPolicyByName(String keyword) {
        List<DTODiscountPolicy> list = new ArrayList<>();
        String sql = "SELECT * FROM DiscountPolicy WHERE PolicyName LIKE ? ORDER BY CreatedAt DESC";

        DAODealer daoDealer = new DAODealer();

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + keyword + "%");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                DTODiscountPolicy dto = new DTODiscountPolicy();
                dto.setPolicyID(rs.getInt("PolicyID"));
                dto.setDealer(daoDealer.getDealerById(rs.getInt("DealerID")));
                dto.setPolicyName(rs.getString("PolicyName"));
                dto.setDescription(rs.getString("Description"));
                dto.setDiscountPercent(getDiscountPercentSafely(rs)); //  Use helper
                dto.setHangPercent(rs.getBigDecimal("HangPercent"));
                dto.setDailyPercent(rs.getBigDecimal("DailyPercent"));
                dto.setStartDate(rs.getDate("StartDate").toLocalDate());
                dto.setEndDate(rs.getDate("EndDate") != null ? rs.getDate("EndDate").toLocalDate() : null);
                dto.setStatus(DiscountPolicyStatus.valueOf(rs.getString("Status").toUpperCase()));
                dto.setCreationDate(rs.getDate("CreatedAt"));
                dto.setLevelID(rs.getInt("LevelID"));
                list.add(dto);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // Get Discount Policy by ID
    public DTODiscountPolicy getPolicyById(int policyId) {
        String sql = "SELECT * FROM DiscountPolicy WHERE PolicyID = ?";
        DAODealer daoDealer = new DAODealer();

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, policyId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                DTODiscountPolicy dto = new DTODiscountPolicy();
                dto.setPolicyID(rs.getInt("PolicyID"));
                dto.setDealer(daoDealer.getDealerById(rs.getInt("DealerID")));
                dto.setPolicyName(rs.getString("PolicyName"));
                dto.setDescription(rs.getString("Description"));
                dto.setDiscountPercent(getDiscountPercentSafely(rs)); //  Use helper
                dto.setHangPercent(rs.getBigDecimal("HangPercent"));
                dto.setDailyPercent(rs.getBigDecimal("DailyPercent"));
                dto.setStartDate(rs.getDate("StartDate").toLocalDate());
                dto.setEndDate(rs.getDate("EndDate") != null ? rs.getDate("EndDate").toLocalDate() : null);
                dto.setStatus(DiscountPolicyStatus.valueOf(rs.getString("Status").toUpperCase()));
                dto.setCreationDate(rs.getDate("CreatedAt"));
                dto.setLevelID(rs.getInt("LevelID"));
                return dto;
            }

        } catch (Exception e) {
            System.out.println(" Error getting policy by ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    //  Update Discount Policy and auto-update Dealer.PolicyID
    public boolean updateDiscountPolicy(DTODiscountPolicy dto) {
        // Try with DiscountPercent first (after migration)
        String sqlWithDiscount = "UPDATE DiscountPolicy SET " +
                "PolicyName = ?, Description = ?, DiscountPercent = ?, HangPercent = ?, DailyPercent = ?, " +
                "StartDate = ?, EndDate = ?, Status = ? " +
                "WHERE PolicyID = ?";

        // Fallback without DiscountPercent (before migration)
        String sqlWithoutDiscount = "UPDATE DiscountPolicy SET " +
                "PolicyName = ?, Description = ?, HangPercent = ?, DailyPercent = ?, " +
                "StartDate = ?, EndDate = ?, Status = ? " +
                "WHERE PolicyID = ?";

        // SQL to update Dealer.PolicyID
        String sqlUpdateDealer = "UPDATE Dealer SET PolicyID = ? WHERE DealerID = ?";

        try (Connection conn = DBUtils.getConnection()) {

            boolean updated = false;

            // Try with DiscountPercent first
            try (PreparedStatement ps = conn.prepareStatement(sqlWithDiscount)) {
                ps.setString(1, dto.getPolicyName());
                ps.setString(2, dto.getDescription());
                ps.setBigDecimal(3, dto.getDiscountPercent());
                ps.setBigDecimal(4, dto.getHangPercent());
                ps.setBigDecimal(5, dto.getDailyPercent());
                ps.setDate(6, Date.valueOf(dto.getStartDate()));
                ps.setDate(7, dto.getEndDate() != null ? Date.valueOf(dto.getEndDate()) : null);
                ps.setString(8, dto.getStatus().toString());
                ps.setInt(9, dto.getPolicyID());

                int rows = ps.executeUpdate();
                if (rows > 0) {
                    System.out.println(" Updated Discount Policy ID: " + dto.getPolicyID() + " (with DiscountPercent)");
                    updated = true;
                }

            } catch (SQLException e) {
                // If DiscountPercent column doesn't exist, try without it
                if (e.getMessage() != null && e.getMessage().contains("DiscountPercent")) {
                    System.out.println(" DiscountPercent column not found, using fallback SQL");

                    try (PreparedStatement ps = conn.prepareStatement(sqlWithoutDiscount)) {
                        ps.setString(1, dto.getPolicyName());
                        ps.setString(2, dto.getDescription());
                        ps.setBigDecimal(3, dto.getHangPercent());
                        ps.setBigDecimal(4, dto.getDailyPercent());
                        ps.setDate(5, Date.valueOf(dto.getStartDate()));
                        ps.setDate(6, dto.getEndDate() != null ? Date.valueOf(dto.getEndDate()) : null);
                        ps.setString(7, dto.getStatus().toString());
                        ps.setInt(8, dto.getPolicyID());

                        int rows = ps.executeUpdate();
                        if (rows > 0) {
                            System.out.println(" Updated Discount Policy ID: " + dto.getPolicyID() + " (without DiscountPercent)");
                            updated = true;
                        }
                    }
                } else {
                    throw e; // Re-throw if different error
                }
            }

            //  Auto-update Dealer.PolicyID if policy was updated successfully
            if (updated && dto.getDealer() != null) {
                try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdateDealer)) {
                    psUpdate.setInt(1, dto.getPolicyID());
                    psUpdate.setInt(2, dto.getDealer().getDealerID());
                    int updatedDealer = psUpdate.executeUpdate();

                    if (updatedDealer > 0) {
                        System.out.println(" Dealer PolicyID updated to: " + dto.getPolicyID() + " for DealerID: " + dto.getDealer().getDealerID());
                    }
                }
            }

            return updated;

        } catch (Exception e) {
            System.out.println(" Error updating Discount Policy: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    //  Delete Discount Policy and set Dealer.PolicyID to NULL
    public boolean deleteDiscountPolicy(int policyId) {
        String sqlGetDealer = "SELECT DealerID FROM DiscountPolicy WHERE PolicyID = ?";
        String sqlDelete = "DELETE FROM DiscountPolicy WHERE PolicyID = ?";
        String sqlUpdateDealer = "UPDATE Dealer SET PolicyID = NULL WHERE PolicyID = ?";

        try (Connection conn = DBUtils.getConnection()) {

            // First, get the DealerID associated with this policy
            Integer dealerId = null;
            try (PreparedStatement ps = conn.prepareStatement(sqlGetDealer)) {
                ps.setInt(1, policyId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        dealerId = rs.getInt("DealerID");
                    }
                }
            }

            // Update Dealer.PolicyID to NULL before deleting policy
            if (dealerId != null) {
                try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdateDealer)) {
                    psUpdate.setInt(1, policyId);
                    psUpdate.executeUpdate();
                    System.out.println(" Set Dealer.PolicyID to NULL for DealerID: " + dealerId);
                }
            }

            // Now delete the policy
            try (PreparedStatement ps = conn.prepareStatement(sqlDelete)) {
                ps.setInt(1, policyId);
                int rows = ps.executeUpdate();

                if (rows > 0) {
                    System.out.println(" Deleted Discount Policy ID: " + policyId);
                    return true;
                } else {
                    System.out.println(" No policy found with ID: " + policyId);
                }
            }

        } catch (SQLException e) {
            // Check if it's a foreign key constraint violation
            if (e.getMessage() != null && e.getMessage().contains("REFERENCE constraint")) {
                System.out.println(" Cannot delete policy ID " + policyId + ": Referenced by other records");
                throw new RuntimeException("Cannot delete policy: Still referenced by purchase orders or other data", e);
            }
            System.out.println(" Error deleting Discount Policy: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    //  Get Policies by Dealer ID
    public List<DTODiscountPolicy> getPoliciesByDealerId(int dealerId) {
        List<DTODiscountPolicy> list = new ArrayList<>();
        String sql = "SELECT * FROM DiscountPolicy WHERE DealerID = ? ORDER BY CreatedAt DESC";
        DAODealer daoDealer = new DAODealer();

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, dealerId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                DTODiscountPolicy dto = new DTODiscountPolicy();
                dto.setPolicyID(rs.getInt("PolicyID"));
                dto.setDealer(daoDealer.getDealerById(rs.getInt("DealerID")));
                dto.setPolicyName(rs.getString("PolicyName"));
                dto.setDescription(rs.getString("Description"));
                dto.setDiscountPercent(getDiscountPercentSafely(rs)); //  Use helper
                dto.setHangPercent(rs.getBigDecimal("HangPercent"));
                dto.setDailyPercent(rs.getBigDecimal("DailyPercent"));
                dto.setStartDate(rs.getDate("StartDate").toLocalDate());
                dto.setEndDate(rs.getDate("EndDate") != null ? rs.getDate("EndDate").toLocalDate() : null);
                dto.setStatus(DiscountPolicyStatus.valueOf(rs.getString("Status").toUpperCase()));
                dto.setCreationDate(rs.getDate("CreatedAt"));
                dto.setLevelID(rs.getInt("LevelID"));
                list.add(dto);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    //  Search Policies by Name and Dealer ID
    public List<DTODiscountPolicy> searchPolicyByNameAndDealer(String keyword, int dealerId) {
        List<DTODiscountPolicy> list = new ArrayList<>();
        String sql = "SELECT * FROM DiscountPolicy WHERE PolicyName LIKE ? AND DealerID = ? ORDER BY CreatedAt DESC";
        DAODealer daoDealer = new DAODealer();

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + keyword + "%");
            ps.setInt(2, dealerId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                DTODiscountPolicy dto = new DTODiscountPolicy();
                dto.setPolicyID(rs.getInt("PolicyID"));
                dto.setDealer(daoDealer.getDealerById(rs.getInt("DealerID")));
                dto.setPolicyName(rs.getString("PolicyName"));
                dto.setDescription(rs.getString("Description"));
                dto.setDiscountPercent(getDiscountPercentSafely(rs)); //  Use helper
                dto.setHangPercent(rs.getBigDecimal("HangPercent"));
                dto.setDailyPercent(rs.getBigDecimal("DailyPercent"));
                dto.setStartDate(rs.getDate("StartDate").toLocalDate());
                dto.setEndDate(rs.getDate("EndDate") != null ? rs.getDate("EndDate").toLocalDate() : null);
                dto.setStatus(DiscountPolicyStatus.valueOf(rs.getString("Status").toUpperCase()));
                dto.setCreationDate(rs.getDate("CreatedAt"));
                dto.setLevelID(rs.getInt("LevelID"));
                list.add(dto);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
