package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class DAOPromotionEvm {

    // ✅ Lấy danh sách PromotionEvm
    public List<DTOPromotionEvm> getAllPromotions() {
        List<DTOPromotionEvm> list = new ArrayList<>();
        String sql = """
            SELECT pe.PromotionEvmID, pe.ModelID, pe.VersionID, pe.PolicyName, pe.DiscountRate,
                   pe.StartDate, pe.EndDate, pe.Description,
                   vm.ModelName, vv.VersionName
            FROM PromotionEvm pe
            LEFT JOIN VehicleModel vm ON pe.ModelID = vm.ModelID
            LEFT JOIN VehicleVersion vv ON pe.VersionID = vv.VersionID
            ORDER BY pe.StartDate DESC
        """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                DTOPromotionEvm promotion = new DTOPromotionEvm();
                promotion.setPromotionEvmID(rs.getInt("PromotionEvmID"));
                promotion.setPolicyName(rs.getString("PolicyName"));
                promotion.setDiscountRate(rs.getBigDecimal("DiscountRate"));
                promotion.setStartDate(rs.getDate("StartDate").toLocalDate());
                promotion.setEndDate(rs.getDate("EndDate").toLocalDate());
                promotion.setDescription(rs.getString("Description"));

                // Model info
                DTOVehicleModel model = new DTOVehicleModel();
                model.setModelID(rs.getInt("ModelID"));
                model.setModelName(rs.getString("ModelName"));
                promotion.setModel(model);

                // Version info
                DTOVehicleVersion version = new DTOVehicleVersion();
                version.setVersionID(rs.getInt("VersionID"));
                version.setVersionName(rs.getString("VersionName"));
                promotion.setVersion(version);

                list.add(promotion);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ✅ Lấy PromotionEvm theo ID
    public DTOPromotionEvm getPromotionById(int promotionID) {
        String sql = """
            SELECT pe.PromotionEvmID, pe.ModelID, pe.VersionID, pe.PolicyName, pe.DiscountRate,
                   pe.StartDate, pe.EndDate, pe.Description,
                   vm.ModelName, vv.VersionName
            FROM PromotionEvm pe
            LEFT JOIN VehicleModel vm ON pe.ModelID = vm.ModelID
            LEFT JOIN VehicleVersion vv ON pe.VersionID = vv.VersionID
            WHERE pe.PromotionEvmID = ?
        """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, promotionID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DTOPromotionEvm promotion = new DTOPromotionEvm();
                    promotion.setPromotionEvmID(rs.getInt("PromotionEvmID"));
                    promotion.setPolicyName(rs.getString("PolicyName"));
                    promotion.setDiscountRate(rs.getBigDecimal("DiscountRate"));
                    promotion.setStartDate(rs.getDate("StartDate").toLocalDate());
                    promotion.setEndDate(rs.getDate("EndDate").toLocalDate());
                    promotion.setDescription(rs.getString("Description"));

                    // Model info
                    DTOVehicleModel model = new DTOVehicleModel();
                    model.setModelID(rs.getInt("ModelID"));
                    model.setModelName(rs.getString("ModelName"));
                    promotion.setModel(model);

                    // Version info
                    DTOVehicleVersion version = new DTOVehicleVersion();
                    version.setVersionID(rs.getInt("VersionID"));
                    version.setVersionName(rs.getString("VersionName"));
                    promotion.setVersion(version);

                    return promotion;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ✅ Tạo PromotionEvm mới
    public boolean createPromotion(DTOPromotionEvm promotion) {
        String sql = "INSERT INTO PromotionEvm (ModelID, VersionID, PolicyName, DiscountRate, StartDate, EndDate, Description) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, promotion.getModel().getModelID());
            ps.setInt(2, promotion.getVersion().getVersionID());
            ps.setString(3, promotion.getPolicyName());
            ps.setBigDecimal(4, promotion.getDiscountRate());
            ps.setDate(5, Date.valueOf(promotion.getStartDate()));
            ps.setDate(6, Date.valueOf(promotion.getEndDate()));
            ps.setString(7, promotion.getDescription());

            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ✅ Cập nhật PromotionEvm
    public boolean updatePromotion(DTOPromotionEvm promotion) {
        String sql = "UPDATE PromotionEvm SET ModelID=?, VersionID=?, PolicyName=?, DiscountRate=?, StartDate=?, EndDate=?, Description=? WHERE PromotionEvmID=?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, promotion.getModel().getModelID());
            ps.setInt(2, promotion.getVersion().getVersionID());
            ps.setString(3, promotion.getPolicyName());
            ps.setBigDecimal(4, promotion.getDiscountRate());
            ps.setDate(5, Date.valueOf(promotion.getStartDate()));
            ps.setDate(6, Date.valueOf(promotion.getEndDate()));
            ps.setString(7, promotion.getDescription());
            ps.setInt(8, promotion.getPromotionEvmID());

            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ✅ Xóa PromotionEvm
    public boolean deletePromotion(int promotionID) {
        String sql = "DELETE FROM PromotionEvm WHERE PromotionEvmID = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, promotionID);

            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
