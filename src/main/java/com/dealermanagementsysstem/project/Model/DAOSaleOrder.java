package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;

import java.sql.*;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DAOSaleOrder {

    private static final Logger log = LoggerFactory.getLogger(DAOSaleOrder.class);

    // ======================================================
    // 1️⃣  TẠO SALE ORDER MỚI
    // ======================================================
    public boolean createSaleOrder(DTOSaleOrder saleOrder) {
        String sqlOrder = "INSERT INTO SaleOrder (customer_customer_id, dealer_dealer_id, staff_staff_id, CreatedAt, Status, Quantity, TotalAmount, PlannedDeliveryDate, ActualDeliveryDate, EtaDays) VALUES (?, ?, ?, GETDATE(), ?, ?, ?, ?, ?, ?)";
        String sqlDetail = "INSERT INTO SaleOrderDetail (SaleOrderID, VehicleID, Price, PolicyID) VALUES (?, ?, ?, ?)";
        Connection conn = null; PreparedStatement psOrder = null; PreparedStatement psDetail = null; ResultSet rs = null;
        try {
            if (saleOrder.getDetail()==null || saleOrder.getDetail().isEmpty()) {
                log.warn("createSaleOrder invoked with empty detail list - abort");
                return false;
            }
            // Recompute header totals if not set or zero
            int totalQty = saleOrder.getTotalQuantity();
            java.math.BigDecimal totalAmt = saleOrder.getTotalAmount();
            if (totalQty <= 0 || totalAmt == null || totalAmt.compareTo(java.math.BigDecimal.ZERO) == 0) {
                totalQty = 0; totalAmt = java.math.BigDecimal.ZERO;
                for (DTOSaleOrderDetail d : saleOrder.getDetail()) {
                    int q = d.getQuantity()!=null? d.getQuantity():1; totalQty += q;
                    java.math.BigDecimal price = d.getPrice()!=null? d.getPrice(): java.math.BigDecimal.ZERO;
                    totalAmt = totalAmt.add(price.multiply(java.math.BigDecimal.valueOf(q)));
                }
                saleOrder.setTotalQuantity(totalQty);
                saleOrder.setTotalAmount(totalAmt);
            }
            conn = DBUtils.getConnection();
            conn.setAutoCommit(false);
            log.debug("[SaleOrder] Inserting header cust={} dealer={} staff={} qty={} amount={} status={}",
                    saleOrder.getCustomer().getCustomerID(), saleOrder.getDealer().getDealerID(), saleOrder.getStaff().getStaffID(),
                    saleOrder.getTotalQuantity(), saleOrder.getTotalAmount(), saleOrder.getStatus());
            psOrder = conn.prepareStatement(sqlOrder, Statement.RETURN_GENERATED_KEYS);
            psOrder.setInt(1, saleOrder.getCustomer().getCustomerID());
            psOrder.setInt(2, saleOrder.getDealer().getDealerID());
            psOrder.setInt(3, saleOrder.getStaff().getStaffID());
            psOrder.setString(4, saleOrder.getStatus().toString());
            psOrder.setInt(5, saleOrder.getTotalQuantity());
            psOrder.setBigDecimal(6, saleOrder.getTotalAmount());
            psOrder.setTimestamp(7, saleOrder.getPlannedDeliveryDate());
            psOrder.setTimestamp(8, saleOrder.getActualDeliveryDate());
            if (saleOrder.getEtaDays()!=null) psOrder.setInt(9, saleOrder.getEtaDays()); else psOrder.setNull(9, java.sql.Types.INTEGER);
            psOrder.executeUpdate();
            rs = psOrder.getGeneratedKeys(); int saleOrderID = 0; if (rs.next()) saleOrderID = rs.getInt(1); saleOrder.setSaleOrderID(saleOrderID);
            log.info("[SaleOrder] Header inserted id={}", saleOrderID);
            psDetail = conn.prepareStatement(sqlDetail);
            int detailCount = 0;
            for (DTOSaleOrderDetail d : saleOrder.getDetail()) {
                if (d.getVehicle()==null) { log.warn("[SaleOrder] Skipping detail without vehicle"); continue; }
                psDetail.setInt(1, saleOrderID);
                psDetail.setInt(2, d.getVehicle().getVehicleID());
                psDetail.setBigDecimal(3, d.getPrice()!=null? d.getPrice(): java.math.BigDecimal.ZERO);
                if (d.getDiscountPolicy()!=null) {
                    psDetail.setInt(4, d.getDiscountPolicy().getPolicyID());
                } else {
                    psDetail.setNull(4, java.sql.Types.INTEGER);
                }
                psDetail.addBatch(); detailCount++;
            }
            if (detailCount==0) { log.error("[SaleOrder] No valid details to insert, rolling back"); conn.rollback(); return false; }
            int[] res = psDetail.executeBatch();
            log.debug("[SaleOrder] Inserted {} detail rows", res.length);
            conn.commit();
            return true;
        } catch (SQLException e) {
            log.error("[SaleOrder] Error creating sale order - rollback", e);
            try { if (conn!=null) conn.rollback(); } catch (SQLException ex) { log.error("[SaleOrder] Rollback failed", ex);}
        } finally {
            try { if (rs!=null) rs.close(); if (psOrder!=null) psOrder.close(); if (psDetail!=null) psDetail.close(); if (conn!=null) conn.close(); } catch (SQLException ex) { log.error("[SaleOrder] Close resource error", ex);}
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
                           so.PlannedDeliveryDate, so.ActualDeliveryDate, so.EtaDays,
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

                // Delivery info
                order.setPlannedDeliveryDate(rs.getTimestamp("PlannedDeliveryDate"));
                order.setActualDeliveryDate(rs.getTimestamp("ActualDeliveryDate"));
                order.setEtaDays((Integer) rs.getObject("EtaDays"));

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
                           so.PlannedDeliveryDate, so.ActualDeliveryDate, so.EtaDays,
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

                // Delivery info
                order.setPlannedDeliveryDate(rs.getTimestamp("PlannedDeliveryDate"));
                order.setActualDeliveryDate(rs.getTimestamp("ActualDeliveryDate"));
                order.setEtaDays((Integer) rs.getObject("EtaDays"));

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
                    SELECT sod.SODetailID, sod.SaleOrderID, sod.VehicleID, sod.Price, sod.PolicyID,
                           v.ManufactureYear, v.Status,
                           vc.ColorID, vc.ColorName,
                           vv.VersionID, vv.VersionName,
                           vm.ModelID, vm.ModelName, vm.BasePrice AS ModelBasePrice,
                           dp.PolicyID, dp.PolicyName
                    FROM SaleOrderDetail sod
                    JOIN Vehicle v ON sod.VehicleID = v.VehicleID
                    LEFT JOIN VehicleColor vc ON v.ColorID = vc.ColorID
                    LEFT JOIN VehicleVersion vv ON v.VersionID = vv.VersionID
                    LEFT JOIN VehicleModel vm ON vv.ModelID = vm.ModelID
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
                vehicle.setVehicleID(rs.getInt("VehicleID"));
                vehicle.setManufactureYear(rs.getInt("ManufactureYear"));
                vehicle.setStatus(VehicleStatus.valueOf(rs.getString("Status")));

                // Color info
                if (rs.getString("ColorName") != null) {
                    DTOVehicleColor color = new DTOVehicleColor();
                    color.setColorID(rs.getInt("ColorID"));
                    color.setColorName(rs.getString("ColorName"));
                    vehicle.setColor(color);
                }

                // Version & Model info
                if (rs.getString("VersionName") != null) {
                    DTOVehicleVersion version = new DTOVehicleVersion();
                    version.setVersionID(rs.getInt("VersionID"));
                    version.setVersionName(rs.getString("VersionName"));
                    if (rs.getString("ModelName") != null) {
                        DTOVehicleModel model = new DTOVehicleModel();
                        model.setModelID(rs.getInt("ModelID"));
                        model.setModelName(rs.getString("ModelName"));
                        model.setBasePrice(rs.getBigDecimal("ModelBasePrice"));
                        version.setModel(model);
                    }
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
                    SELECT sod.SODetailID, sod.SaleOrderID, sod.VehicleID, sod.Price, sod.PolicyID,
                           v.ManufactureYear, v.Status,
                           vc.ColorID, vc.ColorName,
                           vv.VersionID, vv.VersionName,
                           vm.ModelID, vm.ModelName, vm.BasePrice AS ModelBasePrice,
                           dp.PolicyID, dp.PolicyName
                    FROM SaleOrderDetail sod
                    JOIN Vehicle v ON sod.VehicleID = v.VehicleID
                    LEFT JOIN VehicleColor vc ON v.ColorID = vc.ColorID
                    LEFT JOIN VehicleVersion vv ON v.VersionID = vv.VersionID
                    LEFT JOIN VehicleModel vm ON vv.ModelID = vm.ModelID
                    LEFT JOIN DiscountPolicy dp ON sod.PolicyID = dp.PolicyID
                    WHERE sod.SODetailID = ?
                """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, detailId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DTOVehicle vehicle = new DTOVehicle();
                    vehicle.setVehicleID(rs.getInt("VehicleID"));
                    vehicle.setManufactureYear(rs.getInt("ManufactureYear"));
                    vehicle.setStatus(VehicleStatus.valueOf(rs.getString("Status")));

                    if (rs.getString("ColorName") != null) {
                        DTOVehicleColor color = new DTOVehicleColor();
                        color.setColorID(rs.getInt("ColorID"));
                        color.setColorName(rs.getString("ColorName"));
                        vehicle.setColor(color);
                    }

                    if (rs.getString("VersionName") != null) {
                        DTOVehicleVersion version = new DTOVehicleVersion();
                        version.setVersionID(rs.getInt("VersionID"));
                        version.setVersionName(rs.getString("VersionName"));
                        if (rs.getString("ModelName") != null) {
                            DTOVehicleModel model = new DTOVehicleModel();
                            model.setModelID(rs.getInt("ModelID"));
                            model.setModelName(rs.getString("ModelName"));
                            model.setBasePrice(rs.getBigDecimal("ModelBasePrice"));
                            version.setModel(model);
                        }
                        vehicle.setVersion(version);
                    }

                    DTODiscountPolicy discountPolicy = null;
                    if (rs.getString("PolicyName") != null) {
                        discountPolicy = new DTODiscountPolicy();
                        discountPolicy.setPolicyID(rs.getInt("PolicyID"));
                        discountPolicy.setPolicyName(rs.getString("PolicyName"));
                    }

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

    // ======================================================
    // 🔴 XÓA SALE ORDER VÀ CHI TIẾT
    // ======================================================
    public boolean deleteSaleOrder(int saleOrderID) {
        String sqlDetails = "DELETE FROM SaleOrderDetail WHERE SaleOrderID=?";
        String sqlHeader = "DELETE FROM SaleOrder WHERE SaleOrderID=?";
        try (java.sql.Connection conn = utils.DBUtils.getConnection()) {
            conn.setAutoCommit(false);
            try (java.sql.PreparedStatement ps1 = conn.prepareStatement(sqlDetails)) {
                ps1.setInt(1, saleOrderID); ps1.executeUpdate();
            }
            int rows;
            try (java.sql.PreparedStatement ps2 = conn.prepareStatement(sqlHeader)) {
                ps2.setInt(1, saleOrderID); rows = ps2.executeUpdate();
            }
            if (rows > 0) { conn.commit(); log.info("Deleted SaleOrder id={}", saleOrderID); return true; }
            conn.rollback();
        } catch (java.sql.SQLException e) {
            log.error("Failed deleting SaleOrder id={}", saleOrderID, e);
        }
        return false;
    }

    // ======================================================
    // 📦 TÍNH TOÁN NGÀY GIAO DỰ KIẾN VÀ THỰC TẾ
    // ======================================================
    public boolean updateDeliveryInfo(int saleOrderID, java.sql.Timestamp planned, java.sql.Timestamp actual, Integer etaDays) {
        DTOSaleOrder existing = getSaleOrderById(saleOrderID);
        if (existing == null) return false;
        // do not overwrite actual if already set
        java.sql.Timestamp newActual = existing.getActualDeliveryDate() != null ? existing.getActualDeliveryDate() : actual;
        String sql = "UPDATE SaleOrder SET PlannedDeliveryDate=?, ActualDeliveryDate=?, EtaDays=? WHERE SaleOrderID=?";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, planned);
            ps.setTimestamp(2, newActual);
            if (etaDays!=null) ps.setInt(3, etaDays); else ps.setNull(3, java.sql.Types.INTEGER);
            ps.setInt(4, saleOrderID);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Failed updateDeliveryInfo saleOrderID={}", saleOrderID, e);
            return false;
        }
    }
    public void applyPlannedDeliveryEstimate(DTOSaleOrder order) {
        int eta = 7; // fixed ETA
        java.sql.Timestamp created = order.getCreatedAt();
        long base = created != null ? created.getTime() : System.currentTimeMillis();
        long millis = base + eta * 24L*3600*1000;
        order.setPlannedDeliveryDate(new java.sql.Timestamp(millis));
        order.setEtaDays(eta);
    }
    public void applyActualDeliveryIfEligible(DTOSaleOrder order) {
        if (order.getStatus() == SaleOrderStatus.SHIPPED || order.getStatus() == SaleOrderStatus.COMPLETED) {
            if (order.getActualDeliveryDate() == null) {
                order.setActualDeliveryDate(new java.sql.Timestamp(System.currentTimeMillis()));
                updateDeliveryInfo(order.getSaleOrderID(), order.getPlannedDeliveryDate(), order.getActualDeliveryDate(), order.getEtaDays());
            }
        }
    }

}
