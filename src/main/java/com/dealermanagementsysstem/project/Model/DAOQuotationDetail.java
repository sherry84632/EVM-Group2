package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DAOQuotationDetail {

    public int insertDetail(DTOQuotationDetail d) {
        String sql = "INSERT INTO QuotationDetail (QuotationID, ColorID, Quantity, UnitPrice, VIN) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, d.getQuotationId());
            ps.setInt(2, d.getColorId());
            ps.setInt(3, d.getQuantity());
            ps.setBigDecimal(4, d.getUnitPrice());
            if (d.getVin() != null) ps.setString(5, d.getVin()); else ps.setNull(5, Types.CHAR);
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<DTOQuotationDetail> getDetailsByQuotationId(int quotationId) {
        List<DTOQuotationDetail> list = new ArrayList<>();
        String sql = "SELECT * FROM QuotationDetail WHERE QuotationID = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quotationId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                DTOQuotationDetail d = new DTOQuotationDetail();
                d.setQuotationDetailId(rs.getInt("QuotationDetailID"));
                d.setQuotationId(rs.getInt("QuotationID"));
                d.setColorId(rs.getInt("ColorID"));
                d.setQuantity(rs.getInt("Quantity"));
                d.setUnitPrice(rs.getBigDecimal("UnitPrice"));
                d.setVin((String) rs.getObject("VIN"));
                list.add(d);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}


