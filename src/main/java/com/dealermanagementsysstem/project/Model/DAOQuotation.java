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

        // ✅ Lấy thông tin xe theo VIN (JOIN Vehicle + VehicleModel)
        public DTOVehicle getVehicleByVIN(String vin) {
            DTOVehicle vehicle = null;
        log.debug("getVehicleByVIN VIN={}", vin);

            String sql = """
                        SELECT v.VIN, v.ManufactureYear, v.ColorID, vm.ModelName, vm.BasePrice
                        FROM Vehicle v
                        JOIN VehicleModel vm ON v.ModelID = vm.ModelID
                        WHERE v.VIN = ?
                    """;

            try (Connection conn = DBUtils.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, vin);
                log.trace("Executing query for VIN={}", vin);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        vehicle = new DTOVehicle();
                        vehicle.setVIN(rs.getString("VIN"));
                        vehicle.setModelName(rs.getString("ModelName"));
                        vehicle.setManufactureYear(rs.getInt("ManufactureYear"));
                        vehicle.setBasePrice(rs.getBigDecimal("BasePrice"));
                        vehicle.setColorID(rs.getInt("ColorID"));
                        log.debug("Vehicle found model={} colorId={}", vehicle.getModelName(), vehicle.getColorID());
                    } else {
                        log.warn("No vehicle found VIN={}", vin);
                    }
                }
            } catch (SQLException e) {
                log.error("SQL error fetching vehicle VIN={}", vin, e);
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
                        INSERT INTO Quotation (CustomerID, StaffID, DealerID, CreatedAt, Status, LevelID)
                        VALUES (?, ?, ?, ?, ?, ?)
                    """;

            String insertDetailSQL = """
                        INSERT INTO QuotationDetail (QuotationID, VIN, UnitPrice, Quantity, ColorID)
                        VALUES (?, ?, ?, ?, ?)
                    """;

            try (Connection conn = DBUtils.getConnection()) {
                conn.setAutoCommit(false);

                try {
                    // 1. Calculate final price using the formula
            BigDecimal baseFinalPrice = calculateFinalPrice(
                quotation.getVehicle().getVIN(),
                quotation.getDealer().getDealerID()
            );

            // Apply optional extra discount percent (e.g., promotional) if provided
            BigDecimal finalPrice = baseFinalPrice;
            if (quotation.getExtraDiscountPercent() != null && quotation.getExtraDiscountPercent() > 0) {
                BigDecimal extra = BigDecimal.valueOf(quotation.getExtraDiscountPercent()).divide(BigDecimal.valueOf(100));
                finalPrice = finalPrice.multiply(BigDecimal.ONE.subtract(extra));
            }

                    // 2. Insert main Quotation
                    try (PreparedStatement ps = conn.prepareStatement(insertQuotationSQL, Statement.RETURN_GENERATED_KEYS)) {
                        ps.setInt(1, quotation.getCustomer().getCustomerID());
                        int staffId = quotation.getStaff() != null ? quotation.getStaff().getStaffID() : quotation.getDealer().getDealerID(); // fallback old behavior
                        ps.setInt(2, staffId);
                        ps.setInt(3, quotation.getDealer().getDealerID());
                        ps.setTimestamp(4, quotation.getCreatedAt());
                        ps.setString(5, quotation.getStatus() != null ? quotation.getStatus() : "Pending");
                        ps.setInt(6, quotation.getDealer().getLevelID() > 0 ? quotation.getDealer().getLevelID() : 1);

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

                        // 3. Insert QuotationDetail with calculated price
                        try (PreparedStatement psDetail = conn.prepareStatement(insertDetailSQL)) {
                            psDetail.setInt(1, quotationID);
                            psDetail.setString(2, quotation.getVehicle().getVIN());
                            psDetail.setBigDecimal(3, finalPrice);
                            psDetail.setInt(4, Math.max(1, quotation.getQuantity()));
                            psDetail.setInt(5, quotation.getVehicle().getColorID());

                            log.trace("Insert QuotationDetail colorId={}", quotation.getVehicle().getColorID());
                            psDetail.executeUpdate();

                            log.debug("QuotationDetail price={} qty={} extraDiscount={} basePrice={} staffId={}", finalPrice, quotation.getQuantity(), quotation.getExtraDiscountPercent(), baseFinalPrice, staffId);
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
                        SELECT q.QuotationID, q.CreatedAt, q.Status, q.LevelID,
                               c.CustomerID, c.FullName AS CustomerName, c.Email AS CustomerEmail, c.Phone AS CustomerPhone,
                               d.DealerID, d.DealerName, d.Email AS DealerEmail, d.Phone AS DealerPhone,
                               qd.VIN, qd.UnitPrice, qd.Quantity, qd.ColorID,
                               vc.ColorName, vm.ModelName, vm.BasePrice, v.ManufactureYear
                        FROM Quotation q
                        JOIN Customer c ON q.CustomerID = c.CustomerID
                        JOIN Dealer d ON q.DealerID = d.DealerID
                        LEFT JOIN QuotationDetail qd ON q.QuotationID = qd.QuotationID
                        LEFT JOIN VehicleColor vc ON qd.ColorID = vc.ColorID
                        LEFT JOIN Vehicle v ON qd.VIN = v.VIN
                        LEFT JOIN VehicleModel vm ON v.ModelID = vm.ModelID
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
                        quotation.setStatus(rs.getString("Status"));

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

                        // Vehicle info (if available)
                        if (rs.getString("VIN") != null) {
                            DTOVehicle vehicle = new DTOVehicle();
                            vehicle.setVIN(rs.getString("VIN"));
                            vehicle.setModelName(rs.getString("ModelName"));
                            vehicle.setManufactureYear(rs.getInt("ManufactureYear"));
                            vehicle.setBasePrice(rs.getBigDecimal("BasePrice"));
                            vehicle.setColorName(rs.getString("ColorName"));
                            vehicle.setColorID(rs.getInt("ColorID"));
                            quotation.setVehicle(vehicle);
                        }
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
                        SELECT q.QuotationID, q.CreatedAt, q.Status, q.LevelID,
                               c.CustomerID, c.FullName AS CustomerName, c.Email AS CustomerEmail, c.Phone AS CustomerPhone,
                               d.DealerID, d.DealerName, d.Email AS DealerEmail, d.Phone AS DealerPhone,
                               qd.VIN, qd.UnitPrice, qd.Quantity, vc.ColorName, vm.ModelName
                        FROM Quotation q
                        JOIN Customer c ON q.CustomerID = c.CustomerID
                        JOIN Dealer d ON q.DealerID = d.DealerID
                        LEFT JOIN QuotationDetail qd ON q.QuotationID = qd.QuotationID
                        LEFT JOIN VehicleColor vc ON qd.ColorID = vc.ColorID
                        LEFT JOIN Vehicle v ON qd.VIN = v.VIN
                        LEFT JOIN VehicleModel vm ON v.ModelID = vm.ModelID
                        ORDER BY q.CreatedAt DESC
                    """;

            try (Connection conn = DBUtils.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                int lastQuotationId = -1;

                while (rs.next()) {
                    int quotationId = rs.getInt("QuotationID");

                    // If new quotation, create new object
                    if (quotationId != lastQuotationId) {
                        DTOQuotation quotation = new DTOQuotation();
                        quotation.setQuotationID(quotationId);
                        quotation.setCreatedAt(rs.getTimestamp("CreatedAt"));
                        quotation.setStatus(rs.getString("Status"));

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

                        quotations.add(quotation);
                        lastQuotationId = quotationId;
                    }

                    // Add price information if available
                    if (rs.getString("VIN") != null) {
                        DTOQuotation quotation = quotations.get(quotations.size() - 1);

                        // Calculate total price (UnitPrice * Quantity)
                        BigDecimal unitPrice = rs.getBigDecimal("UnitPrice");
                        int quantity = rs.getInt("Quantity");
                        double totalPrice = unitPrice.doubleValue() * quantity;
                        quotation.setTotalPrice(totalPrice);

                        // Set vehicle info
                        DTOVehicle vehicle = new DTOVehicle();
                        vehicle.setVIN(rs.getString("VIN"));
                        vehicle.setModelName(rs.getString("ModelName"));
                        vehicle.setColorName(rs.getString("ColorName"));
                        quotation.setVehicle(vehicle);
                    }
                }

            } catch (SQLException e) {
                log.error("Error fetching all quotations", e);
            }

            return quotations;
        }

        // 🔥 CORE FLOW STEP 4: Update quotation status (Approve/Reject)
        public boolean updateQuotationStatus(int quotationID, String newStatus) {
            String sql = "UPDATE Quotation SET Status = ? WHERE QuotationID = ?";
        log.debug("updateQuotationStatus id={} status={}", quotationID, newStatus);

            try (Connection conn = DBUtils.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, newStatus);
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
                        return "Accepted".equalsIgnoreCase(status) || "Approved".equalsIgnoreCase(status);
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
                        SELECT q.QuotationID, q.CreatedAt, q.Status, q.LevelID,
                               c.CustomerID, c.FullName AS CustomerName, c.Email AS CustomerEmail, c.Phone AS CustomerPhone,
                               d.DealerID, d.DealerName, d.Email AS DealerEmail, d.Phone AS DealerPhone,
                               qd.VIN, qd.UnitPrice, qd.Quantity, vc.ColorName, vm.ModelName, vm.BasePrice, v.ManufactureYear
                        FROM Quotation q
                        JOIN Customer c ON q.CustomerID = c.CustomerID
                        JOIN Dealer d ON q.DealerID = d.DealerID
                        LEFT JOIN QuotationDetail qd ON q.QuotationID = qd.QuotationID
                        LEFT JOIN VehicleColor vc ON qd.ColorID = vc.ColorID
                        LEFT JOIN Vehicle v ON qd.VIN = v.VIN
                        LEFT JOIN VehicleModel vm ON v.ModelID = vm.ModelID
                        WHERE q.DealerID = ?
                        ORDER BY q.CreatedAt DESC
                    """;

            try (Connection conn = DBUtils.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, dealerID);

                try (ResultSet rs = ps.executeQuery()) {
                    int lastQuotationId = -1;

                    while (rs.next()) {
                        int quotationId = rs.getInt("QuotationID");

                        // If new quotation, create new object
                        if (quotationId != lastQuotationId) {
                            DTOQuotation quotation = new DTOQuotation();
                            quotation.setQuotationID(quotationId);
                            quotation.setCreatedAt(rs.getTimestamp("CreatedAt"));
                            quotation.setStatus(rs.getString("Status"));

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

                            // ✅ Initialize quotationDetails list
                            quotation.setQuotationDetails(new ArrayList<>());

                            quotations.add(quotation);
                            lastQuotationId = quotationId;
                        }

                        // Add vehicle and price information if available
                        if (rs.getString("VIN") != null) {
                            DTOQuotation quotation = quotations.get(quotations.size() - 1);

                            // Calculate total price (UnitPrice * Quantity)
                            BigDecimal unitPrice = rs.getBigDecimal("UnitPrice");
                            int quantity = rs.getInt("Quantity");
                            double totalPrice = unitPrice.doubleValue() * quantity;
                            quotation.setTotalPrice(totalPrice);

                            // Set vehicle info
                            DTOVehicle vehicle = new DTOVehicle();
                            vehicle.setVIN(rs.getString("VIN"));
                            vehicle.setModelName(rs.getString("ModelName"));
                            vehicle.setColorName(rs.getString("ColorName"));
                            vehicle.setManufactureYear(rs.getInt("ManufactureYear"));
                            vehicle.setBasePrice(rs.getBigDecimal("BasePrice"));
                            quotation.setVehicle(vehicle);

                            // ✅ Add QuotationDetail to list
                            DTOQuotationDetail detail = new DTOQuotationDetail();
                            detail.setVIN(rs.getString("VIN"));
                            detail.setUnitPrice(unitPrice);
                            detail.setQuantity(quantity);
                            detail.setColorName(rs.getString("ColorName"));
                            detail.setModelName(rs.getString("ModelName"));
                            quotation.getQuotationDetails().add(detail);
                        } else {
                            // ✅ If no QuotationDetail, create empty vehicle object
                            DTOQuotation quotation = quotations.get(quotations.size() - 1);
                            if (quotation.getVehicle() == null) {
                                DTOVehicle emptyVehicle = new DTOVehicle();
                                emptyVehicle.setVIN("N/A");
                                emptyVehicle.setModelName("N/A");
                                quotation.setVehicle(emptyVehicle);
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                log.error("Error fetching quotations by dealer dealerID={}", dealerID, e);
            }

            return quotations;
        }

        // 🔥 PRICE CALCULATION: FinalPrice = BasePrice × (1 - ManufacturerDiscount) × (1 - DealerDiscount)
        public BigDecimal calculateFinalPrice(String vin, int dealerID) {
            String sql = """
                        SELECT vm.BasePrice,
                               ISNULL(dlp.BaseDiscountPercent, 0) as BaseDiscountPercent,
                               ISNULL(dlp.BonusDiscountPercent, 0) as BonusDiscountPercent,
                               ISNULL(dpa.DiscountPercent, 0) as DealerDiscountPercent
                        FROM Vehicle v
                        JOIN VehicleModel vm ON v.ModelID = vm.ModelID
                        JOIN Dealer d ON d.DealerID = ?
                        LEFT JOIN DealerLevelPolicy dlp ON d.LevelID = dlp.LevelID
                        LEFT JOIN DealerPriceAdjustment dpa ON d.DealerID = dpa.DealerID AND v.ModelID = dpa.ModelID
                        WHERE v.VIN = ?
                    """;

            try (Connection conn = DBUtils.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, dealerID);
                ps.setString(2, vin);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        BigDecimal basePrice = rs.getBigDecimal("BasePrice");
                        BigDecimal baseDiscount = rs.getBigDecimal("BaseDiscountPercent");
                        BigDecimal bonusDiscount = rs.getBigDecimal("BonusDiscountPercent");
                        BigDecimal dealerDiscount = rs.getBigDecimal("DealerDiscountPercent");

                        // Calculate manufacturer discount (BaseDiscount + BonusDiscount)
                        BigDecimal manufacturerDiscount = baseDiscount.add(bonusDiscount);

                        // Apply formula: BasePrice × (1 - ManufacturerDiscount) × (1 - DealerDiscount)
                        BigDecimal finalPrice = basePrice
                                .multiply(BigDecimal.ONE.subtract(manufacturerDiscount.divide(new BigDecimal(100))))
                                .multiply(BigDecimal.ONE.subtract(dealerDiscount.divide(new BigDecimal(100))));

                        log.debug("Price calc VIN={} base={} manufacturerDiscount={} dealerDiscount={} final={}", vin, basePrice, manufacturerDiscount, dealerDiscount, finalPrice);

                        return finalPrice;
                    }
                }
            } catch (SQLException e) {
                log.error("Error calculating final price vin={} dealerID={}", vin, dealerID, e);
            }

            return BigDecimal.ZERO;
        }

        // 🔥 QUOTATION DETAIL MANAGEMENT: Insert QuotationDetail
        public boolean insertQuotationDetail(int quotationID, String vin, BigDecimal unitPrice, int quantity, int colorID) {
            String sql = """
                        INSERT INTO QuotationDetail (QuotationID, VIN, UnitPrice, Quantity, ColorID)
                        VALUES (?, ?, ?, ?, ?)
                    """;

            try (Connection conn = DBUtils.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, quotationID);
                ps.setString(2, vin);
                ps.setBigDecimal(3, unitPrice);
                ps.setInt(4, quantity);
                ps.setInt(5, colorID);

                int affectedRows = ps.executeUpdate();
                if (affectedRows > 0) {
                    log.info("QuotationDetail inserted quotationID={} vin={}", quotationID, vin);
                    return true;
                } else {
                    log.warn("No QuotationDetail inserted quotationID={} vin={}", quotationID, vin);
                    return false;
                }

            } catch (SQLException e) {
                log.error("Failed to insert QuotationDetail quotationID={} vin={}", quotationID, vin, e);
                return false;
            }
        }

        // 🔥 QUOTATION DETAIL MANAGEMENT: Get QuotationDetails by QuotationID
        public List<DTOQuotationDetail> getQuotationDetails(int quotationID) {
            List<DTOQuotationDetail> details = new ArrayList<>();

            String sql = """
                        SELECT qd.QuotationDetailID, qd.QuotationID, qd.VIN, qd.UnitPrice, qd.Quantity, qd.ColorID,
                               vc.ColorName, vm.ModelName
                        FROM QuotationDetail qd
                        LEFT JOIN VehicleColor vc ON qd.ColorID = vc.ColorID
                        LEFT JOIN Vehicle v ON qd.VIN = v.VIN
                        LEFT JOIN VehicleModel vm ON v.ModelID = vm.ModelID
                        WHERE qd.QuotationID = ?
                    """;

            try (Connection conn = DBUtils.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, quotationID);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        DTOQuotationDetail detail = new DTOQuotationDetail();
                        detail.setQuotationDetailID(rs.getInt("QuotationDetailID"));
                        detail.setQuotationID(rs.getInt("QuotationID"));
                        detail.setVIN(rs.getString("VIN"));
                        detail.setUnitPrice(rs.getBigDecimal("UnitPrice"));
                        detail.setQuantity(rs.getInt("Quantity"));
                        detail.setColorID(rs.getInt("ColorID"));
                        detail.setColorName(rs.getString("ColorName"));
                        detail.setModelName(rs.getString("ModelName"));
                        details.add(detail);
                    }
                }
            } catch (SQLException e) {
                log.error("Error fetching quotation details quotationID={}", quotationID, e);
            }

            return details;
        }
    }
