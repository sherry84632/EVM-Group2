package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class DAOTestDrive {

    // ✅ Lấy danh sách TestDrive
    public List<DTOTestDrive> getAllTestDrives() {
        List<DTOTestDrive> list = new ArrayList<>();
        String sql = """
            SELECT td.TestDriveID, td.CustomerID, td.VehicleID, td.DealerID, td.StaffID, td.TestDate, td.Feedback,
                   c.CustomerID, c.FullName AS CustomerName, c.Phone AS CustomerPhone,
                   d.DealerID, d.DealerName,
                   ds.StaffID, ds.FullName AS StaffName,
                   v.VehicleID, vm.ModelName, vc.ColorName
            FROM TestDrive td
            JOIN Customer c ON td.CustomerID = c.CustomerID
            JOIN Dealer d ON td.DealerID = d.DealerID
            JOIN DealerStaff ds ON td.StaffID = ds.StaffID
            JOIN Vehicle v ON td.VehicleID = v.VehicleID
            LEFT JOIN VehicleVersion vv ON v.VersionID = vv.VersionID
            LEFT JOIN VehicleModel vm ON vv.ModelID = vm.ModelID
            LEFT JOIN VehicleColor vc ON v.ColorID = vc.ColorID
            ORDER BY td.TestDate DESC
        """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                DTOTestDrive testDrive = new DTOTestDrive();
                testDrive.setTestDriveID(rs.getInt("TestDriveID"));
                java.sql.Date testDate = rs.getDate("TestDate");
                testDrive.setTestDate(testDate != null ? new java.util.Date(testDate.getTime()) : null);
                testDrive.setFeedback(rs.getString("Feedback"));

                // Customer info
                DTOCustomer customer = new DTOCustomer();
                customer.setCustomerID(rs.getInt("CustomerID"));
                customer.setFullName(rs.getString("CustomerName"));
                customer.setPhone(rs.getString("CustomerPhone"));
                testDrive.setCustomer(customer);

                // Dealer info
                DTODealer dealer = new DTODealer();
                dealer.setDealerID(rs.getInt("DealerID"));
                dealer.setDealerName(rs.getString("DealerName"));
                testDrive.setDealer(dealer);

                // Staff info
                DTODealerStaff staff = new DTODealerStaff();
                staff.setStaffID(rs.getInt("StaffID"));
                staff.setFullName(rs.getString("StaffName"));
                testDrive.setStaff(staff);

                // Vehicle info
                DTOVehicle vehicle = new DTOVehicle();
                vehicle.setVehicleID(rs.getInt("VehicleID"));
                testDrive.setVehicle(vehicle);

                list.add(testDrive);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ✅ Lấy TestDrive theo ID
    public DTOTestDrive getTestDriveById(int testDriveID) {
        String sql = """
            SELECT td.TestDriveID, td.CustomerID, td.VehicleID, td.DealerID, td.StaffID, td.TestDate, td.Feedback,
                   c.CustomerID, c.FullName AS CustomerName, c.Phone AS CustomerPhone,
                   d.DealerID, d.DealerName,
                   ds.StaffID, ds.FullName AS StaffName,
                   v.VehicleID, vm.ModelName, vc.ColorName
            FROM TestDrive td
            JOIN Customer c ON td.CustomerID = c.CustomerID
            JOIN Dealer d ON td.DealerID = d.DealerID
            JOIN DealerStaff ds ON td.StaffID = ds.StaffID
            JOIN Vehicle v ON td.VehicleID = v.VehicleID
            LEFT JOIN VehicleVersion vv ON v.VersionID = vv.VersionID
            LEFT JOIN VehicleModel vm ON vv.ModelID = vm.ModelID
            LEFT JOIN VehicleColor vc ON v.ColorID = vc.ColorID
            WHERE td.TestDriveID = ?
        """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, testDriveID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DTOTestDrive testDrive = new DTOTestDrive();
                    testDrive.setTestDriveID(rs.getInt("TestDriveID"));
                    java.sql.Date testDate = rs.getDate("TestDate");
                    testDrive.setTestDate(testDate != null ? new java.util.Date(testDate.getTime()) : null);
                    testDrive.setFeedback(rs.getString("Feedback"));

                    // Customer info
                    DTOCustomer customer = new DTOCustomer();
                    customer.setCustomerID(rs.getInt("CustomerID"));
                    customer.setFullName(rs.getString("CustomerName"));
                    customer.setPhone(rs.getString("CustomerPhone"));
                    testDrive.setCustomer(customer);

                    // Dealer info
                    DTODealer dealer = new DTODealer();
                    dealer.setDealerID(rs.getInt("DealerID"));
                    dealer.setDealerName(rs.getString("DealerName"));
                    testDrive.setDealer(dealer);

                    // Staff info
                    DTODealerStaff staff = new DTODealerStaff();
                    staff.setStaffID(rs.getInt("StaffID"));
                    staff.setFullName(rs.getString("StaffName"));
                    testDrive.setStaff(staff);

                    // Vehicle info
                    DTOVehicle vehicle = new DTOVehicle();
                    vehicle.setVehicleID(rs.getInt("VehicleID"));
                    testDrive.setVehicle(vehicle);

                    return testDrive;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ✅ Tạo TestDrive mới
    public boolean createTestDrive(DTOTestDrive testDrive) {
        String sql = "INSERT INTO TestDrive (CustomerID, VehicleID, DealerID, StaffID, TestDate, Feedback) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, testDrive.getCustomer().getCustomerID());
            ps.setInt(2, testDrive.getVehicle().getVehicleID());
            ps.setInt(3, testDrive.getDealer().getDealerID());
            ps.setInt(4, testDrive.getStaff().getStaffID());
            ps.setDate(5, new java.sql.Date(testDrive.getTestDate().getTime()));
            ps.setString(6, testDrive.getFeedback());

            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ✅ Cập nhật TestDrive
    public boolean updateTestDrive(DTOTestDrive testDrive) {
        String sql = "UPDATE TestDrive SET CustomerID=?, VehicleID=?, DealerID=?, StaffID=?, TestDate=?, Feedback=? WHERE TestDriveID=?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, testDrive.getCustomer().getCustomerID());
            ps.setInt(2, testDrive.getVehicle().getVehicleID());
            ps.setInt(3, testDrive.getDealer().getDealerID());
            ps.setInt(4, testDrive.getStaff().getStaffID());
            ps.setDate(5, new java.sql.Date(testDrive.getTestDate().getTime()));
            ps.setString(6, testDrive.getFeedback());
            ps.setInt(7, testDrive.getTestDriveID());

            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ✅ Xóa TestDrive
    public boolean deleteTestDrive(int testDriveID) {
        String sql = "DELETE FROM TestDrive WHERE TestDriveID = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, testDriveID);

            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}

