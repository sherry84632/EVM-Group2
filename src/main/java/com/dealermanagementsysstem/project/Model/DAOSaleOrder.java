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
    private final DAOQuotation daoQuotation = new DAOQuotation(); // để truy xuất dữ liệu quotation

    // ✅ Tạo SaleOrder mới dựa vào Quotation đã được duyệt
    public int createSaleOrderFromQuotation(int quotationID) {
        log.info("Creating SaleOrder from QuotationID={}", quotationID);

        if (!daoQuotation.isQuotationApproved(quotationID)) {
            log.warn("QuotationID={} chưa được duyệt -> không thể tạo SaleOrder", quotationID);
            return -1;
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
                INSERT INTO SaleOrder (CustomerID, DealerID, StaffID, CreatedAt, Status, TotalQuantity, TotalAmount)
                VALUES (?, ?, ?, ?, ?, ?, ?)
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

                psOrder.setInt(1, quotation.getCustomer().getCustomerID());
                psOrder.setInt(2, quotation.getDealer().getDealerID());
                psOrder.setInt(3, quotation.getDealer().getDealerID()); // tạm lấy dealer làm staff nếu chưa có staff
                psOrder.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                psOrder.setString(5, "Pending");
                psOrder.setInt(6, totalQuantity);
                psOrder.setBigDecimal(7, totalAmount);

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
                SELECT s.SaleOrderID, s.CustomerID, s.DealerID, s.StaffID, s.CreatedAt, s.Status, s.TotalQuantity, s.TotalAmount,
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
                order.getCustomer().setCustomerID(rs.getInt("CustomerID"));
                order.getDealer().setDealerID(rs.getInt("DealerID"));
                order.getStaff().setStaffID(rs.getInt("StaffID"));
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

                list.add(order);
            }

        } catch (SQLException e) {
            log.error("Error fetching SaleOrders", e);
        }

        return list;
    }
}
