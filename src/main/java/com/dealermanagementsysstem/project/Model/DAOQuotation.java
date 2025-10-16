package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DAOQuotation {

    // ✅ Lấy toàn bộ danh sách quotation (kèm Dealer, Customer, tổng tiền từ QuotationDetail)
    public List<DTOQuotation> getAllQuotations() {
        List<DTOQuotation> list = new ArrayList<>();

        String sql = """
            SELECT q.QuotationID, q.CreatedAt, q.Status,
                   d.DealerID, d.DealerName, d.Email AS DealerEmail, d.Phone AS DealerPhone,
                   c.CustomerID, c.FullName AS CustomerName, c.Email AS CustomerEmail, c.Phone AS CustomerPhone,
                   SUM(ISNULL(qd.UnitPrice, 0) * ISNULL(qd.Quantity, 0)) AS TotalAmount
            FROM Quotation q
            JOIN Dealer d ON q.DealerID = d.DealerID
            JOIN Customer c ON q.CustomerID = c.CustomerID
            LEFT JOIN QuotationDetail qd ON q.QuotationID = qd.QuotationID
            GROUP BY q.QuotationID, q.CreatedAt, q.Status,
                     d.DealerID, d.DealerName, d.Email, d.Phone,
                     c.CustomerID, c.FullName, c.Email, c.Phone
            ORDER BY q.QuotationID DESC
        """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                DTOQuotation q = new DTOQuotation();

                // --- Quotation ---
                q.setQuotationID(rs.getInt("QuotationID"));
                q.setCreatedAt(rs.getTimestamp("CreatedAt"));
                q.setStatus(rs.getString("Status"));

                // --- Dealer ---
                DTODealer dealer = new DTODealer();
                dealer.setDealerID(rs.getInt("DealerID"));
                dealer.setDealerName(rs.getString("DealerName"));
                dealer.setEmail(rs.getString("DealerEmail"));
                dealer.setPhone(rs.getString("DealerPhone"));
                q.setDealer(dealer);

                // --- Customer ---
                DTOCustomer customer = new DTOCustomer();
                customer.setCustomerID(rs.getInt("CustomerID"));
                customer.setFullName(rs.getString("CustomerName"));
                customer.setEmail(rs.getString("CustomerEmail"));
                customer.setPhone(rs.getString("CustomerPhone"));
                q.setCustomer(customer);

                // --- Tổng tiền từ chi tiết ---
                q.setTotalPrice(rs.getDouble("TotalAmount"));

                list.add(q);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    // ✅ Insert quotation mới (header only)
    public boolean insertQuotation(DTOQuotation q) {
        String sql = """
            INSERT INTO Quotation (DealerID, CustomerID, StaffID, CreatedAt, Status)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, q.getDealer().getDealerID());
            ps.setInt(2, q.getCustomer().getCustomerID());
            // For now, store staff id in StaffID via Customer's id? Controller provides staff separately
            // Expect controller to handle correct staff id usage when calling insertReturningId
            ps.setInt(3, 0);
            ps.setTimestamp(4, q.getCreatedAt());
            ps.setString(5, q.getStatus());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ✅ Insert quotation mới và trả về ID (header only)
    public Integer insertQuotationReturningId(int dealerId, int customerId, int staffId, Timestamp createdAt, String status) {
        String sql = """
            INSERT INTO Quotation (DealerID, CustomerID, StaffID, CreatedAt, Status)
            VALUES (?, ?, ?, ?, ?)
        """;
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, dealerId);
            ps.setInt(2, customerId);
            ps.setInt(3, staffId);
            ps.setTimestamp(4, createdAt);
            ps.setString(5, status);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ✅ Lấy quotation theo ID (kèm Dealer/Customer, tổng tiền từ chi tiết)
    public DTOQuotation getQuotationById(int quotationId) {
        String sql = """
            SELECT q.QuotationID, q.CreatedAt, q.Status,
                   d.DealerID, d.DealerName, d.Email AS DealerEmail, d.Phone AS DealerPhone,
                   c.CustomerID, c.FullName AS CustomerName, c.Email AS CustomerEmail, c.Phone AS CustomerPhone,
                   SUM(ISNULL(qd.UnitPrice, 0) * ISNULL(qd.Quantity, 0)) AS TotalAmount
            FROM Quotation q
            JOIN Dealer d ON q.DealerID = d.DealerID
            JOIN Customer c ON q.CustomerID = c.CustomerID
            LEFT JOIN QuotationDetail qd ON q.QuotationID = qd.QuotationID
            WHERE q.QuotationID = ?
            GROUP BY q.QuotationID, q.CreatedAt, q.Status,
                     d.DealerID, d.DealerName, d.Email, d.Phone,
                     c.CustomerID, c.FullName, c.Email, c.Phone
        """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quotationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DTOQuotation q = new DTOQuotation();
                    q.setQuotationID(rs.getInt("QuotationID"));
                    q.setCreatedAt(rs.getTimestamp("CreatedAt"));
                    q.setStatus(rs.getString("Status"));

                    DTODealer dealer = new DTODealer();
                    dealer.setDealerID(rs.getInt("DealerID"));
                    dealer.setDealerName(rs.getString("DealerName"));
                    dealer.setEmail(rs.getString("DealerEmail"));
                    dealer.setPhone(rs.getString("DealerPhone"));
                    q.setDealer(dealer);

                    DTOCustomer customer = new DTOCustomer();
                    customer.setCustomerID(rs.getInt("CustomerID"));
                    customer.setFullName(rs.getString("CustomerName"));
                    customer.setEmail(rs.getString("CustomerEmail"));
                    customer.setPhone(rs.getString("CustomerPhone"));
                    q.setCustomer(customer);

                    q.setTotalPrice(rs.getDouble("TotalAmount"));

                    return q;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ✅ Update status
    public boolean updateQuotationStatus(int quotationID, String newStatus) {
        String sql = "UPDATE Quotation SET Status = ? WHERE QuotationID = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, quotationID);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ✅ Search quotation theo DealerName / CustomerName
    public List<DTOQuotation> searchQuotation(String keyword) {
        List<DTOQuotation> list = new ArrayList<>();

        String sql = """
            SELECT q.QuotationID, q.CreatedAt, q.Status,
                   d.DealerID, d.DealerName, d.Email AS DealerEmail, d.Phone AS DealerPhone,
                   c.CustomerID, c.FullName AS CustomerName, c.Email AS CustomerEmail, c.Phone AS CustomerPhone,
                   SUM(ISNULL(qd.UnitPrice, 0) * ISNULL(qd.Quantity, 0)) AS TotalAmount
            FROM Quotation q
            JOIN Dealer d ON q.DealerID = d.DealerID
            JOIN Customer c ON q.CustomerID = c.CustomerID
            LEFT JOIN QuotationDetail qd ON q.QuotationID = qd.QuotationID
            WHERE d.DealerName LIKE ? OR c.FullName LIKE ?
            GROUP BY q.QuotationID, q.CreatedAt, q.Status,
                     d.DealerID, d.DealerName, d.Email, d.Phone,
                     c.CustomerID, c.FullName, c.Email, c.Phone
            ORDER BY q.CreatedAt DESC
        """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String key = "%" + keyword + "%";
            ps.setString(1, key);
            ps.setString(2, key);
            ps.setString(3, key);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                DTOQuotation q = new DTOQuotation();

                q.setQuotationID(rs.getInt("QuotationID"));
                q.setCreatedAt(rs.getTimestamp("CreatedAt"));
                q.setStatus(rs.getString("Status"));

                DTODealer dealer = new DTODealer();
                dealer.setDealerID(rs.getInt("DealerID"));
                dealer.setDealerName(rs.getString("DealerName"));
                dealer.setEmail(rs.getString("DealerEmail"));
                dealer.setPhone(rs.getString("DealerPhone"));
                q.setDealer(dealer);

                DTOCustomer customer = new DTOCustomer();
                customer.setCustomerID(rs.getInt("CustomerID"));
                customer.setFullName(rs.getString("CustomerName"));
                customer.setEmail(rs.getString("CustomerEmail"));
                customer.setPhone(rs.getString("CustomerPhone"));
                q.setCustomer(customer);

                q.setTotalPrice(rs.getDouble("TotalAmount"));

                list.add(q);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }
}
