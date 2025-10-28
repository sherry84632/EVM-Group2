package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;

import java.sql.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Repository
public class DAOQuotation {

    private static final Logger log = LoggerFactory.getLogger(DAOQuotation.class);

    // ✅ Lấy thông tin xe theo VehicleID
    public DTOVehicle getVehicleById(Integer vehicleId) {
        DTOVehicle vehicle = null;
        log.debug("getVehicleById ID={}", vehicleId);

        String sql = """
                    SELECT v.VehicleID, v.ManufactureYear, v.EngineNumber, v.Status, v.CreatedAt, v.UpdatedAt,
                           vc.ColorID, vc.ColorName,
                           vv.VersionID, vv.VersionName,
                           vm.ModelID, vm.ModelName, vm.BasePrice
                    FROM Vehicle v
                    LEFT JOIN VehicleColor vc ON v.ColorID = vc.ColorID
                    LEFT JOIN VehicleVersion vv ON v.VersionID = vv.VersionID
                    LEFT JOIN VehicleModel vm ON vv.ModelID = vm.ModelID
                    WHERE v.VehicleID = ?
                """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, vehicleId);
            log.trace("Executing query for VehicleID={}", vehicleId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    vehicle = new DTOVehicle();
                    vehicle.setVehicleID(rs.getInt("VehicleID"));
                    vehicle.setManufactureYear(rs.getInt("ManufactureYear"));
                    vehicle.setEngineNumber(rs.getString("EngineNumber"));
                    vehicle.setStatus(VehicleStatus.valueOf(rs.getString("Status")));
                    vehicle.setCreatedAt(rs.getTimestamp("CreatedAt"));
                    vehicle.setUpdatedAt(rs.getTimestamp("UpdatedAt"));

                    // Set color relationship
                    if (rs.getString("ColorName") != null) {
                        DTOVehicleColor color = new DTOVehicleColor();
                        color.setColorID(rs.getInt("ColorID"));
                        color.setColorName(rs.getString("ColorName"));
                        vehicle.setColor(color);
                    }

                    // Set version relationship
                    if (rs.getString("VersionName") != null) {
                        DTOVehicleVersion version = new DTOVehicleVersion();
                        version.setVersionID(rs.getInt("VersionID"));
                        version.setVersionName(rs.getString("VersionName"));
                        vehicle.setVersion(version);
                    }

                    log.debug("Vehicle found ID={}", vehicle.getVehicleID());
                } else {
                    log.warn("No vehicle found ID={}", vehicleId);
                }
            }
        } catch (SQLException e) {
            log.error("SQL error fetching vehicle ID={}", vehicleId, e);
        }

        return vehicle;
    }

    // ✅ Lấy thông tin Dealer theo dealerID (từ tài khoản đăng nhập)
    public DTODealer getDealerByID(int dealerID) {
        DTODealer dealer = null;

        String sql = """
                    SELECT DealerID, DealerName, Email, Phone, Address
                    FROM Dealer
                    WHERE DealerID = ?
                """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, dealerID);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    dealer = new DTODealer();
                    dealer.setDealerID(rs.getInt("DealerID"));
                    dealer.setDealerName(rs.getString("DealerName"));
                    dealer.setEmail(rs.getString("Email"));
                    dealer.setPhone(rs.getString("Phone"));
                    dealer.setAddress(rs.getString("Address"));
                }
            }
        } catch (SQLException e) {
            log.error("Error getting dealer dealerID={}", dealerID, e);
        }

        return dealer;
    }

    // 🔥 CORE FLOW STEP 1: Insert new quotation with price calculation
    public int insertQuotation(DTOQuotation quotation) {
        String insertQuotationSQL = """
                    INSERT INTO Quotation (CustomerID, StaffID, DealerID, CreatedAt, Status, TotalAmount, Quantity, LevelID)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DBUtils.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // 1. Insert main Quotation
                try (PreparedStatement ps = conn.prepareStatement(insertQuotationSQL, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, quotation.getCustomer().getCustomerID());
                    ps.setInt(2, quotation.getStaff() != null ? quotation.getStaff().getStaffID() : 0);
                    ps.setInt(3, quotation.getDealer().getDealerID());
                    ps.setTimestamp(4, quotation.getCreatedAt());
                    ps.setString(5, quotation.getStatus() != null ? quotation.getStatus().toString() : "CREATED");
                    ps.setDouble(6, quotation.getTotalPrice());
                    ps.setInt(7, quotation.getQuantity());
                    ps.setInt(8, quotation.getLevelID());

                    int affectedRows = ps.executeUpdate();
                    if (affectedRows == 0) {
                        throw new SQLException("Creating quotation failed, no rows affected.");
                    }

                    int quotationID;
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            quotationID = rs.getInt(1);
                            log.info("Quotation inserted id={}", quotationID);
                        } else {
                            throw new SQLException("Failed to retrieve QuotationID.");
                        }
                    }

                    conn.commit();
                    return quotationID;
                }

            } catch (SQLException e) {
                conn.rollback();
                log.error("Transaction failed, rollback", e);
                return -1;
            }

        } catch (SQLException e) {
            log.error("Insert quotation outer error", e);
            return -1;
        }
    }

    // 🔥 CORE FLOW STEP 2: Get quotation by ID (for approval/review)
    public DTOQuotation getQuotationById(int quotationID) {
        DTOQuotation quotation = null;

        String sql = """
                    SELECT q.QuotationID, q.CreatedAt, q.Status, q.TotalAmount, q.Quantity, q.LevelID,
                           c.CustomerID, c.FullName AS CustomerName, c.Email AS CustomerEmail, c.Phone AS CustomerPhone,
                           d.DealerID, d.DealerName, d.Email AS DealerEmail, d.Phone AS DealerPhone,
                           ds.StaffID, ds.FullName AS StaffName, ds.Email AS StaffEmail, ds.Phone AS StaffPhone
                    FROM Quotation q
                    JOIN Customer c ON q.CustomerID = c.CustomerID
                    JOIN Dealer d ON q.DealerID = d.DealerID
                    LEFT JOIN DealerStaff ds ON q.StaffID = ds.StaffID
                    WHERE q.QuotationID = ?
                """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, quotationID);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    quotation = new DTOQuotation();
                    quotation.setQuotationID(rs.getInt("QuotationID"));
                    quotation.setCreatedAt(rs.getTimestamp("CreatedAt"));
                    quotation.setStatus(QuotationStatus.valueOf(rs.getString("Status")));
                    quotation.setTotalPrice(rs.getDouble("TotalAmount"));
                    quotation.setQuantity(rs.getInt("Quantity"));
                    quotation.setLevelID(rs.getInt("LevelID"));

                    // Customer info
                    DTOCustomer customer = new DTOCustomer();
                    customer.setCustomerID(rs.getInt("CustomerID"));
                    customer.setFullName(rs.getString("CustomerName"));
                    customer.setEmail(rs.getString("CustomerEmail"));
                    customer.setPhone(rs.getString("CustomerPhone"));
                    quotation.setCustomer(customer);

                    // Dealer info
                    DTODealer dealer = new DTODealer();
                    dealer.setDealerID(rs.getInt("DealerID"));
                    dealer.setDealerName(rs.getString("DealerName"));
                    dealer.setEmail(rs.getString("DealerEmail"));
                    dealer.setPhone(rs.getString("DealerPhone"));
                    quotation.setDealer(dealer);

                    // Staff info (if available)
                    if (rs.getString("StaffName") != null) {
                        DTODealerStaff staff = new DTODealerStaff();
                        staff.setStaffID(rs.getInt("StaffID"));
                        staff.setFullName(rs.getString("StaffName"));
                        staff.setEmail(rs.getString("StaffEmail"));
                        staff.setPhone(rs.getString("StaffPhone"));
                        quotation.setStaff(staff);
                    }

                    // Load quotation details
                    List<DTOQuotationDetail> details = getQuotationDetails(quotationID);
                    quotation.setQuotationDetails(details);
                }
            }
        } catch (SQLException e) {
            log.error("Error fetching quotation id={}", quotationID, e);
        }

        return quotation;
    }

    // 🔥 CORE FLOW STEP 3: Get all quotations with price information
    public List<DTOQuotation> getAllQuotations() {
        List<DTOQuotation> quotations = new ArrayList<>();

        String sql = """
                    SELECT q.QuotationID, q.CreatedAt, q.Status, q.TotalAmount, q.Quantity, q.LevelID,
                           c.CustomerID, c.FullName AS CustomerName, c.Email AS CustomerEmail, c.Phone AS CustomerPhone,
                           d.DealerID, d.DealerName, d.Email AS DealerEmail, d.Phone AS DealerPhone,
                           ds.StaffID, ds.FullName AS StaffName, ds.Email AS StaffEmail, ds.Phone AS StaffPhone
                    FROM Quotation q
                    JOIN Customer c ON q.CustomerID = c.CustomerID
                    JOIN Dealer d ON q.DealerID = d.DealerID
                    LEFT JOIN DealerStaff ds ON q.StaffID = ds.StaffID
                    ORDER BY q.CreatedAt DESC
                """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                DTOQuotation quotation = new DTOQuotation();
                quotation.setQuotationID(rs.getInt("QuotationID"));
                quotation.setCreatedAt(rs.getTimestamp("CreatedAt"));
                quotation.setStatus(QuotationStatus.valueOf(rs.getString("Status")));
                quotation.setTotalPrice(rs.getDouble("TotalAmount"));
                quotation.setQuantity(rs.getInt("Quantity"));
                quotation.setLevelID(rs.getInt("LevelID"));

                // Customer info
                DTOCustomer customer = new DTOCustomer();
                customer.setCustomerID(rs.getInt("CustomerID"));
                customer.setFullName(rs.getString("CustomerName"));
                customer.setEmail(rs.getString("CustomerEmail"));
                customer.setPhone(rs.getString("CustomerPhone"));
                quotation.setCustomer(customer);

                // Dealer info
                DTODealer dealer = new DTODealer();
                dealer.setDealerID(rs.getInt("DealerID"));
                dealer.setDealerName(rs.getString("DealerName"));
                dealer.setEmail(rs.getString("DealerEmail"));
                dealer.setPhone(rs.getString("DealerPhone"));
                quotation.setDealer(dealer);

                // Staff info (if available)
                if (rs.getString("StaffName") != null) {
                    DTODealerStaff staff = new DTODealerStaff();
                    staff.setStaffID(rs.getInt("StaffID"));
                    staff.setFullName(rs.getString("StaffName"));
                    staff.setEmail(rs.getString("StaffEmail"));
                    staff.setPhone(rs.getString("StaffPhone"));
                    quotation.setStaff(staff);
                }

                // Load quotation details
                List<DTOQuotationDetail> details = getQuotationDetails(quotation.getQuotationID());
                quotation.setQuotationDetails(details);

                quotations.add(quotation);
            }

        } catch (SQLException e) {
            log.error("Error fetching all quotations", e);
        }

        return quotations;
    }

    // 🔥 CORE FLOW STEP 4: Update quotation status (Approve/Reject)
    public boolean updateQuotationStatus(int quotationID, QuotationStatus newStatus) {
        String sql = "UPDATE Quotation SET Status = ? WHERE QuotationID = ?";
        log.debug("updateQuotationStatus id={} status={}", quotationID, newStatus);

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newStatus.toString());
            ps.setInt(2, quotationID);

            log.trace("Executing status update id={} status={}", quotationID, newStatus);

            int affectedRows = ps.executeUpdate();
            log.trace("Status update affectedRows={}", affectedRows);

            if (affectedRows > 0) {
                log.info("Quotation status updated id={} -> {}", quotationID, newStatus);
                return true;
            } else {
                log.warn("No quotation updated id={} status={}", quotationID, newStatus);
                return false;
            }

        } catch (SQLException e) {
            log.error("Failed updating quotation status id={} status={}", quotationID, newStatus, e);
            return false;
        }
    }

    // 🔥 CORE FLOW STEP 5: Check if quotation is approved (for SaleOrder validation)
    public boolean isQuotationApproved(int quotationID) {
        String sql = "SELECT Status FROM Quotation WHERE QuotationID = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, quotationID);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String status = rs.getString("Status");
                    return "APPROVED".equalsIgnoreCase(status);
                }
            }
        } catch (SQLException e) {
            log.error("Error checking approved quotation id={}", quotationID, e);
        }

        return false;
    }

    // 🔥 CORE FLOW STEP 6: Get quotations by dealer (for dealer-specific view)
    public List<DTOQuotation> getQuotationsByDealer(int dealerID) {
        List<DTOQuotation> quotations = new ArrayList<>();

        String sql = """
                    SELECT q.QuotationID, q.CreatedAt, q.Status, q.TotalAmount, q.Quantity, q.LevelID,
                           c.CustomerID, c.FullName AS CustomerName, c.Email AS CustomerEmail, c.Phone AS CustomerPhone,
                           d.DealerID, d.DealerName, d.Email AS DealerEmail, d.Phone AS DealerPhone,
                           ds.StaffID, ds.FullName AS StaffName, ds.Email AS StaffEmail, ds.Phone AS StaffPhone
                    FROM Quotation q
                    JOIN Customer c ON q.CustomerID = c.CustomerID
                    JOIN Dealer d ON q.DealerID = d.DealerID
                    LEFT JOIN DealerStaff ds ON q.StaffID = ds.StaffID
                    WHERE q.DealerID = ?
                    ORDER BY q.CreatedAt DESC
                """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, dealerID);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DTOQuotation quotation = new DTOQuotation();
                    quotation.setQuotationID(rs.getInt("QuotationID"));
                    quotation.setCreatedAt(rs.getTimestamp("CreatedAt"));
                    quotation.setStatus(QuotationStatus.valueOf(rs.getString("Status")));
                    quotation.setTotalPrice(rs.getDouble("TotalAmount"));
                    quotation.setQuantity(rs.getInt("Quantity"));
                    quotation.setLevelID(rs.getInt("LevelID"));

                    // Customer info
                    DTOCustomer customer = new DTOCustomer();
                    customer.setCustomerID(rs.getInt("CustomerID"));
                    customer.setFullName(rs.getString("CustomerName"));
                    customer.setEmail(rs.getString("CustomerEmail"));
                    customer.setPhone(rs.getString("CustomerPhone"));
                    quotation.setCustomer(customer);

                    // Dealer info
                    DTODealer dealer = new DTODealer();
                    dealer.setDealerID(rs.getInt("DealerID"));
                    dealer.setDealerName(rs.getString("DealerName"));
                    dealer.setEmail(rs.getString("DealerEmail"));
                    dealer.setPhone(rs.getString("DealerPhone"));
                    quotation.setDealer(dealer);

                    // Staff info (if available)
                    if (rs.getString("StaffName") != null) {
                        DTODealerStaff staff = new DTODealerStaff();
                        staff.setStaffID(rs.getInt("StaffID"));
                        staff.setFullName(rs.getString("StaffName"));
                        staff.setEmail(rs.getString("StaffEmail"));
                        staff.setPhone(rs.getString("StaffPhone"));
                        quotation.setStaff(staff);
                    }

                    // Load quotation details
                    List<DTOQuotationDetail> details = getQuotationDetails(quotation.getQuotationID());
                    quotation.setQuotationDetails(details);

                    quotations.add(quotation);
                }
            }
        } catch (SQLException e) {
            log.error("Error fetching quotations by dealer dealerID={}", dealerID, e);
        }

        return quotations;
    }

    // ✅ Get quotations by customer
    public List<DTOQuotation> getQuotationsByCustomer(int customerID) {
        List<DTOQuotation> quotations = new ArrayList<>();

        String sql = """
                    SELECT q.QuotationID, q.CreatedAt, q.Status, q.TotalAmount, q.Quantity, q.LevelID,
                           c.CustomerID, c.FullName AS CustomerName, c.Email AS CustomerEmail, c.Phone AS CustomerPhone,
                           d.DealerID, d.DealerName, d.Email AS DealerEmail, d.Phone AS DealerPhone,
                           ds.StaffID, ds.FullName AS StaffName, ds.Email AS StaffEmail, ds.Phone AS StaffPhone
                    FROM Quotation q
                    JOIN Customer c ON q.CustomerID = c.CustomerID
                    JOIN Dealer d ON q.DealerID = d.DealerID
                    LEFT JOIN DealerStaff ds ON q.StaffID = ds.StaffID
                    WHERE q.CustomerID = ?
                    ORDER BY q.CreatedAt DESC
                """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, customerID);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DTOQuotation quotation = new DTOQuotation();
                    quotation.setQuotationID(rs.getInt("QuotationID"));
                    quotation.setCreatedAt(rs.getTimestamp("CreatedAt"));
                    quotation.setStatus(QuotationStatus.valueOf(rs.getString("Status")));
                    quotation.setTotalPrice(rs.getDouble("TotalAmount"));
                    quotation.setQuantity(rs.getInt("Quantity"));
                    quotation.setLevelID(rs.getInt("LevelID"));

                    // Customer info
                    DTOCustomer customer = new DTOCustomer();
                    customer.setCustomerID(rs.getInt("CustomerID"));
                    customer.setFullName(rs.getString("CustomerName"));
                    customer.setEmail(rs.getString("CustomerEmail"));
                    customer.setPhone(rs.getString("CustomerPhone"));
                    quotation.setCustomer(customer);

                    // Dealer info
                    DTODealer dealer = new DTODealer();
                    dealer.setDealerID(rs.getInt("DealerID"));
                    dealer.setDealerName(rs.getString("DealerName"));
                    dealer.setEmail(rs.getString("DealerEmail"));
                    dealer.setPhone(rs.getString("DealerPhone"));
                    quotation.setDealer(dealer);

                    // Staff info (if available)
                    if (rs.getString("StaffName") != null) {
                        DTODealerStaff staff = new DTODealerStaff();
                        staff.setStaffID(rs.getInt("StaffID"));
                        staff.setFullName(rs.getString("StaffName"));
                        staff.setEmail(rs.getString("StaffEmail"));
                        staff.setPhone(rs.getString("StaffPhone"));
                        quotation.setStaff(staff);
                    }

                    // Load quotation details
                    List<DTOQuotationDetail> details = getQuotationDetails(quotation.getQuotationID());
                    quotation.setQuotationDetails(details);

                    quotations.add(quotation);
                }
            }
        } catch (SQLException e) {
            log.error("Error fetching quotations by customer customerID={}", customerID, e);
        }

        return quotations;
    }

    // ✅ Update quotation total amount
    public boolean updateQuotationTotalAmount(int quotationID, double totalAmount) {
        String sql = "UPDATE Quotation SET TotalAmount = ? WHERE QuotationID = ?";
        log.debug("updateQuotationTotalAmount id={} totalAmount={}", quotationID, totalAmount);

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDouble(1, totalAmount);
            ps.setInt(2, quotationID);

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                log.info("Quotation total amount updated id={} -> {}", quotationID, totalAmount);
                return true;
            } else {
                log.warn("No quotation updated id={} totalAmount={}", quotationID, totalAmount);
                return false;
            }

        } catch (SQLException e) {
            log.error("Failed updating quotation total amount id={} totalAmount={}", quotationID, totalAmount, e);
            return false;
        }
    }

    // ✅ Insert QuotationDetail
    public boolean insertQuotationDetail(DTOQuotationDetail detail) {
        String sql = """
                    INSERT INTO QuotationDetail (QuotationID, VersionID, ColorID, UnitPrice)
                    VALUES (?, ?, ?, ?)
                """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, detail.getQuotation().getQuotationID());
            ps.setInt(2, detail.getVersion().getVersionID());
            ps.setInt(3, detail.getColor().getColorID());
            ps.setBigDecimal(4, detail.getUnitPrice());

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                log.info("QuotationDetail inserted quotationID={} versionID={} colorID={}", 
                        detail.getQuotation().getQuotationID(), 
                        detail.getVersion().getVersionID(), 
                        detail.getColor().getColorID());
                return true;
            } else {
                log.warn("No QuotationDetail inserted quotationID={}", detail.getQuotation().getQuotationID());
                return false;
            }

        } catch (SQLException e) {
            log.error("Failed to insert QuotationDetail quotationID={}", detail.getQuotation().getQuotationID(), e);
            return false;
        }
    }

    // ✅ Get QuotationDetails by QuotationID
    public List<DTOQuotationDetail> getQuotationDetails(int quotationID) {
        List<DTOQuotationDetail> details = new ArrayList<>();

        String sql = """
                    SELECT qd.QuotationDetailID, qd.QuotationID, qd.VersionID, qd.ColorID, qd.UnitPrice,
                           vv.VersionName,
                           vc.ColorName,
                           vm.ModelID, vm.ModelName
                    FROM QuotationDetail qd
                    LEFT JOIN VehicleVersion vv ON qd.VersionID = vv.VersionID
                    LEFT JOIN VehicleColor vc ON qd.ColorID = vc.ColorID
                    LEFT JOIN VehicleModel vm ON vv.ModelID = vm.ModelID
                    WHERE qd.QuotationID = ?
                """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, quotationID);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DTOQuotationDetail detail = new DTOQuotationDetail();
                    detail.setQuotationDetailID(rs.getInt("QuotationDetailID"));
                    detail.setUnitPrice(rs.getBigDecimal("UnitPrice"));

                    // Set quotation relationship
                    DTOQuotation quotation = new DTOQuotation();
                    quotation.setQuotationID(rs.getInt("QuotationID"));
                    detail.setQuotation(quotation);

                    // Set version relationship
                    if (rs.getString("VersionName") != null) {
                        DTOVehicleVersion version = new DTOVehicleVersion();
                        version.setVersionID(rs.getInt("VersionID"));
                        version.setVersionName(rs.getString("VersionName"));
                        detail.setVersion(version);
                    }

                    // Set color relationship
                    if (rs.getString("ColorName") != null) {
                        DTOVehicleColor color = new DTOVehicleColor();
                        color.setColorID(rs.getInt("ColorID"));
                        color.setColorName(rs.getString("ColorName"));
                        detail.setColor(color);
                    }

                    details.add(detail);
                }
            }
        } catch (SQLException e) {
            log.error("Error fetching quotation details quotationID={}", quotationID, e);
        }

        return details;
    }

    // ✅ Update QuotationDetail
    public boolean updateQuotationDetail(DTOQuotationDetail detail) {
        String sql = """
                    UPDATE QuotationDetail 
                    SET VersionID = ?, ColorID = ?, UnitPrice = ?
                    WHERE QuotationDetailID = ?
                """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, detail.getVersion().getVersionID());
            ps.setInt(2, detail.getColor().getColorID());
            ps.setBigDecimal(3, detail.getUnitPrice());
            ps.setInt(4, detail.getQuotationDetailID());

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                log.info("QuotationDetail updated id={}", detail.getQuotationDetailID());
                return true;
            } else {
                log.warn("No QuotationDetail updated id={}", detail.getQuotationDetailID());
                return false;
            }

        } catch (SQLException e) {
            log.error("Failed to update QuotationDetail id={}", detail.getQuotationDetailID(), e);
            return false;
        }
    }

    // ✅ Delete QuotationDetail
    public boolean deleteQuotationDetail(int quotationDetailID) {
        String sql = "DELETE FROM QuotationDetail WHERE QuotationDetailID = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, quotationDetailID);

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                log.info("QuotationDetail deleted id={}", quotationDetailID);
                return true;
            } else {
                log.warn("No QuotationDetail deleted id={}", quotationDetailID);
                return false;
            }

        } catch (SQLException e) {
            log.error("Failed to delete QuotationDetail id={}", quotationDetailID, e);
            return false;
        }
    }
}
