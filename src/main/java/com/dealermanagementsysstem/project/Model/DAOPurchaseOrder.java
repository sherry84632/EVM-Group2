package com.dealermanagementsysstem.project.Model;



import utils.DBUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DAOPurchaseOrder {

    // CREATE
    public int addPurchaseOrder(DTOPurchaseOrder order) {
        String sql = "INSERT INTO PurchaseOrder (DealerID, StaffID, CreatedAt, Status) VALUES (?, ?, GETDATE(), ?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, order.getDealerId());
            ps.setInt(2, order.getStaffId());
            ps.setString(3, order.getStatus());
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // CREATE and return generated ID
    public Integer addPurchaseOrderReturningId(DTOPurchaseOrder order) {
        String sql = "INSERT INTO PurchaseOrder (DealerID, StaffID, CreatedAt, Status) VALUES (?, ?, GETDATE(), ?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, order.getDealerId());
            ps.setInt(2, order.getStaffId());
            ps.setString(3, order.getStatus());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // READ - danh sách
    public List<DTOPurchaseOrder> getAllPurchaseOrders() {
        List<DTOPurchaseOrder> list = new ArrayList<>();
        String sql = "SELECT * FROM PurchaseOrder ORDER BY CreatedAt DESC";
        try (Connection conn = DBUtils.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                DTOPurchaseOrder order = new DTOPurchaseOrder(
                        rs.getInt("PurchaseOrderID"),
                        rs.getInt("DealerID"),
                        rs.getInt("StaffID"),
                        rs.getTimestamp("CreatedAt"),
                        rs.getString("Status")
                );
                list.add(order);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // READ - theo ID
    public DTOPurchaseOrder getPurchaseOrderById(int id) {
        String sql = "SELECT * FROM PurchaseOrder WHERE PurchaseOrderID = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new DTOPurchaseOrder(
                        rs.getInt("PurchaseOrderID"),
                        rs.getInt("DealerID"),
                        rs.getInt("StaffID"),
                        rs.getTimestamp("CreatedAt"),
                        rs.getString("Status")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // UPDATE
    public int updatePurchaseOrder(DTOPurchaseOrder order) {
        String sql = "UPDATE PurchaseOrder SET DealerID=?, StaffID=?, Status=? WHERE PurchaseOrderID=?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, order.getDealerId());
            ps.setInt(2, order.getStaffId());
            ps.setString(3, order.getStatus());
            ps.setInt(4, order.getPurchaseOrderId());
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // DELETE
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
}

