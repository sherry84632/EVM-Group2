package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DAOPurchaseOrderDetail {

    public int addDetail(DTOPurchaseOrderDetail d) {
        String sql = "INSERT INTO PurchaseOrderDetail (PurchaseOrderID, ColorID, Quantity) VALUES (?, ?, ?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, d.getPurchaseOrderId());
            ps.setInt(2, d.getColorId());
            ps.setInt(3, d.getQuantity());
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<DTOPurchaseOrderDetail> getDetailsByOrderId(int orderId) {
        List<DTOPurchaseOrderDetail> list = new ArrayList<>();
        String sql = "SELECT * FROM PurchaseOrderDetail WHERE PurchaseOrderID = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DTOPurchaseOrderDetail d = new DTOPurchaseOrderDetail();
                    d.setPoDetailId(rs.getInt("PODetailID"));
                    d.setPurchaseOrderId(rs.getInt("PurchaseOrderID"));
                    d.setColorId(rs.getInt("ColorID"));
                    d.setQuantity(rs.getInt("Quantity"));
                    list.add(d);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int deleteDetail(int poDetailId) {
        String sql = "DELETE FROM PurchaseOrderDetail WHERE PODetailID = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, poDetailId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}


