package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;
import java.sql.*;

public class DAOQuotation {

    // ✅ Lấy thông tin xe theo VIN (JOIN Vehicle + EVM_VehicleModel)
    public DTOVehicle getVehicleByVIN(String vin) {
        DTOVehicle vehicle = null;

        String sql = """
            SELECT v.VIN, v.ManufactureYear, vm.ModelName, vm.BasePrice
            FROM Vehicle v
            JOIN EVM_VehicleModel vm ON v.ModelID = vm.ModelID
            WHERE v.VIN = ?
        """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, vin);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    vehicle = new DTOVehicle();
                    vehicle.setVIN(rs.getString("VIN"));
                    vehicle.setModelName(rs.getString("ModelName"));
                    vehicle.setManufactureYear(rs.getInt("ManufactureYear"));
                    vehicle.setBasePrice(rs.getBigDecimal("BasePrice"));
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi khi lấy thông tin xe theo VIN!");
            e.printStackTrace();
        }
        return vehicle;
    }

    // ✅ Lấy thông tin Dealer theo dealerID
    public DTODealer getDealerByID(int dealerID) {
        DTODealer dealer = null;

        String sql = """
            SELECT DealerID, DealerName, Email, Phone, Address
            FROM Dealer
            WHERE DealerID = ?
        """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, dealerID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    dealer = new DTODealer();
                    dealer.setDealerID(rs.getInt("DealerID"));
                    dealer.setDealerName(rs.getString("DealerName"));
                    dealer.setEmail(rs.getString("Email"));
                    dealer.setPhone(rs.getString("Phone"));
                    dealer.setAddress(rs.getString("Address"));
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi khi lấy thông tin dealer!");
            e.printStackTrace();
        }
        return dealer;
    }
}
