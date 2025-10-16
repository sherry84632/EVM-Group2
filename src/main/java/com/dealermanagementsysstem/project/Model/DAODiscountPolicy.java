package com.dealermanagementsysstem.project.Model;

import org.springframework.stereotype.Repository;
import utils.DBUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class DAODiscountPolicy {

    // 1️⃣ Lấy toàn bộ chính sách giảm giá
    public List<DTODiscountPolicy> getAllPolicies() throws SQLException {
        List<DTODiscountPolicy> list = new ArrayList<>();
        String sql = "SELECT * FROM DiscountPolicy ORDER BY StartDate DESC";

        try (Connection cn = DBUtils.getConnection();
             PreparedStatement pst = cn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                list.add(extractPolicy(rs));
            }
        }
        return list;
    }

    // 2️⃣ Lấy chính sách đang hiệu lực theo đại lý
    public DTODiscountPolicy getActivePolicy(int dealerId) throws SQLException {
        String sql = """
            SELECT TOP 1 * FROM DiscountPolicy
            WHERE DealerID = ? AND GETDATE() BETWEEN StartDate AND EndDate
              AND Status = 'Active'
        """;

        try (Connection cn = DBUtils.getConnection();
             PreparedStatement pst = cn.prepareStatement(sql)) {

            pst.setInt(1, dealerId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return extractPolicy(rs);
                }
            }
        }
        return null;
    }

    // 3️⃣ Thêm mới chính sách
    public boolean addPolicy(DTODiscountPolicy dto) throws SQLException {
        String sql = """
            INSERT INTO DiscountPolicy(DealerID, PolicyName, Description, HangPercent, DailyPercent, StartDate, EndDate, Status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection cn = DBUtils.getConnection();
             PreparedStatement pst = cn.prepareStatement(sql)) {

            pst.setInt(1, dto.getDealerID());
            pst.setString(2, dto.getPolicyName());
            pst.setString(3, dto.getDescription());
            pst.setDouble(4, dto.getHangPercent());
            pst.setDouble(5, dto.getDailyPercent());
            pst.setDate(6, Date.valueOf(dto.getStartDate()));
            pst.setDate(7, Date.valueOf(dto.getEndDate()));
            pst.setString(8, dto.getStatus());
            return pst.executeUpdate() > 0;
        }
    }

    // 4️⃣ Áp dụng chính sách vào chi tiết đơn hàng
    public boolean applyPolicyToSaleOrderDetail(int orderDetailId, int policyId) throws SQLException {
        String sql = """
            UPDATE SaleOrderDetail SET PolicyID = ?
            WHERE SaleOrderDetailID = ?
        """;

        try (Connection cn = DBUtils.getConnection();
             PreparedStatement pst = cn.prepareStatement(sql)) {
            pst.setInt(1, policyId);
            pst.setInt(2, orderDetailId);
            return pst.executeUpdate() > 0;
        }
    }

    // Helper method
    private DTODiscountPolicy extractPolicy(ResultSet rs) throws SQLException {
        return new DTODiscountPolicy(
                rs.getInt("PolicyID"),
                rs.getInt("DealerID"),
                rs.getString("PolicyName"),
                rs.getString("Description"),
                rs.getDouble("HangPercent"),
                rs.getDouble("DailyPercent"),
                rs.getDate("StartDate").toLocalDate(),
                rs.getDate("EndDate").toLocalDate(),
                rs.getString("Status")
        );
    }
}
