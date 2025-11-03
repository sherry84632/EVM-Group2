package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class DAODealerStaff {

    /**
     * Insert new DealerStaff record
     * Returns the generated StaffID
     */
    public int insertDealerStaff(DTODealerStaff staff) {
        String sql = "INSERT INTO DealerStaff (FullName, Position, Phone, Email, AccountID, DealerID) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection con = DBUtils.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, staff.getFullName());
            // Auto-fill "Sales" if position is null or empty
            String position = staff.getPosition();
            if (position == null || position.trim().isEmpty()) {
                position = "Sales";
            }
            ps.setString(2, position);
            ps.setString(3, staff.getPhone());
            ps.setString(4, staff.getEmail());

            if (staff.getAccount() != null) {
                ps.setInt(5, staff.getAccount().getAccountId());
            } else {
                ps.setNull(5, Types.INTEGER);
            }

            if (staff.getDealer() != null) {
                ps.setInt(6, staff.getDealer().getDealerID());
            } else {
                ps.setNull(6, Types.INTEGER);
            }

            int rows = ps.executeUpdate();

            if (rows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int newStaffId = generatedKeys.getInt(1);
                        System.out.println("✅ DealerStaff created successfully: " + staff.getFullName() + " (ID: " + newStaffId + ")");
                        return newStaffId;
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Failed to insert DealerStaff!");
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Get DealerStaff by AccountID
     */
    public DTODealerStaff getDealerStaffByAccountId(int accountId) {
        String sql = """
            SELECT ds.StaffID, ds.FullName, ds.Position, ds.Phone, ds.Email, ds.AccountID, ds.DealerID,
                   d.DealerName, d.Address, d.Email as DealerEmail, d.Phone as DealerPhone
            FROM DealerStaff ds
            LEFT JOIN Dealer d ON ds.DealerID = d.DealerID
            WHERE ds.AccountID = ?
        """;

        try (Connection con = DBUtils.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, accountId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DTODealerStaff staff = new DTODealerStaff();
                    staff.setStaffID(rs.getInt("StaffID"));
                    staff.setFullName(rs.getString("FullName"));
                    staff.setPosition(rs.getString("Position"));
                    staff.setPhone(rs.getString("Phone"));
                    staff.setEmail(rs.getString("Email"));

                    // Set dealer if exists
                    if (rs.getString("DealerName") != null) {
                        DTODealer dealer = new DTODealer();
                        dealer.setDealerID(rs.getInt("DealerID"));
                        dealer.setDealerName(rs.getString("DealerName"));
                        dealer.setAddress(rs.getString("Address"));
                        dealer.setEmail(rs.getString("DealerEmail"));
                        dealer.setPhone(rs.getString("DealerPhone"));
                        staff.setDealer(dealer);
                    }

                    return staff;
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Failed to get DealerStaff by AccountID!");
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Update DealerStaff record
     */
    public boolean updateDealerStaff(DTODealerStaff staff) {
        String sql = "UPDATE DealerStaff SET FullName=?, Position=?, Phone=?, Email=?, DealerID=? WHERE StaffID=?";

        try (Connection con = DBUtils.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, staff.getFullName());
            // Auto-fill "Sales" if position is null or empty
            String position = staff.getPosition();
            if (position == null || position.trim().isEmpty()) {
                position = "Sales";
            }
            ps.setString(2, position);
            ps.setString(3, staff.getPhone());
            ps.setString(4, staff.getEmail());

            if (staff.getDealer() != null) {
                ps.setInt(5, staff.getDealer().getDealerID());
            } else {
                ps.setNull(5, Types.INTEGER);
            }

            ps.setInt(6, staff.getStaffID());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("✅ DealerStaff updated successfully: " + staff.getFullName());
                return true;
            }
        } catch (SQLException e) {
            System.out.println("❌ Failed to update DealerStaff!");
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Delete DealerStaff by StaffID
     */
    public boolean deleteDealerStaff(int staffId) {
        String sql = "DELETE FROM DealerStaff WHERE StaffID = ?";

        try (Connection con = DBUtils.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, staffId);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("🗑️ DealerStaff deleted successfully (ID: " + staffId + ")");
                return true;
            } else {
                System.out.println("⚠️ DealerStaff not found (ID: " + staffId + ")");
                return false;
            }
        } catch (SQLException e) {
            System.out.println("❌ Failed to delete DealerStaff!");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete DealerStaff by AccountID
     */
    public boolean deleteDealerStaffByAccountId(int accountId) {
        String sql = "DELETE FROM DealerStaff WHERE AccountID = ?";

        try (Connection con = DBUtils.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, accountId);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("🗑️ DealerStaff deleted successfully for Account ID: " + accountId);
                return true;
            }
        } catch (SQLException e) {
            System.out.println("❌ Failed to delete DealerStaff by AccountID!");
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Get all DealerStaff
     */
    public List<DTODealerStaff> getAllDealerStaff() {
        List<DTODealerStaff> list = new ArrayList<>();
        String sql = """
            SELECT ds.StaffID, ds.FullName, ds.Position, ds.Phone, ds.Email, ds.AccountID, ds.DealerID,
                   d.DealerName, d.Address, d.Email as DealerEmail, d.Phone as DealerPhone
            FROM DealerStaff ds
            LEFT JOIN Dealer d ON ds.DealerID = d.DealerID
            ORDER BY ds.StaffID DESC
        """;

        try (Connection con = DBUtils.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                DTODealerStaff staff = new DTODealerStaff();
                staff.setStaffID(rs.getInt("StaffID"));
                staff.setFullName(rs.getString("FullName"));
                staff.setPosition(rs.getString("Position"));
                staff.setPhone(rs.getString("Phone"));
                staff.setEmail(rs.getString("Email"));

                // Set dealer if exists
                if (rs.getString("DealerName") != null) {
                    DTODealer dealer = new DTODealer();
                    dealer.setDealerID(rs.getInt("DealerID"));
                    dealer.setDealerName(rs.getString("DealerName"));
                    dealer.setAddress(rs.getString("Address"));
                    dealer.setEmail(rs.getString("DealerEmail"));
                    dealer.setPhone(rs.getString("DealerPhone"));
                    staff.setDealer(dealer);
                }

                list.add(staff);
            }
        } catch (SQLException e) {
            System.out.println("❌ Failed to get all DealerStaff!");
            e.printStackTrace();
        }
        return list;
    }
}

