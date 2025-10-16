package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DAOPurchaseOrder {

    // ✅ Lấy danh sách tất cả đơn hàng
    public List<DTOPurchaseOrder> getAllPurchaseOrders() {
        List<DTOPurchaseOrder> list = new ArrayList<>();
        String sql = "SELECT * FROM PurchaseOrder ORDER BY PurchaseOrderID DESC";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                DTOPurchaseOrder o = new DTOPurchaseOrder();
                o.setPurchaseOrderId(rs.getInt("PurchaseOrderID"));
                o.setDealerId(rs.getInt("DealerID"));
                o.setStaffId(rs.getInt("StaffID"));
                o.setStatus(rs.getString("Status"));
                o.setCreatedAt(rs.getTimestamp("CreatedAt"));
                list.add(o);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ✅ Thêm đơn hàng mới (DealerID tự động, ID tự sinh)
    public int addPurchaseOrder(DTOPurchaseOrder order) {
        String sql = "INSERT INTO PurchaseOrder (DealerID, StaffID, Status) VALUES (?, ?, ?)";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, order.getDealerId());
            ps.setInt(2, order.getStaffId());
            ps.setString(3, order.getStatus());
            ps.executeUpdate();

            // ✅ Lấy ID tự sinh (PurchaseOrderID)
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // ✅ Lấy đơn hàng theo ID
    public DTOPurchaseOrder getPurchaseOrderById(int id) {
        String sql = "SELECT * FROM PurchaseOrder WHERE PurchaseOrderID = ?";
        DTOPurchaseOrder o = null;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                o = new DTOPurchaseOrder();
                o.setPurchaseOrderId(rs.getInt("PurchaseOrderID"));
                o.setDealerId(rs.getInt("DealerID"));
                o.setStaffId(rs.getInt("StaffID"));
                o.setStatus(rs.getString("Status"));
                o.setCreatedAt(rs.getTimestamp("CreatedAt"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return o;
    }

    // ✅ Cập nhật đơn hàng
    public int updatePurchaseOrder(DTOPurchaseOrder order) {
        String sql = "UPDATE PurchaseOrder SET StaffID = ?, Status = ? WHERE PurchaseOrderID = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, order.getStaffId());
            ps.setString(2, order.getStatus());
            ps.setInt(3, order.getPurchaseOrderId());

            return ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // ✅ Xóa đơn hàng
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

    // ✅ Lấy DealerID dựa trên tên tài khoản (nếu cần)
    public int getDealerIdByAccount(String username) {
        String sql = "SELECT DealerID FROM Account WHERE Username = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("DealerID");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
