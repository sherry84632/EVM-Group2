package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;
import java.sql.*;

public class DAOQuotation {

    // ✅ Lấy thông tin Vehicle theo VIN để hiển thị trên form báo giá
    public DTOVehicle getVehicleByVIN(String vin) {
        String sql = "SELECT VIN, ModelName, BasePrice, ManufactureYear FROM Vehicle WHERE VIN = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, vin);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                DTOVehicle v = new DTOVehicle();
                v.setVIN(rs.getString("VIN"));
                v.setModelName(rs.getString("ModelName"));
                v.setBasePrice(rs.getBigDecimal("BasePrice"));
                v.setManufactureYear(rs.getInt("ManufactureYear"));
                return v;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ✅ Lấy thông tin Dealer theo ID (nếu cần thêm chi tiết từ DB)
    public DTODealer getDealerById(int dealerId) {
        String sql = "SELECT DealerID, DealerName, Email, Phone, Address FROM Dealer WHERE DealerID = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, dealerId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                DTODealer d = new DTODealer();
                d.setDealerID(rs.getInt("DealerID"));
                d.setDealerName(rs.getString("DealerName"));
                d.setEmail(rs.getString("Email"));
                d.setPhone(rs.getString("Phone"));
                d.setAddress(rs.getString("Address"));
                return d;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
