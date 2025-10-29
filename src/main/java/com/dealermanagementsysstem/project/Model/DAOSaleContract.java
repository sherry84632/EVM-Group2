package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class DAOSaleContract {

    // ✅ Lấy danh sách SaleContract
    public List<DTOSaleContract> getAllSaleContracts() {
        List<DTOSaleContract> list = new ArrayList<>();
        String sql = """
            SELECT sc.ContractID, sc.SaleOrderID, sc.ContractDate, sc.Status, sc.TotalAmount
            FROM SaleContract sc
            ORDER BY sc.ContractDate DESC
        """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                DTOSaleContract contract = new DTOSaleContract();
                contract.setContractID(rs.getInt("ContractID"));
                java.sql.Date contractDate = rs.getDate("ContractDate");
                contract.setContractDate(contractDate != null ? new java.util.Date(contractDate.getTime()) : null);
                contract.setStatus(SaleContractStatus.valueOf(rs.getString("Status")));
                contract.setTotalAmount(rs.getBigDecimal("TotalAmount"));

                // SaleOrder info
                DTOSaleOrder saleOrder = new DTOSaleOrder();
                saleOrder.setSaleOrderID(rs.getInt("SaleOrderID"));
                contract.setSaleOrder(saleOrder);

                list.add(contract);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ✅ Lấy SaleContract theo ID
    public DTOSaleContract getSaleContractById(int contractID) {
        String sql = """
            SELECT sc.ContractID, sc.SaleOrderID, sc.ContractDate, sc.Status, sc.TotalAmount
            FROM SaleContract sc
            WHERE sc.ContractID = ?
        """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, contractID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DTOSaleContract contract = new DTOSaleContract();
                    contract.setContractID(rs.getInt("ContractID"));
                    java.sql.Date contractDate = rs.getDate("ContractDate");
                contract.setContractDate(contractDate != null ? new java.util.Date(contractDate.getTime()) : null);
                    contract.setStatus(SaleContractStatus.valueOf(rs.getString("Status")));
                    contract.setTotalAmount(rs.getBigDecimal("TotalAmount"));

                    // SaleOrder info
                    DTOSaleOrder saleOrder = new DTOSaleOrder();
                    saleOrder.setSaleOrderID(rs.getInt("SaleOrderID"));
                    contract.setSaleOrder(saleOrder);

                    return contract;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ✅ Tạo SaleContract mới
    public boolean createSaleContract(DTOSaleContract contract) {
        String sql = "INSERT INTO SaleContract (SaleOrderID, ContractDate, Status, TotalAmount) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, contract.getSaleOrder().getSaleOrderID());
            ps.setDate(2, new java.sql.Date(contract.getContractDate().getTime()));
            ps.setString(3, contract.getStatus().toString());
            ps.setBigDecimal(4, contract.getTotalAmount());

            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ✅ Cập nhật trạng thái SaleContract
    public boolean updateSaleContractStatus(int contractID, SaleContractStatus status) {
        String sql = "UPDATE SaleContract SET Status = ? WHERE ContractID = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.toString());
            ps.setInt(2, contractID);

            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ✅ Lấy SaleContract theo SaleOrderID
    public DTOSaleContract getSaleContractBySaleOrderId(int saleOrderID) {
        String sql = """
            SELECT sc.ContractID, sc.SaleOrderID, sc.ContractDate, sc.Status, sc.TotalAmount
            FROM SaleContract sc
            WHERE sc.SaleOrderID = ?
        """;
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, saleOrderID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DTOSaleContract c = new DTOSaleContract();
                    c.setContractID(rs.getInt("ContractID"));
                    java.sql.Date contractDate = rs.getDate("ContractDate");
                    c.setContractDate(contractDate != null ? new java.util.Date(contractDate.getTime()) : null);
                    c.setStatus(SaleContractStatus.valueOf(rs.getString("Status")));
                    c.setTotalAmount(rs.getBigDecimal("TotalAmount"));
                    DTOSaleOrder so = new DTOSaleOrder(); so.setSaleOrderID(rs.getInt("SaleOrderID")); c.setSaleOrder(so);
                    return c;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Delete a contract by its ContractID */
    public boolean deleteContract(int contractID) {
        String sql = "DELETE FROM SaleContract WHERE ContractID=?";
        try (java.sql.Connection conn = DBUtils.getConnection(); java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, contractID); return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }
    /** Delete all contracts referencing a sale order (for cascade manual) */
    public int deleteContractsBySaleOrderID(int saleOrderID) {
        String sql = "DELETE FROM SaleContract WHERE SaleOrderID=?";
        try (java.sql.Connection conn = DBUtils.getConnection(); java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, saleOrderID); return ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); return 0; }
    }
}
