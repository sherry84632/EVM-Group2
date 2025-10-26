package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;

import java.sql.*;
import java.util.*;
import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DAOSaleOrder {

    private static final Logger log = LoggerFactory.getLogger(DAOSaleOrder.class);

    // ======================================================
    // 1️⃣  TẠO SALE ORDER MỚI
    // ======================================================
    public boolean createSaleOrder(DTOSaleOrder saleOrder) {
        String sqlOrder = "INSERT INTO SaleOrder (customer_customer_id, dealer_dealer_id, staff_staff_id, CreatedAt, Status, Quantity, TotalAmount) "
                + "VALUES (?, ?, ?, GETDATE(), ?, ?, ?)";
        String sqlDetail = "INSERT INTO SaleOrderDetail (SaleOrderID, VIN, Price, PolicyID) "
                + "VALUES (?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement psOrder = null;
        PreparedStatement psDetail = null;
        ResultSet rs = null;

        try {
            conn = DBUtils.getConnection();
            conn.setAutoCommit(false); // ⚙️ Transaction start
            log.debug("Creating SaleOrder for customerId={} dealerId={} staffId={}",
                    saleOrder.getCustomer().getCustomerID(),
                    saleOrder.getDealer().getDealerID(),
                    saleOrder.getStaff().getStaffID());

            // === Insert SaleOrder ===
            psOrder = conn.prepareStatement(sqlOrder, Statement.RETURN_GENERATED_KEYS);
            psOrder.setInt(1, saleOrder.getCustomer().getCustomerID());
            psOrder.setInt(2, saleOrder.getDealer().getDealerID());
            psOrder.setInt(3, saleOrder.getStaff().getStaffID());
            psOrder.setString(4, saleOrder.getStatus().toString());
            psOrder.setInt(5, saleOrder.getTotalQuantity());
            psOrder.setBigDecimal(6, saleOrder.getTotalAmount());

            psOrder.executeUpdate();
            log.trace("Inserted SaleOrder main row");

            // === Get generated SaleOrderID ===
            rs = psOrder.getGeneratedKeys();
            int saleOrderID = 0;
            if (rs.next()) {
                saleOrderID = rs.getInt(1);
                log.info("Generated SaleOrderID={}", saleOrderID);
            }

            // Update aggregated fields on DTO for downstream view usage
            saleOrder.setSaleOrderID(saleOrderID);

            // === Insert SaleOrderDetails ===
            psDetail = conn.prepareStatement(sqlDetail);
            for (DTOSaleOrderDetail detail : saleOrder.getDetail()) {
                psDetail.setInt(1, saleOrderID);
                psDetail.setString(2, detail.getVehicle().getVIN());
                psDetail.setBigDecimal(3, detail.getPrice());
                psDetail.setInt(4, detail.getDiscountPolicy().getPolicyID());
                psDetail.addBatch();
                log.trace("Queued SaleOrderDetail VIN={} price={}", detail.getVehicle().getVIN(), detail.getPrice());
            }
            psDetail.executeBatch();
            log.debug("Inserted {} SaleOrderDetail rows", saleOrder.getDetail().size());

            conn.commit();
            log.info("SaleOrder committed id={}", saleOrderID);
            return true;

        } catch (SQLException e) {
            log.error("Error creating SaleOrder - performing rollback", e);
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                log.error("Rollback failed", ex);
            }
        } finally {
            try {
                if (rs != null) rs.close();
                if (psOrder != null) psOrder.close();
                if (psDetail != null) psDetail.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                log.error("Error closing resources", ex);
            }
        }
        return false;
    }


    // ======================================================
    // 2️⃣  LẤY TOÀN BỘ SALE ORDERS
    // ======================================================
    public List<DTOSaleOrder> getAllSaleOrders() {
        List<DTOSaleOrder> list = new ArrayList<>();

        String sql = """
                    SELECT so.SaleOrderID, so.CreatedAt, so.Status, so.TotalAmount, so.Quantity,
                           c.CustomerID, c.FullName AS CustomerName,
                           d.DealerID, d.DealerName,
                           s.StaffID, s.FullName AS StaffName
                    FROM SaleOrder so
                    JOIN Customer c ON so.customer_customer_id = c.CustomerID
                    JOIN Dealer d ON so.dealer_dealer_id = d.DealerID
                    JOIN DealerStaff s ON so.staff_staff_id = s.StaffID
                    ORDER BY so.SaleOrderID DESC
                """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                DTOSaleOrder order = new DTOSaleOrder();
                order.setSaleOrderID(rs.getInt("SaleOrderID"));
                order.setCreatedAt(rs.getTimestamp("CreatedAt"));
                order.setStatus(SaleOrderStatus.valueOf(rs.getString("Status")));
                order.setTotalAmount(rs.getBigDecimal("TotalAmount"));
                order.setTotalQuantity(rs.getInt("Quantity"));

                // Customer
                DTOCustomer customer = new DTOCustomer();
                customer.setCustomerID(rs.getInt("CustomerID"));
                customer.setFullName(rs.getString("CustomerName"));
                order.setCustomer(customer);

                // Dealer
                DTODealer dealer = new DTODealer();
                dealer.setDealerID(rs.getInt("DealerID"));
                dealer.setDealerName(rs.getString("DealerName"));
                order.setDealer(dealer);

                // Staff
                DTODealerStaff staff = new DTODealerStaff();
                staff.setStaffID(rs.getInt("StaffID"));
                staff.setFullName(rs.getString("StaffName"));
                order.setStaff(staff);

                // Total
                order.setDetail(getSaleOrderDetails(order.getSaleOrderID()));
                list.add(order);
            }

        } catch (SQLException e) {
            log.error("Error retrieving all SaleOrders", e);
        }
        return list;
    }

    // ======================================================
    // 3️⃣  LẤY SALE ORDER THEO ID
    // ======================================================
    public DTOSaleOrder getSaleOrderById(int id) {
        DTOSaleOrder order = null;
        String sql = """
                    SELECT so.SaleOrderID, so.CreatedAt, so.Status, so.TotalAmount, so.Quantity,
                           c.CustomerID, c.FullName AS CustomerName,
                           d.DealerID, d.DealerName,
                           s.StaffID, s.FullName AS StaffName
                    FROM SaleOrder so
                    JOIN Customer c ON so.customer_customer_id = c.CustomerID
                    JOIN Dealer d ON so.dealer_dealer_id = d.DealerID
                    JOIN DealerStaff s ON so.staff_staff_id = s.StaffID
                    WHERE so.SaleOrderID = ?
                """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                order = new DTOSaleOrder();
                order.setSaleOrderID(rs.getInt("SaleOrderID"));
                order.setCreatedAt(rs.getTimestamp("CreatedAt"));
                order.setStatus(SaleOrderStatus.valueOf(rs.getString("Status")));
                order.setTotalAmount(rs.getBigDecimal("TotalAmount"));
                order.setTotalQuantity(rs.getInt("Quantity"));

                DTOCustomer c = new DTOCustomer();
                c.setCustomerID(rs.getInt("CustomerID"));
                c.setFullName(rs.getString("CustomerName"));
                order.setCustomer(c);

                DTODealer d = new DTODealer();
                d.setDealerID(rs.getInt("DealerID"));
                d.setDealerName(rs.getString("DealerName"));
                order.setDealer(d);

                DTODealerStaff s = new DTODealerStaff();
                s.setStaffID(rs.getInt("StaffID"));
                s.setFullName(rs.getString("StaffName"));
                order.setStaff(s);

                order.setDetail(getSaleOrderDetails(id));
            }

        } catch (SQLException e) {
            log.error("Error retrieving SaleOrder by id={}", id, e);
        }
        return order;
    }

    // ======================================================
    // 4️⃣  LẤY CHI TIẾT ĐƠN HÀNG
    // ======================================================
    public List<DTOSaleOrderDetail> getSaleOrderDetails(int saleOrderID) {
        List<DTOSaleOrderDetail> details = new ArrayList<>();

        String sql = """
                    SELECT sod.SODetailID, sod.SaleOrderID, sod.VIN, sod.Price, sod.PolicyID,
                           v.ManufactureYear, v.Status,
                           vc.ColorID, vc.ColorName,
                           vv.VersionID, vv.VersionName,
                           dp.PolicyID, dp.PolicyName
                    FROM SaleOrderDetail sod
                    JOIN Vehicle v ON sod.VIN = v.VIN
                    LEFT JOIN VehicleColor vc ON v.ColorID = vc.ColorID
                    LEFT JOIN VehicleVersion vv ON v.VersionID = vv.VersionID
                    LEFT JOIN DiscountPolicy dp ON sod.PolicyID = dp.PolicyID
                    WHERE sod.SaleOrderID = ?
                """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, saleOrderID);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                // Vehicle info
                DTOVehicle vehicle = new DTOVehicle();
                vehicle.setVIN(rs.getString("VIN"));
                vehicle.setManufactureYear(rs.getInt("ManufactureYear"));
                vehicle.setStatus(VehicleStatus.valueOf(rs.getString("Status")));

                // Color info
                if (rs.getString("ColorName") != null) {
                    DTOVehicleColor color = new DTOVehicleColor();
                    color.setColorID(rs.getInt("ColorID"));
                    color.setColorName(rs.getString("ColorName"));
                    vehicle.setColor(color);
                }

                // Version info
                if (rs.getString("VersionName") != null) {
                    DTOVehicleVersion version = new DTOVehicleVersion();
                    version.setVersionID(rs.getInt("VersionID"));
                    version.setVersionName(rs.getString("VersionName"));
                    vehicle.setVersion(version);
                }

                // Discount Policy info
                DTODiscountPolicy discountPolicy = null;
                if (rs.getString("PolicyName") != null) {
                    discountPolicy = new DTODiscountPolicy();
                    discountPolicy.setPolicyID(rs.getInt("PolicyID"));
                    discountPolicy.setPolicyName(rs.getString("PolicyName"));
                }

                DTOSaleOrderDetail detail = new DTOSaleOrderDetail();
                detail.setSoDetailID(rs.getInt("SODetailID"));
                detail.setVehicle(vehicle);
                detail.setPrice(rs.getBigDecimal("Price"));
                detail.setDiscountPolicy(discountPolicy);

                details.add(detail);
            }

        } catch (SQLException e) {
            log.error("Error retrieving SaleOrder details saleOrderID={}", saleOrderID, e);
        }
        return details;
    }

    // ======================================================
// 5️⃣  LẤY 1 CHI TIẾT SALE ORDER DETAIL THEO ID
// ======================================================
    public DTOSaleOrderDetail getDetailById(int detailId) {
        DTOSaleOrderDetail detail = null;
        String sql = """
                    SELECT sod.SODetailID, sod.SaleOrderID, sod.VIN, sod.Price, sod.PolicyID,
                           v.ManufactureYear, v.Status,
                           vc.ColorID, vc.ColorName,
                           vv.VersionID, vv.VersionName,
                           dp.PolicyID, dp.PolicyName
                    FROM SaleOrderDetail sod
                    JOIN Vehicle v ON sod.VIN = v.VIN
                    LEFT JOIN VehicleColor vc ON v.ColorID = vc.ColorID
                    LEFT JOIN VehicleVersion vv ON v.VersionID = vv.VersionID
                    LEFT JOIN DiscountPolicy dp ON sod.PolicyID = dp.PolicyID
                    WHERE sod.SODetailID = ?
                """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, detailId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // === Vehicle Info ===
                    DTOVehicle vehicle = new DTOVehicle();
                    vehicle.setVIN(rs.getString("VIN"));
                    vehicle.setManufactureYear(rs.getInt("ManufactureYear"));
                    vehicle.setStatus(VehicleStatus.valueOf(rs.getString("Status")));

                    // Color info
                    if (rs.getString("ColorName") != null) {
                        DTOVehicleColor color = new DTOVehicleColor();
                        color.setColorID(rs.getInt("ColorID"));
                        color.setColorName(rs.getString("ColorName"));
                        vehicle.setColor(color);
                    }

                    // Version info
                    if (rs.getString("VersionName") != null) {
                        DTOVehicleVersion version = new DTOVehicleVersion();
                        version.setVersionID(rs.getInt("VersionID"));
                        version.setVersionName(rs.getString("VersionName"));
                        vehicle.setVersion(version);
                    }

                    // Discount Policy info
                    DTODiscountPolicy discountPolicy = null;
                    if (rs.getString("PolicyName") != null) {
                        discountPolicy = new DTODiscountPolicy();
                        discountPolicy.setPolicyID(rs.getInt("PolicyID"));
                        discountPolicy.setPolicyName(rs.getString("PolicyName"));
                    }

                    // === SaleOrderDetail ===
                    detail = new DTOSaleOrderDetail();
                    detail.setSoDetailID(rs.getInt("SODetailID"));
                    detail.setVehicle(vehicle);
                    detail.setPrice(rs.getBigDecimal("Price"));
                    detail.setDiscountPolicy(discountPolicy);
                }
            }

        } catch (SQLException e) {
            log.error("Error retrieving SaleOrderDetail by id={}", detailId, e);
        }

        return detail;
    }

    // ======================================================
// 🟢 UPDATE CHỈ STATUS CỦA SALE ORDER
// ======================================================
    public boolean updateSaleOrderStatus(int saleOrderID, String status) {
        String sql = "UPDATE SaleOrder SET Status = ? WHERE SaleOrderID = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.toString());
            ps.setInt(2, saleOrderID);
            int rows = ps.executeUpdate();

            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

}
