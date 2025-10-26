package com.dealermanagementsysstem.project.Model;
import utils.DBUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DAODealerPriceAdjustment {

    // ============ INSERT NEW DISCOUNT ============
    public boolean createDiscount(DTODealerPriceAdjustment dto) {
        String sql = "INSERT INTO DealerPriceAdjustment " +
                "(DealerID, ModelID, DiscountAmount, DiscountPercent, StartDate, EndDate, Notes, PromotionName) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, dto.getDealer().getDealerID());
            ps.setInt(2, dto.getVehicleModel().getModelID());
            ps.setObject(3, dto.getDiscountAmount());
            ps.setObject(4, dto.getDiscountPercent());
            ps.setDate(5, Date.valueOf(dto.getStartDate()));
            ps.setDate(6, dto.getEndDate() != null ? Date.valueOf(dto.getEndDate()) : null);
            ps.setString(7, dto.getNotes());
            ps.setString(8, dto.getPromotionName());

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // ✅ Lấy discount theo DealerID
    public List<DTODealerPriceAdjustment> getDiscountsByDealer(int dealerID) {
        List<DTODealerPriceAdjustment> list = new ArrayList<>();
        String sql = "SELECT * FROM DealerPriceAdjustment WHERE DealerID = ? ORDER BY StartDate DESC";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dealerID);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                DTODealerPriceAdjustment dto = new DTODealerPriceAdjustment(
                        rs.getInt("AdjustmentID"),
                        rs.getObject("DealerID",DTODealer.class),
                        rs.getObject("ModelID",DTOVehicleModel.class),
                        rs.getObject("DiscountAmount", Double.class),
                        rs.getObject("DiscountPercent", Double.class),
                        rs.getDate("StartDate").toLocalDate(),
                        rs.getDate("EndDate") != null ? rs.getDate("EndDate").toLocalDate() : null,
                        rs.getString("Notes"),
                        rs.getString("PromotionName")
                );
                list.add(dto);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // ✅ Tìm discount theo tên và DealerID
    public List<DTODealerPriceAdjustment> searchByPromotionNameAndDealer(String name, int dealerID) {
        List<DTODealerPriceAdjustment> list = new ArrayList<>();
        String sql = "SELECT * FROM DealerPriceAdjustment WHERE DealerID = ? AND PromotionName LIKE ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dealerID);
            ps.setString(2, "%" + name + "%");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                DTODealerPriceAdjustment dto = new DTODealerPriceAdjustment(
                        rs.getInt("AdjustmentID"),
                        rs.getObject("DealerID",DTODealer.class),
                        rs.getObject("ModelID",DTOVehicleModel.class),
                        rs.getObject("DiscountAmount", Double.class),
                        rs.getObject("DiscountPercent", Double.class),
                        rs.getDate("StartDate").toLocalDate(),
                        rs.getDate("EndDate") != null ? rs.getDate("EndDate").toLocalDate() : null,
                        rs.getString("Notes"),
                        rs.getString("PromotionName")
                );
                list.add(dto);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

}
