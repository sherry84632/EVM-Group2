package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class DAOTestDrive {

    //  Tạo TestDrive với đầy đủ thông tin (bao gồm dealerID và staffID)
    public boolean insertTestDrive(int customerID, java.util.Date testDate, Integer vehicleID,
                                   Integer dealerID, Integer staffID) {
        String sql = "INSERT INTO TestDrive (CustomerID, TestDate, VehicleID, DealerID, StaffID) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, customerID);
            ps.setTimestamp(2, new Timestamp(testDate.getTime()));

            //  VehicleID có thể NULL
            if (vehicleID != null && vehicleID > 0) {
                ps.setInt(3, vehicleID);
            } else {
                ps.setNull(3, java.sql.Types.INTEGER);
            }

            //  DealerID có thể NULL
            if (dealerID != null && dealerID > 0) {
                ps.setInt(4, dealerID);
            } else {
                ps.setNull(4, java.sql.Types.INTEGER);
            }

            //  StaffID có thể NULL
            if (staffID != null && staffID > 0) {
                ps.setInt(5, staffID);
            } else {
                ps.setNull(5, java.sql.Types.INTEGER);
            }

            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println(" TestDrive inserted successfully for Customer ID: " + customerID +
                                 (vehicleID != null ? " with Vehicle ID: " + vehicleID : " (no vehicle)") +
                                 (dealerID != null ? " Dealer ID: " + dealerID : "") +
                                 (staffID != null ? " Staff ID: " + staffID : ""));
                return true;
            }

        } catch (SQLException e) {
            System.out.println(" Failed to insert test drive!");
            e.printStackTrace();
        }
        return false;
    }

    //  Lấy test drive của customer theo customerID
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

}

