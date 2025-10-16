package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DAOQuotation {

    // ✅ Thêm mới một quotation
    public int insertQuotation(DTOQuotation quotation) {
        int n = 0;
        String sql = "INSERT INTO Quotation (CustomerID, StaffID, DealerID, CreatedAt, Status) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, quotation.getCustomer().getCustomerID());
            ps.setString(2, quotation.getDealer().get);
            ps.setInt(3, quotation.getDealerID()); // có thể cố định 1 nếu bạn muốn
            ps.setTimestamp(4, quotation.getCreatedAt());
            ps.setString(5, quotation.getStatus());

            n = ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return n;
    }

    // ✅ Lấy tất cả quotation
    public List<DTOQuotation> getAllQuotations() {
        List<DTOQuotation> list = new ArrayList<>();
        String sql = "SELECT QuotationID, CustomerID, StaffID, DealerID, CreatedAt, Status FROM Quotation";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                DTOQuotation q = new DTOQuotation();
                q.setQuotationID(rs.getInt("QuotationID"));
                q.setCustomerID(rs.getInt("CustomerID"));
                q.setStaffID(rs.getInt("StaffID"));
                q.setDealerID(rs.getInt("DealerID"));
                q.setCreatedAt(rs.getTimestamp("CreatedAt"));
                q.setStatus(rs.getString("Status"));
                list.add(q);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    // ✅ Lấy quotation theo ID
    public DTOQuotation getQuotationById(int quotationID) {
        DTOQuotation q = null;
        String sql = "SELECT QuotationID, CustomerID, StaffID, DealerID, CreatedAt, Status FROM Quotation WHERE QuotationID = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, quotationID);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                q = new DTOQuotation();
                q.setQuotationID(rs.getInt("QuotationID"));
                q.setCustomerID(rs.getInt("CustomerID"));
                q.setStaffID(rs.getInt("StaffID"));
                q.setDealerID(rs.getInt("DealerID"));
                q.setCreatedAt(rs.getTimestamp("CreatedAt"));
                q.setStatus(rs.getString("Status"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return q;
    }
}
