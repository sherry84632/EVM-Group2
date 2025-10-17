package com.dealermanagementsysstem.project.Model;

import org.springframework.stereotype.Repository;
import utils.DBUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository // 🔹 THÊM DÒNG NÀY — rất quan trọng!
public class DAOPurchaseOrder {

    // Lấy danh sách tất cả PurchaseOrders
    public List<DTOPurchaseOrder> getAllPurchaseOrders() {
        List<DTOPurchaseOrder> list = new ArrayList<>();
        String sql = "SELECT PurchaseOrderID, DealerID, StaffID, CreatedAt, Status FROM PurchaseOrder ORDER BY PurchaseOrderID DESC";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                DTOPurchaseOrder dto = new DTOPurchaseOrder();
                dto.setPurchaseOrderId(rs.getInt("PurchaseOrderID"));
                dto.setDealerId(rs.getInt("DealerID"));
                dto.setStaffId(rs.getInt("StaffID"));
                dto.setCreatedAt(rs.getTimestamp("CreatedAt"));
                dto.setStatus(rs.getString("Status"));
                list.add(dto);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Lấy 1 đơn theo id (kèm chi tiết)
    public DTOPurchaseOrder getPurchaseOrderById(int id) {
        String sqlOrder = "SELECT PurchaseOrderID, DealerID, StaffID, CreatedAt, Status FROM PurchaseOrder WHERE PurchaseOrderID = ?";
        String sqlDetail = "SELECT PODetailID, PurchaseOrderID, ColorID, Quantity, ModelID FROM PurchaseOrderDetail WHERE PurchaseOrderID = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement psOrder = conn.prepareStatement(sqlOrder)) {

            psOrder.setInt(1, id);
            try (ResultSet rs = psOrder.executeQuery()) {
                if (rs.next()) {
                    DTOPurchaseOrder dto = new DTOPurchaseOrder();
                    dto.setPurchaseOrderId(rs.getInt("PurchaseOrderID"));
                    dto.setDealerId(rs.getInt("DealerID"));
                    dto.setStaffId(rs.getInt("StaffID"));
                    dto.setCreatedAt(rs.getTimestamp("CreatedAt"));
                    dto.setStatus(rs.getString("Status"));

                    // details
                    try (PreparedStatement psDetail = conn.prepareStatement(sqlDetail)) {
                        psDetail.setInt(1, id);
                        try (ResultSet drs = psDetail.executeQuery()) {
                            List<DTOPurchaseOrderDetail> details = new ArrayList<>();
                            while (drs.next()) {
                                DTOPurchaseOrderDetail d = new DTOPurchaseOrderDetail();
                                d.setPoDetailId(drs.getInt("PODetailID"));
                                d.setPurchaseOrderId(drs.getInt("PurchaseOrderID"));
                                d.setColorId(drs.getInt("ColorID"));
                                d.setQuantity(drs.getInt("Quantity"));
                                d.setModelId(drs.getInt("ModelID"));
                                details.add(d);
                            }
                            dto.setOrderDetails(details);
                        }
                    }

                    return dto;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Cập nhật status cho PurchaseOrder
    public boolean updatePurchaseOrderStatus(int id, String newStatus) {
        String sql = "UPDATE PurchaseOrder SET Status = ? WHERE PurchaseOrderID = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Xoá đơn
    public int deletePurchaseOrder(int id) {
        String sql = "DELETE FROM PurchaseOrder WHERE PurchaseOrderID = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // Thêm đơn hàng mới
    public boolean insertPurchaseOrder(DTOPurchaseOrder order) {
        String sql = "INSERT INTO PurchaseOrder (DealerID, StaffID, CreatedAt, Status) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, order.getDealerId());
            ps.setInt(2, order.getStaffId());
            ps.setTimestamp(3, (Timestamp) order.getCreatedAt());
            ps.setString(4, order.getStatus());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

}
