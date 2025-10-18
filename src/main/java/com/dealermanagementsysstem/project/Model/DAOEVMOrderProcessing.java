package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class DAOEVMOrderProcessing {
    public int addProcessing(DTOEVMOrderProcessing process) {
        String sql = "INSERT INTO EVM_OrderProcessing (PurchaseOrderID, EvmStaffID, ActionType, Remarks) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, process.getPurchaseOrderId());
            ps.setInt(2, process.getEvmStaffId());
            ps.setString(3, process.getActionType());
            ps.setString(4, process.getRemarks());
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // Lấy danh sách log xử lý theo đơn hàng
    public List<DTOEVMOrderProcessing> getProcessHistoryByOrderId(int orderId) {
        List<DTOEVMOrderProcessing> list = new ArrayList<>();
        String sql = "SELECT * FROM EVM_OrderProcessing WHERE PurchaseOrderID = ? ORDER BY ActionDate DESC";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                DTOEVMOrderProcessing p = new DTOEVMOrderProcessing(
                        rs.getInt("ProcessID"),
                        rs.getInt("PurchaseOrderID"),
                        rs.getInt("EvmStaffID"),
                        rs.getString("ActionType"),
                        rs.getTimestamp("ActionDate"),
                        rs.getString("Remarks")
                );
                list.add(p);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
