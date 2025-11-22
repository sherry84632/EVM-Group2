package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class DAOTestDrive {


    //  Tạo TestDrive mới
    public boolean createTestDrive(DTOTestDrive testDrive) {
        String sql = "INSERT INTO TestDrive (CustomerID, VehicleID, DealerID, StaffID, TestDate, Feedback, Status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, testDrive.getCustomer().getCustomerID());
            if (testDrive.getVehicle()!=null && testDrive.getVehicle().getVehicleID()>0) ps.setInt(2, testDrive.getVehicle().getVehicleID()); else ps.setNull(2, java.sql.Types.INTEGER);
            if (testDrive.getDealer()!=null && testDrive.getDealer().getDealerID()>0) ps.setInt(3, testDrive.getDealer().getDealerID()); else ps.setNull(3, java.sql.Types.INTEGER);
            if (testDrive.getStaff()!=null && testDrive.getStaff().getStaffID()>0) ps.setInt(4, testDrive.getStaff().getStaffID()); else ps.setNull(4, java.sql.Types.INTEGER);
            ps.setTimestamp(5, new java.sql.Timestamp(testDrive.getTestDate().getTime()));
            ps.setString(6, testDrive.getFeedback());
            ps.setString(7, testDrive.getStatus()!=null? testDrive.getStatus():"NOT_YET");
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    //  Tạo TestDrive đơn giản với customerID, testDate và optional vehicleID
    public boolean insertTestDrive(int customerID, java.util.Date testDate, Integer vehicleID) {
        // Gọi overload method với dealerID và staffID = null
        return insertTestDrive(customerID, testDate, vehicleID, null, null);
    }

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



    //  Xóa TestDrive
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

    public List<DTOTestDrive> getTestDrivesByDealerFiltered(int dealerId, String statusFilter, String sortDir) {
        List<DTOTestDrive> list = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT td.TestDriveID, td.CustomerID, td.VehicleID, td.DealerID, td.StaffID, td.TestDate, td.Feedback, td.Status, c.FullName AS CustomerName, ds.FullName AS StaffName FROM TestDrive td ");
        sb.append("JOIN Customer c ON td.CustomerID = c.CustomerID ");
        sb.append("LEFT JOIN DealerStaff ds ON td.StaffID = ds.StaffID ");
        sb.append("WHERE td.DealerID = ? ");
        if(statusFilter!=null && !statusFilter.isBlank()) sb.append("AND td.Status = ? ");
        sb.append("ORDER BY td.TestDate ").append("asc".equalsIgnoreCase(sortDir)?"ASC":"DESC");
        try(Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sb.toString())){
            ps.setInt(1,dealerId);
            if(statusFilter!=null && !statusFilter.isBlank()) ps.setString(2,statusFilter);
            try(ResultSet rs = ps.executeQuery()){
                while(rs.next()){
                    DTOTestDrive td = new DTOTestDrive();
                    td.setTestDriveID(rs.getInt("TestDriveID"));
                    java.sql.Timestamp ts = rs.getTimestamp("TestDate");
                    td.setTestDate(ts!=null? new java.util.Date(ts.getTime()): null);
                    td.setFeedback(rs.getString("Feedback"));
                    td.setStatus(resolveDynamicStatus(rs.getString("Status"), td.getTestDate()));
                    DTOCustomer cust = new DTOCustomer(); cust.setCustomerID(rs.getInt("CustomerID")); cust.setFullName(rs.getString("CustomerName")); td.setCustomer(cust);
                    if(rs.getObject("StaffID")!=null){ DTODealerStaff st = new DTODealerStaff(); st.setStaffID(rs.getInt("StaffID")); st.setFullName(rs.getString("StaffName")); td.setStaff(st);}
                    DTODealer dealer = new DTODealer(); dealer.setDealerID(dealerId); td.setDealer(dealer);
                    list.add(td);
                }
            }
        } catch(SQLException e){ e.printStackTrace(); }
        return list;
    }

    private String resolveDynamicStatus(String stored, java.util.Date testDate){
        if(stored==null || stored.isBlank()) stored="NOT_YET";
        if("NOT_YET".equals(stored) && testDate!=null){
            java.time.LocalDate d = testDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            if(d.equals(java.time.LocalDate.now())) return "TODAY";
        }
        return stored;
    }

    public boolean updateStatus(int testDriveID, String newStatus){
        String sql = "UPDATE TestDrive SET Status=? WHERE TestDriveID=?";
        try(Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setString(1,newStatus); ps.setInt(2,testDriveID); return ps.executeUpdate()>0;
        } catch(SQLException e){ e.printStackTrace(); }
        return false;
    }
}
