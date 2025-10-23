package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;
import org.springframework.stereotype.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.math.BigDecimal;
import java.util.List;

@Repository
public class DAOSaleOrder {

    private static final Logger log = LoggerFactory.getLogger(DAOSaleOrder.class);
    private final DAOQuotation daoQuotation; // để truy xuất dữ liệu quotation

    public DAOSaleOrder(DAOQuotation daoQuotation) {
        this.daoQuotation = daoQuotation;
    }
    // ✅ Tạo SaleOrder mới dựa vào Quotation đã được duyệt
    public int createSaleOrderFromQuotation(int quotationID) {
        log.info("Creating SaleOrder from QuotationID={}", quotationID);

        // ✅ FIX: Check if quotation is approved
        if (!daoQuotation.isQuotationApproved(quotationID)) {
            log.warn("QuotationID={} chưa được duyệt -> không thể tạo SaleOrder", quotationID);
            return -1;
        }

        // ✅ FIX: Check if quotation has already been converted (prevent duplicate orders)
        if (daoQuotation.isQuotationConverted(quotationID)) {
            log.warn("QuotationID={} đã được chuyển thành SaleOrder -> không thể tạo lại", quotationID);
            return -2; // Return -2 to indicate "already converted"
        }

        DTOQuotation quotation = daoQuotation.getQuotationById(quotationID);
        if (quotation == null) {
            log.error("Không tìm thấy QuotationID={}", quotationID);
            return -1;
        }

        List<DTOQuotationDetail> details = daoQuotation.getQuotationDetails(quotationID);
        if (details == null || details.isEmpty()) {
            log.error("QuotationID={} không có chi tiết", quotationID);
            return -1;
        }

        String insertOrderSQL = """
                INSERT INTO SaleOrder (QuotationID, CustomerID, DealerID, StaffID, CreatedAt, Status, TotalQuantity, TotalAmount)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        String insertDetailSQL = """
                INSERT INTO SaleOrderDetail (SaleOrderID, VIN, Quantity, Price, QuotationID, ColorID)
                VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DBUtils.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement psOrder = conn.prepareStatement(insertOrderSQL, Statement.RETURN_GENERATED_KEYS)) {

                // ✅ Tổng số lượng và tổng tiền
                int totalQuantity = details.stream().mapToInt(DTOQuotationDetail::getQuantity).sum();
                BigDecimal totalAmount = details.stream()
                        .map(d -> d.getUnitPrice().multiply(BigDecimal.valueOf(d.getQuantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                // ✅ FIX: Insert with QuotationID and proper StaffID handling
                psOrder.setInt(1, quotationID); // QuotationID
                psOrder.setInt(2, quotation.getCustomer().getCustomerID());
                psOrder.setInt(3, quotation.getDealer().getDealerID());

                // ✅ FIX: Use NULL for StaffID if not available (from Quotation's staff)
                if (quotation.getStaff() != null && quotation.getStaff().getStaffID() > 0) {
                    psOrder.setInt(4, quotation.getStaff().getStaffID());
                } else {
                    psOrder.setNull(4, java.sql.Types.INTEGER);
                }

                psOrder.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
                psOrder.setString(6, "Pending");
                psOrder.setInt(7, totalQuantity);
                psOrder.setBigDecimal(8, totalAmount);

                psOrder.executeUpdate();

                int saleOrderID;
                try (ResultSet rs = psOrder.getGeneratedKeys()) {
                    if (rs.next()) {
                        saleOrderID = rs.getInt(1);
                        log.info("SaleOrder inserted ID={}", saleOrderID);
                    } else {
                        throw new SQLException("Không lấy được SaleOrderID sau khi insert.");
                    }
                }

                // ✅ Thêm các SaleOrderDetail
                try (PreparedStatement psDetail = conn.prepareStatement(insertDetailSQL)) {
                    for (DTOQuotationDetail d : details) {
                        psDetail.setInt(1, saleOrderID);
                        psDetail.setString(2, d.getVIN());
                        psDetail.setInt(3, d.getQuantity());
                        psDetail.setBigDecimal(4, d.getUnitPrice());
                        psDetail.setInt(5, quotationID);
                        psDetail.setInt(6, d.getColorID());
                        psDetail.addBatch();
                    }
                    psDetail.executeBatch();
                }

                // ✅ FIX: Mark quotation as converted to prevent duplicate orders
                if (!daoQuotation.markQuotationAsConverted(quotationID)) {
                    log.warn("Failed to mark quotation as converted, but SaleOrder created id={}", saleOrderID);
                }

                conn.commit();
                log.info("SaleOrder created successfully from QuotationID={} -> SaleOrderID={}", quotationID, saleOrderID);
                return saleOrderID;

            } catch (SQLException e) {
                conn.rollback();
                log.error("Tạo SaleOrder thất bại, rollback QuotationID={}", quotationID, e);
                return -1;
            }

        } catch (SQLException e) {
            log.error("Database error khi tạo SaleOrder từ QuotationID={}", quotationID, e);
            return -1;
        }
    }

    // ✅ Lấy tất cả SaleOrder
    public List<DTOSaleOrder> getAllSaleOrders() {
        List<DTOSaleOrder> list = new java.util.ArrayList<>();

        String sql = """
                SELECT s.SaleOrderID, s.QuotationID, s.CustomerID, s.DealerID, s.StaffID,
                       s.CreatedAt, s.Status, s.TotalQuantity, s.TotalAmount,
                       c.FullName AS CustomerName, d.DealerName
                FROM SaleOrder s
                JOIN Customer c ON s.CustomerID = c.CustomerID
                JOIN Dealer d ON s.DealerID = d.DealerID
                ORDER BY s.CreatedAt DESC
            """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                DTOSaleOrder order = new DTOSaleOrder();
                order.setSaleOrderID(rs.getInt("SaleOrderID"));
                order.setQuotationID(rs.getInt("QuotationID")); // ✅ FIX: Add QuotationID
                order.setCreatedAt(rs.getTimestamp("CreatedAt"));
                order.setStatus(rs.getString("Status"));
                order.setTotalQuantity(rs.getInt("TotalQuantity"));
                order.setTotalAmount(rs.getBigDecimal("TotalAmount"));

                DTOCustomer customer = new DTOCustomer();
                customer.setCustomerID(rs.getInt("CustomerID"));
                customer.setFullName(rs.getString("CustomerName"));
                order.setCustomer(customer);

                DTODealer dealer = new DTODealer();
                dealer.setDealerID(rs.getInt("DealerID"));
                dealer.setDealerName(rs.getString("DealerName"));
                order.setDealer(dealer);

                // ✅ FIX: Set staff properly
                DTODealerStaff staff = new DTODealerStaff();
                int staffId = rs.getInt("StaffID");
                if (!rs.wasNull()) {
                    staff.setStaffID(staffId);
                }
                order.setStaff(staff);

                list.add(order);
            }

        } catch (SQLException e) {
            log.error("Error fetching SaleOrders", e);
        }

        return list;
    }

    // ✅ Lấy SaleOrder theo ID với đầy đủ thông tin
    public DTOSaleOrder getSaleOrderById(int saleOrderID) {
        log.debug("Getting SaleOrder by ID={}", saleOrderID);

        String sql = """
                SELECT s.SaleOrderID, s.QuotationID, s.CustomerID, s.DealerID, s.StaffID,
                       s.CreatedAt, s.Status, s.TotalQuantity, s.TotalAmount,
                       c.FullName AS CustomerName, c.Email AS CustomerEmail, c.Phone AS CustomerPhone,
                       d.DealerName, d.Email AS DealerEmail, d.Phone AS DealerPhone
                FROM SaleOrder s
                JOIN Customer c ON s.CustomerID = c.CustomerID
                JOIN Dealer d ON s.DealerID = d.DealerID
                WHERE s.SaleOrderID = ?
            """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, saleOrderID);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DTOSaleOrder order = new DTOSaleOrder();
                    order.setSaleOrderID(rs.getInt("SaleOrderID"));
                    order.setQuotationID(rs.getInt("QuotationID"));
                    order.setCreatedAt(rs.getTimestamp("CreatedAt"));
                    order.setStatus(rs.getString("Status"));
                    order.setTotalQuantity(rs.getInt("TotalQuantity"));
                    order.setTotalAmount(rs.getBigDecimal("TotalAmount"));

                    // Customer info
                    DTOCustomer customer = new DTOCustomer();
                    customer.setCustomerID(rs.getInt("CustomerID"));
                    customer.setFullName(rs.getString("CustomerName"));
                    customer.setEmail(rs.getString("CustomerEmail"));
                    customer.setPhone(rs.getString("CustomerPhone"));
                    order.setCustomer(customer);

                    // Dealer info
                    DTODealer dealer = new DTODealer();
                    dealer.setDealerID(rs.getInt("DealerID"));
                    dealer.setDealerName(rs.getString("DealerName"));
                    dealer.setEmail(rs.getString("DealerEmail"));
                    dealer.setPhone(rs.getString("DealerPhone"));
                    order.setDealer(dealer);

                    // Staff info
                    DTODealerStaff staff = new DTODealerStaff();
                    int staffId = rs.getInt("StaffID");
                    if (!rs.wasNull()) {
                        staff.setStaffID(staffId);
                    }
                    order.setStaff(staff);

                    // ✅ Load order details
                    List<DTOSaleOrderDetail> details = getSaleOrderDetails(saleOrderID);
                    order.setDetails(details);

                    log.info("SaleOrder loaded successfully ID={}", saleOrderID);
                    return order;
                }
            }
        } catch (SQLException e) {
            log.error("Error fetching SaleOrder ID={}", saleOrderID, e);
        }

        return null;
    }

    // ✅ Lấy danh sách SaleOrderDetail theo SaleOrderID
    public List<DTOSaleOrderDetail> getSaleOrderDetails(int saleOrderID) {
        log.debug("Getting SaleOrderDetails for SaleOrderID={}", saleOrderID);

        List<DTOSaleOrderDetail> details = new java.util.ArrayList<>();

        String sql = """
                SELECT sod.SaleOrderDetailID, sod.SaleOrderID, sod.VIN, sod.Quantity, sod.Price,
                       sod.QuotationID, sod.ColorID,
                       vc.ColorName, vm.ModelName, v.ManufactureYear
                FROM SaleOrderDetail sod
                LEFT JOIN VehicleColor vc ON sod.ColorID = vc.ColorID
                LEFT JOIN Vehicle v ON sod.VIN = v.VIN
                LEFT JOIN VehicleModel vm ON v.ModelID = vm.ModelID
                WHERE sod.SaleOrderID = ?
            """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, saleOrderID);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DTOSaleOrderDetail detail = new DTOSaleOrderDetail();
                    detail.setSaleOrderDetailID(rs.getInt("SaleOrderDetailID"));
                    detail.setSaleOrderID(rs.getInt("SaleOrderID"));
                    detail.setVIN(rs.getString("VIN"));
                    detail.setQuantity(rs.getInt("Quantity"));
                    detail.setPrice(rs.getBigDecimal("Price"));
                    detail.setQuotationID(rs.getInt("QuotationID"));
                    detail.setColorID(rs.getInt("ColorID"));
                    detail.setColorName(rs.getString("ColorName"));
                    detail.setModelName(rs.getString("ModelName"));
                    detail.setManufactureYear(rs.getInt("ManufactureYear"));

                    details.add(detail);
                }
            }

            log.debug("Loaded {} details for SaleOrderID={}", details.size(), saleOrderID);

        } catch (SQLException e) {
            log.error("Error fetching SaleOrderDetails for SaleOrderID={}", saleOrderID, e);
        }

        return details;
    }

    // ✅ Lấy SaleOrder theo QuotationID (để kiểm tra quotation đã convert chưa)
    public DTOSaleOrder getSaleOrderByQuotationId(int quotationID) {
        log.debug("Getting SaleOrder by QuotationID={}", quotationID);

        String sql = """
                SELECT s.SaleOrderID, s.QuotationID, s.CustomerID, s.DealerID, s.StaffID,
                       s.CreatedAt, s.Status, s.TotalQuantity, s.TotalAmount
                FROM SaleOrder s
                WHERE s.QuotationID = ?
            """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, quotationID);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DTOSaleOrder order = new DTOSaleOrder();
                    order.setSaleOrderID(rs.getInt("SaleOrderID"));
                    order.setQuotationID(rs.getInt("QuotationID"));
                    order.setCreatedAt(rs.getTimestamp("CreatedAt"));
                    order.setStatus(rs.getString("Status"));
                    order.setTotalQuantity(rs.getInt("TotalQuantity"));
                    order.setTotalAmount(rs.getBigDecimal("TotalAmount"));

                    log.info("Found SaleOrder ID={} for QuotationID={}", order.getSaleOrderID(), quotationID);
                    return order;
                }
            }
        } catch (SQLException e) {
            log.error("Error fetching SaleOrder by QuotationID={}", quotationID, e);
        }

        return null;
    }
}
