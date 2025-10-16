package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DAOSaleOrder {

    // ✅ Thêm mới SaleOrder + SaleOrderDetail
    public boolean insertSaleOrder(DTOSaleOrder order) {
        String insertOrderSQL = """
            INSERT INTO SaleOrder (CustomerID, DealerID, StaffID, CreatedAt, Status)
            VALUES (?, ?, ?, ?, ?)
        """;

        String insertDetailSQL = """
            INSERT INTO SaleOrderDetail (SaleOrderID, VIN, Price)
            VALUES (?, ?, ?)
        """;

        try (Connection conn = DBUtils.getConnection()) {
            conn.setAutoCommit(false); // ✅ Bắt đầu transaction

            // --- Insert SaleOrder ---
            try (PreparedStatement psOrder = conn.prepareStatement(insertOrderSQL, Statement.RETURN_GENERATED_KEYS)) {
                psOrder.setInt(1, order.getCustomer().getCustomerID());
                psOrder.setInt(2, order.getDealer().getDealerID());
                psOrder.setInt(3, order.getStaff().getStaffID());
                psOrder.setTimestamp(4, order.getCreatedAt());
                psOrder.setString(5, order.getStatus());

                int affectedRows = psOrder.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Creating sale order failed, no rows affected.");
                }

                // --- Lấy SaleOrderID vừa tạo ---
                int saleOrderID;
                try (ResultSet rs = psOrder.getGeneratedKeys()) {
                    if (rs.next()) {
                        saleOrderID = rs.getInt(1);
                    } else {
                        throw new SQLException("Failed to retrieve SaleOrderID.");
                    }
                }

                // --- Insert SaleOrderDetail ---
                if (order.getDetail() != null) {
                    try (PreparedStatement psDetail = conn.prepareStatement(insertDetailSQL)) {
                        for (DTOSaleOrderDetail detail : order.getDetail()) {
                            psDetail.setInt(1, saleOrderID);
                            psDetail.setString(2, detail.getVehicle().getVIN());
                            psDetail.setBigDecimal(3, detail.getPrice());
                            psDetail.addBatch();
                        }
                        psDetail.executeBatch();
                    }
                }

                conn.commit(); // ✅ Commit transaction
                System.out.println("✅ SaleOrder inserted successfully with ID: " + saleOrderID);
                return true;

            } catch (SQLException e) {
                conn.rollback();
                System.err.println("❌ Transaction failed, rolled back!");
                e.printStackTrace();
                return false;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // ✅ Lấy danh sách tất cả SaleOrder kèm Detail
    public List<DTOSaleOrder> getAllSaleOrders() {
        List<DTOSaleOrder> orders = new ArrayList<>();

        String sql = """
            SELECT so.SaleOrderID, so.CreatedAt, so.Status,
                   c.CustomerID, c.FullName AS CustomerName, c.Email AS CustomerEmail, c.Phone AS CustomerPhone,
                   d.DealerID, d.DealerName, d.Email AS DealerEmail, d.Phone AS DealerPhone,
                   s.StaffID, s.FullName AS StaffName, s.Email AS StaffEmail, s.Position,
                   sod.SODetailID, sod.VIN, sod.Price
            FROM SaleOrder so
            JOIN Customer c ON so.CustomerID = c.CustomerID
            JOIN Dealer d ON so.DealerID = d.DealerID
            JOIN DealerStaff s ON so.StaffID = s.StaffID
            LEFT JOIN SaleOrderDetail sod ON so.SaleOrderID = sod.SaleOrderID
            ORDER BY so.SaleOrderID DESC
        """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            DTOSaleOrder currentOrder = null;
            int lastOrderId = -1;

            while (rs.next()) {
                int saleOrderId = rs.getInt("SaleOrderID");

                // --- Nếu là order mới ---
                if (saleOrderId != lastOrderId) {
                    currentOrder = new DTOSaleOrder();
                    currentOrder.setSaleOrderID(saleOrderId);
                    currentOrder.setCreatedAt(rs.getTimestamp("CreatedAt"));
                    currentOrder.setStatus(rs.getString("Status"));

                    // --- Customer ---
                    DTOCustomer customer = new DTOCustomer();
                    customer.setCustomerID(rs.getInt("CustomerID"));
                    customer.setFullName(rs.getString("CustomerName"));
                    customer.setEmail(rs.getString("CustomerEmail"));
                    customer.setPhone(rs.getString("CustomerPhone"));
                    currentOrder.setCustomer(customer);

                    // --- Dealer ---
                    DTODealer dealer = new DTODealer();
                    dealer.setDealerID(rs.getInt("DealerID"));
                    dealer.setDealerName(rs.getString("DealerName"));
                    dealer.setEmail(rs.getString("DealerEmail"));
                    dealer.setPhone(rs.getString("DealerPhone"));
                    currentOrder.setDealer(dealer);

                    // --- Staff ---
                    DTODealerStaff staff = new DTODealerStaff();
                    staff.setStaffID(rs.getInt("StaffID"));
                    staff.setFullName(rs.getString("StaffName"));
                    staff.setEmail(rs.getString("StaffEmail"));
                    staff.setPosition(rs.getString("Position"));
                    currentOrder.setStaff(staff);

                    currentOrder.setDetail(new ArrayList<>());
                    orders.add(currentOrder);
                    lastOrderId = saleOrderId;
                }

                // --- Detail ---
                if (rs.getString("VIN") != null) {
                    DTOSaleOrderDetail detail = new DTOSaleOrderDetail();
                    detail.setSoDetailID(rs.getInt("SODetailID"));
                    detail.setSaleOrderID(saleOrderId);

                    DTOVehicle vehicle = new DTOVehicle();
                    vehicle.setVIN(rs.getString("VIN"));
                    detail.setVehicle(vehicle);

                    detail.setPrice(rs.getBigDecimal("Price"));

                    currentOrder.getDetail().add(detail);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return orders;
    }
}
