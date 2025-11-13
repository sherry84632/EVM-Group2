package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Repository
public class DAOQuotation {

    private static final Logger log = LoggerFactory.getLogger(DAOQuotation.class);

    //  Lấy thông tin xe theo VehicleID
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
                    String statusStr = rs.getString("Status");
                    if (statusStr != null && !statusStr.isBlank()) {
                        try {
                            vehicle.setStatus(VehicleStatus.valueOf(statusStr));
                        } catch (IllegalArgumentException ex) {
                            log.warn("Unknown VehicleStatus '{}' for VehicleID={}. Setting null", statusStr, vehicleId);
                        }
                    }
                    vehicle.setCreatedAt(rs.getTimestamp("CreatedAt"));
                    vehicle.setUpdatedAt(rs.getTimestamp("UpdatedAt"));

                    // Color relationship
                    if (rs.getString("ColorName") != null) {
                        DTOVehicleColor color = new DTOVehicleColor();
                        color.setColorID(rs.getInt("ColorID"));
                        color.setColorName(rs.getString("ColorName"));
                        vehicle.setColor(color);
                    }

                    // Version + Model relationship (needed for basePrice & modelName)
                    if (rs.getString("VersionName") != null) {
                        DTOVehicleVersion version = new DTOVehicleVersion();
                        version.setVersionID(rs.getInt("VersionID"));
                        version.setVersionName(rs.getString("VersionName"));
                        // Attach model if present
                        if (rs.getString("ModelName") != null) {
                            DTOVehicleModel model = new DTOVehicleModel();
                            model.setModelID(rs.getInt("ModelID"));
                            model.setModelName(rs.getString("ModelName"));
                            model.setBasePrice(rs.getBigDecimal("BasePrice"));
                            version.setModel(model);
                        }
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

    //  Lấy thông tin Dealer theo dealerID (từ tài khoản đăng nhập)
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

    //  CORE FLOW STEP 1: Insert new quotation with price calculation
    public int insertQuotation(DTOQuotation quotation) {
        String insertQuotationSQL = "INSERT INTO Quotation (CustomerID, StaffID, DealerID, CreatedAt, Status, TotalAmount, Quantity, LevelID, DiscountPercent) VALUES (?,?,?,?,?,?,?,?,?)";

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
                    ps.setObject(9, quotation.getDiscountPercent());

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

    // CORE FLOW STEP 2: Get quotation by ID (for approval/review)
    public DTOQuotation getQuotationById(int quotationID) {
        DTOQuotation quotation = null;

        String sql = "SELECT q.QuotationID, q.CreatedAt, q.Status, q.TotalAmount, q.Quantity, q.LevelID, q.DiscountPercent, c.CustomerID, c.FullName AS CustomerName, c.Email AS CustomerEmail, c.Phone AS CustomerPhone, d.DealerID, d.DealerName, d.Email AS DealerEmail, d.Phone AS DealerPhone, ds.StaffID, ds.FullName AS StaffName, ds.Email AS StaffEmail, ds.Phone AS StaffPhone FROM Quotation q JOIN Customer c ON q.CustomerID = c.CustomerID JOIN Dealer d ON q.DealerID = d.DealerID LEFT JOIN DealerStaff ds ON q.StaffID = ds.StaffID WHERE q.QuotationID = ?";

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
                    quotation.setDiscountPercent(rs.getObject("DiscountPercent") != null ? rs.getDouble("DiscountPercent") : null);

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

    //  CORE FLOW STEP 3: Get all quotations with price information
    public List<DTOQuotation> getAllQuotations() {
        List<DTOQuotation> quotations = new ArrayList<>();

        String sql = "SELECT q.QuotationID, q.CreatedAt, q.Status, q.TotalAmount, q.Quantity, q.LevelID, q.DiscountPercent, c.CustomerID, c.FullName AS CustomerName, c.Email AS CustomerEmail, c.Phone AS CustomerPhone, d.DealerID, d.DealerName, d.Email AS DealerEmail, d.Phone AS DealerPhone, ds.StaffID, ds.FullName AS StaffName, ds.Email AS StaffEmail, ds.Phone AS StaffPhone FROM Quotation q JOIN Customer c ON q.CustomerID = c.CustomerID JOIN Dealer d ON q.DealerID = d.DealerID LEFT JOIN DealerStaff ds ON q.StaffID = ds.StaffID ORDER BY q.CreatedAt DESC";

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
                quotation.setDiscountPercent(rs.getObject("DiscountPercent") != null ? rs.getDouble("DiscountPercent") : null);

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

    // Get quotations filtered by dealer ID
    public List<DTOQuotation> getQuotationsByDealerId(int dealerId) {
        List<DTOQuotation> quotations = new ArrayList<>();

        String sql = "SELECT q.QuotationID, q.CreatedAt, q.Status, q.TotalAmount, q.Quantity, q.LevelID, q.DiscountPercent, c.CustomerID, c.FullName AS CustomerName, c.Email AS CustomerEmail, c.Phone AS CustomerPhone, d.DealerID, d.DealerName, d.Email AS DealerEmail, d.Phone AS DealerPhone, ds.StaffID, ds.FullName AS StaffName, ds.Email AS StaffEmail, ds.Phone AS StaffPhone FROM Quotation q JOIN Customer c ON q.CustomerID = c.CustomerID JOIN Dealer d ON q.DealerID = d.DealerID LEFT JOIN DealerStaff ds ON q.StaffID = ds.StaffID WHERE q.DealerID = ? ORDER BY q.CreatedAt DESC";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, dealerId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DTOQuotation quotation = new DTOQuotation();
                    quotation.setQuotationID(rs.getInt("QuotationID"));
                    quotation.setCreatedAt(rs.getTimestamp("CreatedAt"));
                    quotation.setStatus(QuotationStatus.valueOf(rs.getString("Status")));
                    quotation.setTotalPrice(rs.getDouble("TotalAmount"));
                    quotation.setQuantity(rs.getInt("Quantity"));
                    quotation.setLevelID(rs.getInt("LevelID"));
                    quotation.setDiscountPercent(rs.getObject("DiscountPercent") != null ? rs.getDouble("DiscountPercent") : null);

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
            log.error("Error fetching quotations by dealerId={}", dealerId, e);
        }

        return quotations;
    }

    //  CORE FLOW STEP 4: Update quotation status (Approve/Reject)
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

    //  CORE FLOW STEP 5: Check if quotation is approved (for SaleOrder validation)
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

    //  CORE FLOW STEP 6: Get quotations by dealer (for dealer-specific view)
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
                    quotation.setDiscountPercent(rs.getObject("DiscountPercent") != null ? rs.getDouble("DiscountPercent") : null);

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

    //  Get quotations by customer
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
                    quotation.setDiscountPercent(rs.getObject("DiscountPercent") != null ? rs.getDouble("DiscountPercent") : null);

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

    //  Update quotation total amount
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

    //  Find existing quotation by customer and dealer
    public Integer findExistingQuotationId(int dealerID, int customerID) {
        String sql = "SELECT QuotationID FROM Quotation WHERE DealerID = ? AND CustomerID = ? AND Status <> 'REJECTED' ORDER BY CreatedAt DESC";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dealerID); ps.setInt(2, customerID);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt("QuotationID"); }
        } catch (SQLException e) { log.error("findExistingQuotationId dealerID={} customerID={}", dealerID, customerID, e); }
        return null;
    }

    //  Create quotation if not exists, return existing or new QuotationID
    public int createQuotationIfNotExists(int dealerID, int customerID, int staffID, int levelID) {
        Integer existing = findExistingQuotationId(dealerID, customerID); if (existing != null) return existing;
        DTOQuotation q = new DTOQuotation();
        DTODealer d = new DTODealer(); d.setDealerID(dealerID); q.setDealer(d);
        DTOCustomer c = new DTOCustomer(); c.setCustomerID(customerID); q.setCustomer(c);
        q.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
        q.setStatus(QuotationStatus.CREATED); q.setTotalPrice(0); q.setQuantity(0); q.setLevelID(levelID);
        q.setDiscountPercent(0.0);
        if (staffID > 0) { DTODealerStaff s = new DTODealerStaff(); s.setStaffID(staffID); q.setStaff(s); }
        return insertQuotation(q);
    }

    //  Insert QuotationDetail (restored after truncation)
    public boolean insertQuotationDetail(DTOQuotationDetail detail) {
        String sql = "INSERT INTO QuotationDetail (QuotationID, VersionID, ColorID, UnitPrice, Quantity) VALUES (?,?,?,?,?)";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, detail.getQuotation().getQuotationID());
            ps.setInt(2, detail.getVersion().getVersionID());
            ps.setInt(3, detail.getColor().getColorID());
            ps.setBigDecimal(4, detail.getUnitPrice());
            ps.setInt(5, detail.getQuantity());
            int rows = ps.executeUpdate();
            if (rows > 0) { log.info("QuotationDetail inserted quotationID={} versionID={} colorID={} qty={}", detail.getQuotation().getQuotationID(), detail.getVersion().getVersionID(), detail.getColor().getColorID(), detail.getQuantity()); return true; }
            log.warn("No QuotationDetail inserted quotationID={}", detail.getQuotation().getQuotationID());
        } catch (SQLException e) { log.error("Failed to insert QuotationDetail quotationID={}", detail.getQuotation().getQuotationID(), e); }
        return false;
    }

    //  Get QuotationDetails by QuotationID (with model attached)
    public List<DTOQuotationDetail> getQuotationDetails(int quotationID) {
        List<DTOQuotationDetail> details = new ArrayList<>();
        String sql = """
                SELECT qd.QuotationDetailID, qd.QuotationID, qd.VersionID, qd.ColorID, qd.UnitPrice, qd.Quantity, qd.AppliedDealerDiscountPercent,
                       vv.VersionName, vm.ModelID, vm.ModelName, vc.ColorID AS CColorID, vc.ColorName,
                       q.DiscountPercent AS QuotationDiscountPercent
                FROM QuotationDetail qd
                LEFT JOIN VehicleVersion vv ON qd.VersionID = vv.VersionID
                LEFT JOIN VehicleModel vm ON vv.ModelID = vm.ModelID
                LEFT JOIN VehicleColor vc ON qd.ColorID = vc.ColorID
                LEFT JOIN Quotation q ON qd.QuotationID = q.QuotationID
                WHERE qd.QuotationID = ?
                """;
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quotationID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DTOQuotationDetail d = new DTOQuotationDetail();
                    d.setQuotationDetailID(rs.getInt("QuotationDetailID"));
                    d.setUnitPrice(rs.getBigDecimal("UnitPrice"));
                    d.setQuantity(rs.getInt("Quantity"));
                    Double appliedDiscount = rs.getDouble("AppliedDealerDiscountPercent");
                    if (!rs.wasNull()) {
                        d.setAppliedDealerDiscountPercent(appliedDiscount);
                    }
                    DTOQuotation qRef = new DTOQuotation();
                    qRef.setQuotationID(rs.getInt("QuotationID"));
                    Double qDisc = (Double) rs.getObject("QuotationDiscountPercent");
                    if (qDisc != null) qRef.setDiscountPercent(qDisc);
                    d.setQuotation(qRef);
                    if (rs.getString("VersionName") != null) {
                        DTOVehicleVersion v = new DTOVehicleVersion();
                        v.setVersionID(rs.getInt("VersionID"));
                        v.setVersionName(rs.getString("VersionName"));
                        if (rs.getString("ModelName") != null) {
                            DTOVehicleModel m = new DTOVehicleModel();
                            m.setModelID(rs.getInt("ModelID"));
                            m.setModelName(rs.getString("ModelName"));
                            v.setModel(m);
                        }
                        d.setVersion(v);
                    }
                    if (rs.getString("ColorName") != null) {
                        DTOVehicleColor c = new DTOVehicleColor();
                        c.setColorID(rs.getInt("CColorID"));
                        c.setColorName(rs.getString("ColorName"));
                        d.setColor(c);
                    }
                    // Compute final net after stacking discounts (dealer line then quotation base)
                    java.math.BigDecimal unitGross = d.getUnitPrice() != null ? d.getUnitPrice() : java.math.BigDecimal.ZERO;
                    double dealerPct = d.getAppliedDealerDiscountPercent() != null ? d.getAppliedDealerDiscountPercent() : 0.0;
                    double basePct = qRef.getDiscountPercent() != null ? qRef.getDiscountPercent() : 0.0;
                    java.math.BigDecimal netAfterDealer = unitGross.multiply(java.math.BigDecimal.valueOf(1 - dealerPct / 100.0));
                    java.math.BigDecimal finalNet = netAfterDealer.multiply(java.math.BigDecimal.valueOf(1 - basePct / 100.0));
                    d.setFinalNetAfterAll(finalNet);
                    details.add(d);
                }
            }
        } catch (SQLException e) {
            log.error("Error fetching quotation details quotationID={}", quotationID, e);
        }
        return details;
    }

    //  Update QuotationDetail
    public boolean updateQuotationDetail(DTOQuotationDetail detail) {
        String sql = "UPDATE QuotationDetail SET VersionID = ?, ColorID = ?, UnitPrice = ?, Quantity = ?, AppliedDealerDiscountPercent = ? WHERE QuotationDetailID = ?";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, detail.getVersion().getVersionID());
            ps.setInt(2, detail.getColor().getColorID());
            ps.setBigDecimal(3, detail.getUnitPrice());
            ps.setInt(4, detail.getQuantity());
            
            // Set AppliedDealerDiscountPercent (nullable)
            if (detail.getAppliedDealerDiscountPercent() != null) {
                ps.setDouble(5, detail.getAppliedDealerDiscountPercent());
            } else {
                ps.setNull(5, java.sql.Types.DECIMAL);
            }
            
            ps.setInt(6, detail.getQuotationDetailID());
            
            int rows = ps.executeUpdate();
            if (rows > 0) {
                log.info("QuotationDetail updated id={} with dealer discount={}", 
                    detail.getQuotationDetailID(), detail.getAppliedDealerDiscountPercent());
                return true;
            }
            log.warn("No QuotationDetail updated id={}", detail.getQuotationDetailID());
        } catch (SQLException e) {
            log.error("Failed to update QuotationDetail id={}", detail.getQuotationDetailID(), e);
        }
        return false;
    }

    //  Delete QuotationDetail
    public boolean deleteQuotationDetail(int quotationDetailID) {
        String sql = "DELETE FROM QuotationDetail WHERE QuotationDetailID = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quotationDetailID);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                log.info("QuotationDetail deleted id={}", quotationDetailID);
                return true;
            }
            log.warn("No QuotationDetail deleted id={}", quotationDetailID);
        } catch (SQLException e) {
            log.error("Failed to delete QuotationDetail id={}", quotationDetailID, e);
        }
        return false;
    }

    //  Recalculate Quotation total & quantity (gross/net) using BOTH dealer discount AND base discount
    public void recalcQuotationTotal(int quotationID) {
        List<DTOQuotationDetail> details = getQuotationDetails(quotationID);
        int totalQty = details.stream().mapToInt(DTOQuotationDetail::getQuantity).sum();

        // Calculate gross total (before any discounts)
        double gross = details.stream().mapToDouble(d -> d.getSubtotal().doubleValue()).sum();

        // Apply dealer discount (line-level) first
        double afterDealerDiscount = details.stream().mapToDouble(d -> {
            double subtotal = d.getSubtotal().doubleValue();
            Double dealerDiscountPct = d.getAppliedDealerDiscountPercent();
            if (dealerDiscountPct != null && dealerDiscountPct > 0) {
                return subtotal * (1 - dealerDiscountPct / 100.0);
            }
            return subtotal;
        }).sum();

        // Then apply base discount (quotation-level) on top
        DTOQuotation q = getQuotationById(quotationID);
        double baseDiscountPct = (q != null && q.getDiscountPercent() != null) ? q.getDiscountPercent() : 0.0;
        double finalNet = afterDealerDiscount * (1 - baseDiscountPct / 100.0);

        // Update database with final net total
        String sql = "UPDATE Quotation SET TotalAmount = ?, Quantity = ? WHERE QuotationID = ?";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, finalNet);
            ps.setInt(2, totalQty);
            ps.setInt(3, quotationID);
            ps.executeUpdate();
            log.info("Recalculated quotation total: gross={}, afterDealer={}, finalNet={} (base discount={}%)",
                gross, afterDealerDiscount, finalNet, baseDiscountPct);
        } catch (SQLException e) {
            log.error("Failed updating aggregates quotationID={}", quotationID, e);
        }
    }

    //  Update quotation discount percent and recompute total immediately
    public boolean updateQuotationDiscount(int quotationID, double discountPercent) {
        String sql = "UPDATE Quotation SET DiscountPercent = ? WHERE QuotationID = ?"; // fixed missing declaration
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, discountPercent); ps.setInt(2, quotationID); boolean ok = ps.executeUpdate() > 0; if (ok) recalcQuotationTotal(quotationID); return ok;
        } catch (SQLException e) { log.error("Failed updating discount quotationID={}", quotationID, e); return false; }
    }

    //  Update quotation detail quantity only
    public boolean updateQuotationDetailQuantity(int quotationDetailID, int quantity) {
        String sql = "UPDATE QuotationDetail SET Quantity = ? WHERE QuotationDetailID = ?";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Math.max(1, quantity));
            ps.setInt(2, quotationDetailID);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            log.error("Failed to update detail quantity id={} qty={}", quotationDetailID, quantity, e);
            return false;
        }
    }

    // check if quotation detail exists
    public boolean existsQuotationDetail(int quotationID, int versionID, int colorID) {
        String sql = "SELECT 1 FROM QuotationDetail WHERE QuotationID=? AND VersionID=? AND ColorID=?";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quotationID); ps.setInt(2, versionID); ps.setInt(3, colorID);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { log.error("existsQuotationDetail check failed qID={} vID={} cID={}", quotationID, versionID, colorID, e); }
        return false;
    }

    // bulk add vehicles (version/color/price) ignoring discount and skipping duplicates
    public int addMultipleDetails(int quotationID, List<DTOVehicle> vehicles, int defaultQty) {
        int added = 0;
        for (DTOVehicle veh : vehicles) {
            if (veh.getVersion()==null || veh.getColor()==null) continue;
            int vID = veh.getVersion().getVersionID(); int cID = veh.getColor().getColorID();
            if (existsQuotationDetail(quotationID, vID, cID)) { log.debug("Skip duplicate versionID={} colorID={}", vID, cID); continue; }
            DTOQuotationDetail d = new DTOQuotationDetail();
            DTOQuotation qRef = new DTOQuotation(); qRef.setQuotationID(quotationID); d.setQuotation(qRef);
            DTOVehicleVersion vRef = new DTOVehicleVersion(); vRef.setVersionID(vID); d.setVersion(vRef);
            DTOVehicleColor cRef = new DTOVehicleColor(); cRef.setColorID(cID); d.setColor(cRef);
            d.setUnitPrice(veh.getVersion().getModel()!=null?veh.getVersion().getModel().getBasePrice():java.math.BigDecimal.ZERO);
            d.setQuantity(Math.max(1, defaultQty));
            if (insertQuotationDetail(d)) added++;
        }
        if (added>0) recalcQuotationTotal(quotationID);
        return added;
    }

    // bulk add vehicles (version/color/price) with specific quantities, ignoring discount and skipping duplicates
    public int addMultipleDetailsWithQuantities(int quotationID, List<DTOVehicle> vehicles, List<Integer> quantities) {
        int added = 0;
        for (int i=0;i<vehicles.size();i++) {
            DTOVehicle veh = vehicles.get(i);
            if (veh==null || veh.getVersion()==null || veh.getColor()==null) continue;
            int vID = veh.getVersion().getVersionID();
            int cID = veh.getColor().getColorID();
            if (existsQuotationDetail(quotationID, vID, cID)) continue;
            int qty = 1;
            if (quantities != null && quantities.size() > i) {
                Integer qVal = quantities.get(i);
                if (qVal != null && qVal > 0) qty = qVal; }
            DTOQuotationDetail d = new DTOQuotationDetail();
            DTOQuotation qRef = new DTOQuotation(); qRef.setQuotationID(quotationID); d.setQuotation(qRef);
            DTOVehicleVersion vRef = new DTOVehicleVersion(); vRef.setVersionID(vID); d.setVersion(vRef);
            DTOVehicleColor cRef = new DTOVehicleColor(); cRef.setColorID(cID); d.setColor(cRef);
            d.setUnitPrice(veh.getVersion().getModel()!=null?veh.getVersion().getModel().getBasePrice():java.math.BigDecimal.ZERO);
            d.setQuantity(qty);
            if (insertQuotationDetail(d)) added++;
        }
        if (added>0) recalcQuotationTotal(quotationID);
        return added;
    }

    //  Get detailed information of a specific quotation detail by ID (with joins)
    public DTOQuotationDetail getQuotationDetailById(int detailId) {
        String sql = "SELECT qd.QuotationDetailID, qd.QuotationID, qd.VersionID, qd.ColorID, qd.UnitPrice, qd.Quantity, qd.AppliedDealerDiscountPercent, " +
                "vv.VersionName, vm.ModelID, vm.ModelName, vm.BasePrice, vc.ColorName, q.DiscountPercent, q.Status " +
                "FROM QuotationDetail qd " +
                "LEFT JOIN VehicleVersion vv ON qd.VersionID = vv.VersionID " +
                "LEFT JOIN VehicleModel vm ON vv.ModelID = vm.ModelID " +
                "LEFT JOIN VehicleColor vc ON qd.ColorID = vc.ColorID " +
                "LEFT JOIN Quotation q ON qd.QuotationID = q.QuotationID " +
                "WHERE qd.QuotationDetailID = ?";
        try (java.sql.Connection conn = utils.DBUtils.getConnection(); java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, detailId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DTOQuotationDetail d = new DTOQuotationDetail();
                    d.setQuotationDetailID(rs.getInt("QuotationDetailID"));

                    DTOQuotation qRef = new DTOQuotation();
                    qRef.setQuotationID(rs.getInt("QuotationID"));
                    Double disc = (Double) rs.getObject("DiscountPercent");
                    if (disc != null) qRef.setDiscountPercent(disc);
                    String status = rs.getString("Status");
                    if (status != null) try { qRef.setStatus(QuotationStatus.valueOf(status)); } catch (IllegalArgumentException ignore) {}
                    d.setQuotation(qRef);

                    DTOVehicleVersion v = new DTOVehicleVersion();
                    v.setVersionID(rs.getInt("VersionID"));
                    v.setVersionName(rs.getString("VersionName"));
                    DTOVehicleModel m = new DTOVehicleModel();
                    m.setModelID(rs.getInt("ModelID"));
                    m.setModelName(rs.getString("ModelName"));
                    m.setBasePrice(rs.getBigDecimal("BasePrice"));
                    v.setModel(m);
                    d.setVersion(v);

                    DTOVehicleColor c = new DTOVehicleColor();
                    c.setColorID(rs.getInt("ColorID"));
                    c.setColorName(rs.getString("ColorName"));
                    d.setColor(c);

                    d.setUnitPrice(rs.getBigDecimal("UnitPrice"));
                    d.setQuantity(rs.getInt("Quantity"));

                    Double appliedDiscount = rs.getDouble("AppliedDealerDiscountPercent");
                    if (!rs.wasNull()) d.setAppliedDealerDiscountPercent(appliedDiscount);
                    // compute stacked final net
                    java.math.BigDecimal unitGross = d.getUnitPrice() != null ? d.getUnitPrice() : java.math.BigDecimal.ZERO;
                    double dealerPct = d.getAppliedDealerDiscountPercent() != null ? d.getAppliedDealerDiscountPercent() : 0.0;
                    double basePct = qRef.getDiscountPercent() != null ? qRef.getDiscountPercent() : 0.0;
                    java.math.BigDecimal netAfterDealer = unitGross.multiply(java.math.BigDecimal.valueOf(1 - dealerPct / 100.0));
                    java.math.BigDecimal finalNet = netAfterDealer.multiply(java.math.BigDecimal.valueOf(1 - basePct / 100.0));
                    d.setFinalNetAfterAll(finalNet);

                    return d;
                }
            }
        } catch (java.sql.SQLException e) {
            org.slf4j.LoggerFactory.getLogger(DAOQuotation.class).error("Error fetching quotation detail id={}", detailId, e);
        }
        return null;
    }

    //  Delete Quotation
    public boolean deleteQuotation(int quotationID) {
        String sqlDetails = "DELETE FROM QuotationDetail WHERE QuotationID=?";
        String sqlQuotation = "DELETE FROM Quotation WHERE QuotationID=?";
        try (java.sql.Connection conn = utils.DBUtils.getConnection()) {
            conn.setAutoCommit(false);
            try (java.sql.PreparedStatement ps1 = conn.prepareStatement(sqlDetails)) {
                ps1.setInt(1, quotationID); ps1.executeUpdate();
            }
            try (java.sql.PreparedStatement ps2 = conn.prepareStatement(sqlQuotation)) {
                ps2.setInt(1, quotationID); int rows = ps2.executeUpdate();
                if (rows > 0) { conn.commit(); org.slf4j.LoggerFactory.getLogger(DAOQuotation.class).info("Deleted quotation id={}", quotationID); return true; }
            }
            conn.rollback();
        } catch (java.sql.SQLException e) {
            org.slf4j.LoggerFactory.getLogger(DAOQuotation.class).error("Failed deleting quotation id={}", quotationID, e);
        }
        return false;
    }

    //  Update unit price and quantity together for a quotation detail
    public boolean updateQuotationDetailFields(int quotationDetailID, java.math.BigDecimal unitPrice, int quantity) {
        String sql = "UPDATE QuotationDetail SET UnitPrice = ?, Quantity = ? WHERE QuotationDetailID = ?";
        try (java.sql.Connection conn = utils.DBUtils.getConnection(); java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, unitPrice != null ? unitPrice : java.math.BigDecimal.ZERO);
            ps.setInt(2, Math.max(1, quantity));
            ps.setInt(3, quotationDetailID);
            return ps.executeUpdate() > 0;
        } catch (java.sql.SQLException e) {
            org.slf4j.LoggerFactory.getLogger(DAOQuotation.class).error("Failed bulk field update detailID={}", quotationDetailID, e);
            return false;
        }
    }

    /** Check if quotation has a COMPLETED sale order -> lock editing */
    public boolean isQuotationLocked(int quotationID) {
        String sql = "SELECT TOP 1 1 FROM SaleOrder WHERE QuotationID = ? AND Status = 'COMPLETED'";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quotationID);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { log.error("isQuotationLocked failed quotationID={}", quotationID, e); }
        return false;
    }
    /** Return the first completed sale order id for display (or null) */
    public Integer getCompletedSaleOrderId(int quotationID) {
        String sql = "SELECT TOP 1 SaleOrderID FROM SaleOrder WHERE QuotationID = ? AND Status = 'COMPLETED' ORDER BY SaleOrderID";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quotationID);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt("SaleOrderID"); }
        } catch (SQLException e) { log.error("getCompletedSaleOrderId failed quotationID={}", quotationID, e); }
        return null;
    }
}
