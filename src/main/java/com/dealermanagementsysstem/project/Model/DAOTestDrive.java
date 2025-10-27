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

    // ✅ Tạo TestDrive đơn giản với customerID, testDate và optional vehicleID
    public boolean insertTestDrive(int customerID, java.util.Date testDate, Integer vehicleID) {
        // Gọi overload method với dealerID và staffID = null
        return insertTestDrive(customerID, testDate, vehicleID, null, null);
    }

    // ✅ Tạo TestDrive với đầy đủ thông tin (bao gồm dealerID và staffID)
    public boolean insertTestDrive(int customerID, java.util.Date testDate, Integer vehicleID,
                                   Integer dealerID, Integer staffID) {
        String sql = "INSERT INTO TestDrive (CustomerID, TestDate, VehicleID, DealerID, StaffID) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, customerID);
            ps.setTimestamp(2, new Timestamp(testDate.getTime()));

            // ✅ VehicleID có thể NULL
            if (vehicleID != null && vehicleID > 0) {
                ps.setInt(3, vehicleID);
            } else {
                ps.setNull(3, java.sql.Types.INTEGER);
            }

            // ✅ DealerID có thể NULL
            if (dealerID != null && dealerID > 0) {
                ps.setInt(4, dealerID);
            } else {
                ps.setNull(4, java.sql.Types.INTEGER);
            }

            // ✅ StaffID có thể NULL
            if (staffID != null && staffID > 0) {
                ps.setInt(5, staffID);
            } else {
                ps.setNull(5, java.sql.Types.INTEGER);
            }

            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ TestDrive inserted successfully for Customer ID: " + customerID +
                                 (vehicleID != null ? " with Vehicle ID: " + vehicleID : " (no vehicle)") +
                                 (dealerID != null ? " Dealer ID: " + dealerID : "") +
                                 (staffID != null ? " Staff ID: " + staffID : ""));
                return true;
            }

        } catch (SQLException e) {
            System.out.println("❌ Failed to insert test drive!");
            e.printStackTrace();
        }
        return false;
    }

    // ✅ Lấy test drive của customer theo customerID
    public DTOTestDrive getTestDriveByCustomerId(int customerID) {
        String sql = """
            SELECT TOP 1 td.TestDriveID, td.CustomerID, td.VehicleID, td.TestDate, td.Feedback,
                   v.VehicleID, vm.ModelName, vc.ColorName, vm.BasePrice, vm.Brand, v.ManufactureYear
            FROM TestDrive td
            LEFT JOIN Vehicle v ON td.VehicleID = v.VehicleID
            LEFT JOIN VehicleVersion vv ON v.VersionID = vv.VersionID
            LEFT JOIN VehicleModel vm ON vv.ModelID = vm.ModelID
            LEFT JOIN VehicleColor vc ON v.ColorID = vc.ColorID
            WHERE td.CustomerID = ?
            ORDER BY td.TestDate DESC
        """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, customerID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DTOTestDrive testDrive = new DTOTestDrive();
                    testDrive.setTestDriveID(rs.getInt("TestDriveID"));
                    java.sql.Timestamp testDate = rs.getTimestamp("TestDate");
                    testDrive.setTestDate(testDate != null ? new java.util.Date(testDate.getTime()) : null);
                    testDrive.setFeedback(rs.getString("Feedback"));

                    // Customer info (chỉ cần ID)
                    DTOCustomer customer = new DTOCustomer();
                    customer.setCustomerID(customerID);
                    testDrive.setCustomer(customer);

                    // Vehicle info (nếu có)
                    if (rs.getObject("VehicleID") != null) {
                        DTOVehicle vehicle = new DTOVehicle();
                        vehicle.setVehicleID(rs.getInt("VehicleID"));
                        vehicle.setManufactureYear(rs.getInt("ManufactureYear"));

                        // Color
                        if (rs.getString("ColorName") != null) {
                            DTOVehicleColor color = new DTOVehicleColor();
                            color.setColorName(rs.getString("ColorName"));
                            vehicle.setColor(color);
                        }

                        // Version with Model
                        if (rs.getString("ModelName") != null) {
                            DTOVehicleVersion version = new DTOVehicleVersion();
                            DTOVehicleModel model = new DTOVehicleModel();
                            model.setModelName(rs.getString("ModelName"));
                            model.setBrand(rs.getString("Brand"));
                            model.setBasePrice(rs.getBigDecimal("BasePrice"));
                            version.setModel(model);
                            vehicle.setVersion(version);
                        }

                        testDrive.setVehicle(vehicle);
                    }

                    return testDrive;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
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

