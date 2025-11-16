package com.dealermanagementsysstem.project.Model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import utils.DBUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class DAODealerPriceAdjustment {
    private static final Logger log = LoggerFactory.getLogger(DAODealerPriceAdjustment.class);

    // ============ INSERT NEW DISCOUNT ============
    public boolean createDiscount(DTODealerPriceAdjustment dto) {
        String sql = "INSERT INTO DealerPriceAdjustment " +
                "(DealerID, ModelID, DiscountAmount, DiscountPercent, StartDate, EndDate, Notes, PromotionName, ApplicableModelIDs) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, dto.getDealer().getDealerID());
            ps.setObject(2, dto.getVehicleModel() != null ? dto.getVehicleModel().getModelID() : null);
            ps.setObject(3, dto.getDiscountAmount());
            ps.setObject(4, dto.getDiscountPercent());
            ps.setDate(5, Date.valueOf(dto.getStartDate()));
            ps.setDate(6, dto.getEndDate() != null ? Date.valueOf(dto.getEndDate()) : null);
            ps.setString(7, dto.getNotes());
            ps.setString(8, dto.getPromotionName());
            ps.setString(9, dto.getApplicableModelIDs());

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            log.error("Error creating discount dealerID={} modelIDs={}", dto.getDealer().getDealerID(), dto.getApplicableModelIDs(), e);
        }
        return false;
    }

    //  Lấy discount theo DealerID
    public List<DTODealerPriceAdjustment> getDiscountsByDealer(int dealerID) {
        List<DTODealerPriceAdjustment> list = new ArrayList<>();
        String sql = "SELECT dpa.*, vm.ModelName FROM DealerPriceAdjustment dpa " +
                     "LEFT JOIN VehicleModel vm ON dpa.ModelID = vm.ModelID " +
                     "WHERE dpa.DealerID = ? ORDER BY dpa.StartDate DESC";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dealerID);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                // Create Dealer object
                DTODealer dealer = new DTODealer();
                dealer.setDealerID(rs.getInt("DealerID"));

                // Create VehicleModel object
                DTOVehicleModel vehicleModel = new DTOVehicleModel();
                vehicleModel.setModelID(rs.getInt("ModelID"));
                vehicleModel.setModelName(rs.getString("ModelName")); // Load model name

                DTODealerPriceAdjustment dto = new DTODealerPriceAdjustment(
                        rs.getInt("AdjustmentID"),
                        dealer,
                        vehicleModel,
                        rs.getObject("DiscountAmount", Double.class),
                        rs.getObject("DiscountPercent", Double.class),
                        rs.getDate("StartDate").toLocalDate(),
                        rs.getDate("EndDate") != null ? rs.getDate("EndDate").toLocalDate() : null,
                        rs.getString("Notes"),
                        rs.getString("PromotionName")
                );
                dto.setApplicableModelIDs(rs.getString("ApplicableModelIDs"));
                list.add(dto);
            }
        } catch (Exception e) {
            log.error("Error fetching discounts dealerID={}", dealerID, e);
        }
        return list;
    }

    //  Tìm discount theo tên và DealerID
    public List<DTODealerPriceAdjustment> searchByPromotionNameAndDealer(String name, int dealerID) {
        List<DTODealerPriceAdjustment> list = new ArrayList<>();
        String sql = "SELECT dpa.*, vm.ModelName FROM DealerPriceAdjustment dpa " +
                     "LEFT JOIN VehicleModel vm ON dpa.ModelID = vm.ModelID " +
                     "WHERE dpa.DealerID = ? AND dpa.PromotionName LIKE ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dealerID);
            ps.setString(2, "%" + name + "%");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                // Create Dealer object
                DTODealer dealer = new DTODealer();
                dealer.setDealerID(rs.getInt("DealerID"));

                // Create VehicleModel object
                DTOVehicleModel vehicleModel = new DTOVehicleModel();
                vehicleModel.setModelID(rs.getInt("ModelID"));
                vehicleModel.setModelName(rs.getString("ModelName")); // Load model name

                DTODealerPriceAdjustment dto = new DTODealerPriceAdjustment(
                        rs.getInt("AdjustmentID"),
                        dealer,
                        vehicleModel,
                        rs.getObject("DiscountAmount", Double.class),
                        rs.getObject("DiscountPercent", Double.class),
                        rs.getDate("StartDate").toLocalDate(),
                        rs.getDate("EndDate") != null ? rs.getDate("EndDate").toLocalDate() : null,
                        rs.getString("Notes"),
                        rs.getString("PromotionName")
                );
                dto.setApplicableModelIDs(rs.getString("ApplicableModelIDs"));
                list.add(dto);
            }
        } catch (Exception e) {
            log.error("Error searching discounts name={} dealerID={}", name, dealerID, e);
        }
        return list;
    }

    public DTODealerPriceAdjustment getDiscountById(int adjustmentID) {
        String sql = "SELECT dpa.*, vm.ModelName FROM DealerPriceAdjustment dpa " +
                     "LEFT JOIN VehicleModel vm ON dpa.ModelID = vm.ModelID " +
                     "WHERE dpa.AdjustmentID = ?";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, adjustmentID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DTODealer dealer = new DTODealer();
                    dealer.setDealerID(rs.getInt("DealerID"));

                    DTOVehicleModel model = new DTOVehicleModel();
                    model.setModelID(rs.getInt("ModelID"));
                    model.setModelName(rs.getString("ModelName")); // Load model name

                    DTODealerPriceAdjustment dto = new DTODealerPriceAdjustment(
                            rs.getInt("AdjustmentID"),
                            dealer,
                            model,
                            rs.getObject("DiscountAmount", Double.class),
                            rs.getObject("DiscountPercent", Double.class),
                            rs.getDate("StartDate").toLocalDate(),
                            rs.getDate("EndDate") != null ? rs.getDate("EndDate").toLocalDate() : null,
                            rs.getString("Notes"),
                            rs.getString("PromotionName")
                    );
                    dto.setApplicableModelIDs(rs.getString("ApplicableModelIDs"));
                    return dto;
                }
            }
        } catch (Exception e) {
            log.error("Error getDiscountById adjustmentID={}", adjustmentID, e);
        }
        return null;
    }

    public List<DTODealerPriceAdjustment> getActiveDiscountsByDealer(int dealerID) {
        List<DTODealerPriceAdjustment> list = new ArrayList<>();
        String sql = "SELECT dpa.*, vm.ModelName FROM DealerPriceAdjustment dpa " +
                     "LEFT JOIN VehicleModel vm ON dpa.ModelID = vm.ModelID " +
                     "WHERE dpa.DealerID=?";
        java.time.LocalDate today = java.time.LocalDate.now();
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dealerID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.time.LocalDate start = rs.getDate("StartDate").toLocalDate();
                    java.sql.Date endDateSql = rs.getDate("EndDate");
                    java.time.LocalDate end = endDateSql != null ? endDateSql.toLocalDate() : null;

                    // Filter: only active discounts
                    if (start.isAfter(today)) continue; // not started
                    if (end != null && end.isBefore(today)) continue; // expired

                    DTODealer dealer = new DTODealer();
                    dealer.setDealerID(rs.getInt("DealerID"));

                    DTOVehicleModel model = new DTOVehicleModel();
                    model.setModelID(rs.getInt("ModelID"));
                    model.setModelName(rs.getString("ModelName")); // Load model name

                    DTODealerPriceAdjustment dto = new DTODealerPriceAdjustment(
                            rs.getInt("AdjustmentID"), dealer, model,
                            rs.getObject("DiscountAmount", Double.class),
                            rs.getObject("DiscountPercent", Double.class),
                            start, end, rs.getString("Notes"), rs.getString("PromotionName")
                    );
                    dto.setApplicableModelIDs(rs.getString("ApplicableModelIDs"));
                    list.add(dto);
                }
            }
        } catch (Exception e) {
            log.error("Error active discounts dealerID={}", dealerID, e);
        }
        return list;
    }

    public boolean updateDiscount(DTODealerPriceAdjustment dto) {
        String sql = "UPDATE DealerPriceAdjustment SET ModelID = ?, DiscountAmount = ?, DiscountPercent = ?, StartDate = ?, EndDate = ?, Notes = ?, PromotionName = ?, ApplicableModelIDs = ? WHERE AdjustmentID = ?";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, dto.getVehicleModel() != null ? dto.getVehicleModel().getModelID() : null);
            ps.setObject(2, dto.getDiscountAmount());
            ps.setObject(3, dto.getDiscountPercent());
            ps.setDate(4, Date.valueOf(dto.getStartDate()));
            ps.setDate(5, dto.getEndDate() != null ? Date.valueOf(dto.getEndDate()) : null);
            ps.setString(6, dto.getNotes());
            ps.setString(7, dto.getPromotionName());
            ps.setString(8, dto.getApplicableModelIDs());
            ps.setInt(9, dto.getAdjustmentID());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            log.error("Error updating discount adjustmentID={}", dto.getAdjustmentID(), e);
        }
        return false;
    }

    public boolean deleteDiscount(int adjustmentID) {
        String sql = "DELETE FROM DealerPriceAdjustment WHERE AdjustmentID = ?";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, adjustmentID);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            log.error("Error deleting discount adjustmentID={}", adjustmentID, e);
        }
        return false;
    }
}
