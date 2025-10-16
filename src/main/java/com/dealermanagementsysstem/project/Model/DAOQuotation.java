package dao;

import com.dealermanagementsysstem.project.Model.*;
import utils.DBUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DAOQuotation {

    // ✅ Lấy toàn bộ danh sách quotation (kèm Dealer, Customer, Vehicle, DiscountPolicy)
    public List<DAOQuotation> getAllQuotations() {
        List<DTOQuotation> list = new ArrayList<>();

        String sql = """
            SELECT q.QuotationID, q.CreatedAt, q.Status,
                   d.DealerID, d.DealerName, d.Email AS DealerEmail, d.Phone AS DealerPhone,
                   c.CustomerID, c.FullName AS CustomerName, c.Email AS CustomerEmail, c.Phone AS CustomerPhone,
                   v.VehicleID, v.ModelName, v.BasePrice,
                   dp.DiscountPolicyID, dp.HangPercent
            FROM Quotation q
            JOIN Dealer d ON q.DealerID = d.DealerID
            JOIN Customer c ON q.CustomerID = c.CustomerID
            JOIN Vehicle v ON q.VehicleID = v.VehicleID
            LEFT JOIN DiscountPolicy dp ON q.DiscountPolicyID = dp.DiscountPolicyID
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

                // --- Vehicle ---
                DTOVehicle vehicle = new DTOVehicle();
                vehicle.setVIN(rs.getString("VIN"));
                vehicle.setModelName(rs.getString("ModelName"));
                vehicle.setBasePrice(rs.getBigDecimal("BasePrice"));
                q.setVehicle(vehicle);

                // --- Discount Policy ---
                if (rs.getObject("DiscountPolicyID") != null) {
                    DTODiscountPolicy dp = new DTODiscountPolicy();
                    dp.setPolicyID(rs.getInt("DiscountPolicyID"));
                    dp.setHangPercent(rs.getDouble("HangPercent"));
                    q.setDiscountPolicy(dp);
                }

                // --- Total Price ---
                double basePrice = q.getVehicle().getBasePrice().doubleValue();
                if (q.getDiscountPolicy() != null) {
                    double percent = q.getDiscountPolicy().getHangPercent();
                    q.setTotalPrice(basePrice * percent);
                } else {
                    q.setTotalPrice(basePrice);
                }

                list.add(q);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    // ✅ Insert quotation mới
    public boolean insertQuotation(DTOQuotation q) {
        String sql = """
            INSERT INTO Quotation (DealerID, CustomerID, VehicleID, DiscountPolicyID, CreatedAt, Status)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, q.getDealer().getDealerID());
            ps.setInt(2, q.getCustomer().getCustomerID());
            ps.setInt(3, q.getVehicle().getVehicleID());

            if (q.getDiscountPolicy() != null)
                ps.setInt(4, q.getDiscountPolicy().getDiscountPolicyID());
            else
                ps.setNull(4, Types.INTEGER);

            ps.setTimestamp(5, q.getCreatedAt());
            ps.setString(6, q.getStatus());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
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

    // ✅ Search quotation theo DealerName / CustomerName / ModelName
    public List<DTOQuotation> searchQuotation(String keyword) {
        List<DTOQuotation> list = new ArrayList<>();

        String sql = """
            SELECT q.QuotationID, q.CreatedAt, q.Status,
                   d.DealerID, d.DealerName, d.Email AS DealerEmail, d.Phone AS DealerPhone,
                   c.CustomerID, c.FullName AS CustomerName, c.Email AS CustomerEmail, c.Phone AS CustomerPhone,
                   v.VehicleID, v.ModelName, v.BasePrice,
                   dp.DiscountPolicyID, dp.HangPercent
            FROM Quotation q
            JOIN Dealer d ON q.DealerID = d.DealerID
            JOIN Customer c ON q.CustomerID = c.CustomerID
            JOIN Vehicle v ON q.VehicleID = v.VehicleID
            LEFT JOIN DiscountPolicy dp ON q.DiscountPolicyID = dp.DiscountPolicyID
            WHERE d.DealerName LIKE ? OR c.FullName LIKE ? OR v.ModelName LIKE ?
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

                DTOVehicle vehicle = new DTOVehicle();
                vehicle.setVehicleID(rs.getInt("VehicleID"));
                vehicle.setModelName(rs.getString("ModelName"));
                vehicle.setBasePrice(rs.getBigDecimal("BasePrice"));
                q.setVehicle(vehicle);

                if (rs.getObject("DiscountPolicyID") != null) {
                    DTODiscountPolicy dp = new DTODiscountPolicy();
                    dp.setDiscountPolicyID(rs.getInt("DiscountPolicyID"));
                    dp.setHangPercent(rs.getBigDecimal("HangPercent"));
                    q.setDiscountPolicy(dp);
                }

                double basePrice = q.getVehicle().getBasePrice().doubleValue();
                if (q.getDiscountPolicy() != null) {
                    double percent = q.getDiscountPolicy().getHangPercent().doubleValue();
                    q.setTotalPrice(basePrice * percent);
                } else {
                    q.setTotalPrice(basePrice);
                }

                list.add(q);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }
}
