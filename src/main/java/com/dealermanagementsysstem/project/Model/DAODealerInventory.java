package com.dealermanagementsysstem.project.Model;

import utils.DBUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Repository
public class DAODealerInventory {

    private static final Logger log = LoggerFactory.getLogger(DAODealerInventory.class);

    // ✅ Lấy danh sách xe theo DealerID
    public List<DTODealerInventory> getVehiclesByDealerID(int dealerID) {
        List<DTODealerInventory> list = new ArrayList<>();
        String sql = """
                    SELECT di.DealerInventoryID, di.DealerID, di.VIN, di.ReceivedDate, di.Status, di.CostPrice,
                           v.VehicleID, v.ManufactureYear, v.EngineNumber, v.Status AS VehicleStatus,
                           vc.ColorID, vc.ColorName,
                           vv.VersionID, vv.VersionName,
                           vm.ModelID, vm.ModelName, vm.BasePrice
                    FROM DealerInventory di
                    LEFT JOIN Vehicle v ON di.VehicleID = v.VehicleID
                    LEFT JOIN VehicleColor vc ON v.ColorID = vc.ColorID
                    LEFT JOIN VehicleVersion vv ON v.VersionID = vv.VersionID
                    LEFT JOIN VehicleModel vm ON vv.ModelID = vm.ModelID
                    WHERE di.DealerID = ?
                    ORDER BY di.ReceivedDate DESC
                """;
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dealerID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DTODealerInventory dto = new DTODealerInventory();
                    dto.setDealerInventoryID(rs.getInt("DealerInventoryID"));
                    dto.setReceivedDate(rs.getDate("ReceivedDate"));
                    dto.setStatus(DealerInventoryStatus.valueOf(rs.getString("Status")));
                    dto.setVin(rs.getString("VIN"));
                    dto.setCostPrice(rs.getBigDecimal("CostPrice")); // ✅ Lấy giá cost

                    DTODealer dealer = new DTODealer();
                    dealer.setDealerID(rs.getInt("DealerID"));
                    dto.setDealer(dealer);

                    // enrich vehicle data for UI rendering
                    DTOVehicle vehicle = new DTOVehicle();
                    vehicle.setVehicleID(rs.getInt("VehicleID"));
                    vehicle.setManufactureYear(rs.getInt("ManufactureYear"));
                    vehicle.setEngineNumber(rs.getString("EngineNumber"));
                    if (rs.getString("VehicleStatus") != null) {
                        vehicle.setStatus(VehicleStatus.valueOf(rs.getString("VehicleStatus")));
                    }
                    if (rs.getString("ColorName") != null) {
                        DTOVehicleColor color = new DTOVehicleColor();
                        color.setColorID(rs.getInt("ColorID"));
                        color.setColorName(rs.getString("ColorName"));
                        vehicle.setColor(color);
                    }
                    if (rs.getString("VersionName") != null) {
                        DTOVehicleVersion version = new DTOVehicleVersion();
                        version.setVersionID(rs.getInt("VersionID"));
                        version.setVersionName(rs.getString("VersionName"));
                        // attach model for base price if exists
                        DTOVehicleModel model = new DTOVehicleModel();
                        model.setModelID(rs.getInt("ModelID"));
                        model.setModelName(rs.getString("ModelName"));
                        model.setBasePrice(rs.getBigDecimal("BasePrice"));
                        version.setModel(model);
                        vehicle.setVersion(version);
                    }
                    dto.setVehicle(vehicle);
                    list.add(dto);
                }
            }
        } catch (SQLException e) {
            log.error("Error fetching inventory for dealerID={}", dealerID, e);
        }
        return list;
    }

    // ✅ Support keyword filtering by model/version/color name
    public List<DTODealerInventory> getVehiclesByDealerIDWithKeyword(int dealerID, String keyword) {
        List<DTODealerInventory> list = new ArrayList<>();
        String sql = """
                    SELECT di.DealerInventoryID, di.DealerID, di.VIN, di.ReceivedDate, di.Status,
                           v.VehicleID, v.ManufactureYear, v.EngineNumber, v.Status AS VehicleStatus,
                           vc.ColorID, vc.ColorName,
                           vv.VersionID, vv.VersionName,
                           vm.ModelID, vm.ModelName, vm.BasePrice
                    FROM DealerInventory di
                    LEFT JOIN Vehicle v ON di.VehicleID = v.VehicleID
                    LEFT JOIN VehicleColor vc ON v.ColorID = vc.ColorID
                    LEFT JOIN VehicleVersion vv ON v.VersionID = vv.VersionID
                    LEFT JOIN VehicleModel vm ON vv.ModelID = vm.ModelID
                    WHERE di.DealerID = ?
                      AND di.VIN LIKE ?
                    ORDER BY di.ReceivedDate DESC
                """;
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dealerID);
            String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
            ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DTODealerInventory dto = new DTODealerInventory();
                    dto.setDealerInventoryID(rs.getInt("DealerInventoryID"));
                    dto.setReceivedDate(rs.getDate("ReceivedDate"));
                    dto.setStatus(DealerInventoryStatus.valueOf(rs.getString("Status")));
                    dto.setVin(rs.getString("VIN"));

                    DTODealer dealer = new DTODealer();
                    dealer.setDealerID(rs.getInt("DealerID"));
                    dto.setDealer(dealer);
                    // enrich vehicle
                    DTOVehicle vehicle = new DTOVehicle();
                    vehicle.setVehicleID(rs.getInt("VehicleID"));
                    vehicle.setManufactureYear(rs.getInt("ManufactureYear"));
                    vehicle.setEngineNumber(rs.getString("EngineNumber"));
                    if (rs.getString("VehicleStatus") != null) {
                        vehicle.setStatus(VehicleStatus.valueOf(rs.getString("VehicleStatus")));
                    }
                    if (rs.getString("ColorName") != null) {
                        DTOVehicleColor color = new DTOVehicleColor();
                        color.setColorID(rs.getInt("ColorID"));
                        color.setColorName(rs.getString("ColorName"));
                        vehicle.setColor(color);
                    }
                    if (rs.getString("VersionName") != null) {
                        DTOVehicleVersion version = new DTOVehicleVersion();
                        version.setVersionID(rs.getInt("VersionID"));
                        version.setVersionName(rs.getString("VersionName"));
                        DTOVehicleModel model = new DTOVehicleModel();
                        model.setModelID(rs.getInt("ModelID"));
                        model.setModelName(rs.getString("ModelName"));
                        model.setBasePrice(rs.getBigDecimal("BasePrice"));
                        version.setModel(model);
                        vehicle.setVersion(version);
                    }
                    dto.setVehicle(vehicle);
                    list.add(dto);
                }
            }
        } catch (SQLException e) {
            log.error("Error fetching inventory with keyword for dealerID={}", dealerID, e);
        }
        return list;
    }

    // ✅ Full filter query
    public List<DTODealerInventory> getVehiclesByDealerIDWithFilters(int dealerID,
                                                                     String vin,
                                                                     Integer modelId,
                                                                     Integer versionId,
                                                                     Integer colorId,
                                                                     String status,
                                                                     java.sql.Date receivedFrom,
                                                                     java.sql.Date receivedTo) {
        List<DTODealerInventory> list = new ArrayList<>();
        String sql = """
                SELECT di.DealerInventoryID, di.DealerID, di.VIN, di.ReceivedDate, di.Status,
                       v.VehicleID, v.ManufactureYear, v.EngineNumber, v.Status AS VehicleStatus,
                       vc.ColorID, vc.ColorName,
                       vv.VersionID, vv.VersionName,
                       vm.ModelID, vm.ModelName, vm.BasePrice
                FROM DealerInventory di
                LEFT JOIN Vehicle v ON di.VehicleID = v.VehicleID
                LEFT JOIN VehicleColor vc ON v.ColorID = vc.ColorID
                LEFT JOIN VehicleVersion vv ON v.VersionID = vv.VersionID
                LEFT JOIN VehicleModel vm ON vv.ModelID = vm.ModelID
                WHERE di.DealerID = ?
                  AND (? IS NULL OR di.VIN LIKE ?)
                  AND (? IS NULL OR vm.ModelID = ?)
                  AND (? IS NULL OR vv.VersionID = ?)
                  AND (? IS NULL OR vc.ColorID = ?)
                  AND (? IS NULL OR di.Status = ?)
                  AND (? IS NULL OR ? IS NULL OR di.ReceivedDate BETWEEN ? AND ?)
                ORDER BY di.ReceivedDate DESC
                """;
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setInt(idx++, dealerID);
            // vin
            if (vin == null || vin.isBlank()) { ps.setNull(idx++, Types.VARCHAR); ps.setNull(idx++, Types.VARCHAR); }
            else { ps.setString(idx++, vin); ps.setString(idx++, "%" + vin + "%"); }
            // modelId
            if (modelId == null || modelId <= 0) { ps.setNull(idx++, Types.INTEGER); ps.setNull(idx++, Types.INTEGER); }
            else { ps.setInt(idx++, modelId); ps.setInt(idx++, modelId); }
            // versionId
            if (versionId == null || versionId <= 0) { ps.setNull(idx++, Types.INTEGER); ps.setNull(idx++, Types.INTEGER); }
            else { ps.setInt(idx++, versionId); ps.setInt(idx++, versionId); }
            // colorId
            if (colorId == null || colorId <= 0) { ps.setNull(idx++, Types.INTEGER); ps.setNull(idx++, Types.INTEGER); }
            else { ps.setInt(idx++, colorId); ps.setInt(idx++, colorId); }
            // status
            if (status == null || status.isBlank()) { ps.setNull(idx++, Types.VARCHAR); ps.setNull(idx++, Types.VARCHAR); }
            else { ps.setString(idx++, status); ps.setString(idx++, status); }
            // date range
            if (receivedFrom == null || receivedTo == null) {
                ps.setNull(idx++, Types.DATE); ps.setNull(idx++, Types.DATE); ps.setNull(idx++, Types.DATE); ps.setNull(idx++, Types.DATE);
            } else {
                ps.setDate(idx++, receivedFrom); ps.setDate(idx++, receivedTo); ps.setDate(idx++, receivedFrom); ps.setDate(idx++, receivedTo);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DTODealerInventory dto = new DTODealerInventory();
                    dto.setDealerInventoryID(rs.getInt("DealerInventoryID"));
                    dto.setReceivedDate(rs.getDate("ReceivedDate"));
                    dto.setStatus(DealerInventoryStatus.valueOf(rs.getString("Status")));
                    dto.setVin(rs.getString("VIN"));
                    DTODealer dealer = new DTODealer();
                    dealer.setDealerID(rs.getInt("DealerID"));
                    dto.setDealer(dealer);
                    DTOVehicle vehicle = new DTOVehicle();
                    vehicle.setVehicleID(rs.getInt("VehicleID"));
                    vehicle.setManufactureYear(rs.getInt("ManufactureYear"));
                    vehicle.setEngineNumber(rs.getString("EngineNumber"));
                    if (rs.getString("VehicleStatus") != null) vehicle.setStatus(VehicleStatus.valueOf(rs.getString("VehicleStatus")));
                    if (rs.getString("ColorName") != null) { DTOVehicleColor c = new DTOVehicleColor(); c.setColorID(rs.getInt("ColorID")); c.setColorName(rs.getString("ColorName")); vehicle.setColor(c); }
                    if (rs.getString("VersionName") != null) { DTOVehicleVersion v = new DTOVehicleVersion(); v.setVersionID(rs.getInt("VersionID")); v.setVersionName(rs.getString("VersionName")); DTOVehicleModel m = new DTOVehicleModel(); m.setModelID(rs.getInt("ModelID")); m.setModelName(rs.getString("ModelName")); m.setBasePrice(rs.getBigDecimal("BasePrice")); v.setModel(m); vehicle.setVersion(v); }
                    dto.setVehicle(vehicle);
                    list.add(dto);
                }
            }
        } catch (SQLException e) {
            log.error("Error fetching filtered inventory dealerID={} vin={} model={} version={} color={}", dealerID, vin, modelId, versionId, colorId, e);
        }
        return list;
    }

    // ✅ Summary stats by filters
    public DTOInventorySummary getInventorySummary(int dealerID,
                                                   String vin,
                                                   Integer modelId,
                                                   Integer versionId,
                                                   Integer colorId,
                                                   String status,
                                                   java.sql.Date receivedFrom,
                                                   java.sql.Date receivedTo) {
        DTOInventorySummary s = new DTOInventorySummary();
        String sql = """
            SELECT 
              COUNT(*) AS Total,
              SUM(CASE WHEN di.Status = 'AVAILABLE' THEN 1 ELSE 0 END) AS AvailableCnt,
              SUM(CASE WHEN di.Status = 'RESERVED' THEN 1 ELSE 0 END) AS ReservedCnt,
              SUM(CASE WHEN di.Status = 'SOLD' THEN 1 ELSE 0 END) AS SoldCnt,
              SUM(CASE WHEN di.Status = 'TRANSFERRED' THEN 1 ELSE 0 END) AS TransferredCnt,
              SUM(CASE WHEN di.Status = 'AVAILABLE' THEN vm.BasePrice ELSE 0 END) AS InventoryValue
            FROM DealerInventory di
            LEFT JOIN Vehicle v ON di.VehicleID = v.VehicleID
            LEFT JOIN VehicleVersion vv ON v.VersionID = vv.VersionID
            LEFT JOIN VehicleModel vm ON vv.ModelID = vm.ModelID
            WHERE di.DealerID = ?
              AND (? IS NULL OR di.VIN LIKE ?)
              AND (? IS NULL OR vm.ModelID = ?)
              AND (? IS NULL OR vv.VersionID = ?)
              AND (? IS NULL OR v.ColorID = ?)
              AND (? IS NULL OR di.Status = ?)
              AND (? IS NULL OR ? IS NULL OR di.ReceivedDate BETWEEN ? AND ?)
        """;
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setInt(idx++, dealerID);
            if (vin == null || vin.isBlank()) { ps.setNull(idx++, Types.VARCHAR); ps.setNull(idx++, Types.VARCHAR); } else { ps.setString(idx++, vin); ps.setString(idx++, "%" + vin + "%"); }
            if (modelId == null || modelId <= 0) { ps.setNull(idx++, Types.INTEGER); ps.setNull(idx++, Types.INTEGER); } else { ps.setInt(idx++, modelId); ps.setInt(idx++, modelId); }
            if (versionId == null || versionId <= 0) { ps.setNull(idx++, Types.INTEGER); ps.setNull(idx++, Types.INTEGER); } else { ps.setInt(idx++, versionId); ps.setInt(idx++, versionId); }
            if (colorId == null || colorId <= 0) { ps.setNull(idx++, Types.INTEGER); ps.setNull(idx++, Types.INTEGER); } else { ps.setInt(idx++, colorId); ps.setInt(idx++, colorId); }
            if (status == null || status.isBlank()) { ps.setNull(idx++, Types.VARCHAR); ps.setNull(idx++, Types.VARCHAR); } else { ps.setString(idx++, status); ps.setString(idx++, status); }
            if (receivedFrom == null || receivedTo == null) { ps.setNull(idx++, Types.DATE); ps.setNull(idx++, Types.DATE); ps.setNull(idx++, Types.DATE); ps.setNull(idx++, Types.DATE); } else { ps.setDate(idx++, receivedFrom); ps.setDate(idx++, receivedTo); ps.setDate(idx++, receivedFrom); ps.setDate(idx++, receivedTo); }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    s.setTotal(rs.getInt("Total"));
                    s.setInStock(rs.getInt("AvailableCnt"));
                    s.setReserved(rs.getInt("ReservedCnt"));
                    s.setSold(rs.getInt("SoldCnt"));
                    s.setTransferred(rs.getInt("TransferredCnt"));
                    java.math.BigDecimal val = rs.getBigDecimal("InventoryValue");
                    s.setInventoryValue(val != null ? val : java.math.BigDecimal.ZERO);
                }
            }
        } catch (SQLException e) {
            log.error("Error computing inventory summary dealerID={}", dealerID, e);
        }
        return s;
    }

    // ✅ Lookup lists for cascading dropdowns
    public List<DTOVehicleModel> getAllModels() {
        List<DTOVehicleModel> list = new ArrayList<>();
        String sql = "SELECT ModelID, ModelName, BasePrice FROM VehicleModel ORDER BY ModelName";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) { DTOVehicleModel m = new DTOVehicleModel(); m.setModelID(rs.getInt("ModelID")); m.setModelName(rs.getString("ModelName")); m.setBasePrice(rs.getBigDecimal("BasePrice")); list.add(m);} }
        catch (SQLException e) { log.error("Error fetching models", e);} return list;
    }
    public List<DTOVehicleVersion> getVersionsByModel(int modelId) {
        List<DTOVehicleVersion> list = new ArrayList<>();
        String sql = "SELECT VersionID, VersionName FROM VehicleVersion WHERE ModelID = ? ORDER BY VersionName";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, modelId); try (ResultSet rs = ps.executeQuery()) { while (rs.next()) { DTOVehicleVersion v = new DTOVehicleVersion(); v.setVersionID(rs.getInt("VersionID")); v.setVersionName(rs.getString("VersionName")); list.add(v);} }
        } catch (SQLException e) { log.error("Error fetching versions by model {}", modelId, e);} return list;
    }
    public List<DTOVehicleColor> getColorsByModelVersion(Integer modelId, Integer versionId) {
        List<DTOVehicleColor> list = new ArrayList<>();
        String sql = versionId != null && versionId > 0
                ? "SELECT vc.ColorID, vc.ColorName FROM VehicleColor vc JOIN VehicleVersion vv ON vc.ModelID = vv.ModelID WHERE vv.VersionID = ? ORDER BY vc.ColorName"
                : "SELECT ColorID, ColorName FROM VehicleColor WHERE ModelID = ? ORDER BY ColorName";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (versionId != null && versionId > 0) ps.setInt(1, versionId); else ps.setInt(1, modelId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) { DTOVehicleColor c = new DTOVehicleColor(); c.setColorID(rs.getInt("ColorID")); c.setColorName(rs.getString("ColorName")); list.add(c);} }
        } catch (SQLException e) { log.error("Error fetching colors model {} version {}", modelId, versionId, e);} return list;
    }

    // ✅ Vehicle detail by VIN (include model/version/color, engine, received, status, price, PO id)
    public java.util.Optional<VehicleInventoryDetail> getDetailByVin(String vin) {
        String sql = """
            SELECT TOP 1 di.DealerInventoryID, di.DealerID, di.VIN, di.ReceivedDate, di.Status AS InventoryStatus,
                   v.VehicleID, v.EngineNumber, v.Status AS VehicleStatus,
                   vv.VersionID, vv.VersionName, vv.Engine, vv.Transmission,
                   vm.ModelID, vm.ModelName, vm.BasePrice, vm.ModelImage,
                   vc.ColorID, vc.ColorName,
                   d.PurchaseOrderID
            FROM DealerInventory di
            LEFT JOIN Vehicle v ON di.VehicleID = v.VehicleID
            LEFT JOIN VehicleVersion vv ON v.VersionID = vv.VersionID
            LEFT JOIN VehicleModel vm ON vv.ModelID = vm.ModelID
            LEFT JOIN VehicleColor vc ON v.ColorID = vc.ColorID
            LEFT JOIN DeliveryDetail dd ON dd.VehicleID = v.VehicleID
            LEFT JOIN Delivery d ON d.DeliveryID = dd.DeliveryID
            WHERE di.VIN = ?
            ORDER BY di.ReceivedDate DESC
        """;
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, vin);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    VehicleInventoryDetail detail = new VehicleInventoryDetail();
                    detail.setVin(rs.getString("VIN"));
                    detail.setDealerInventoryId(rs.getInt("DealerInventoryID"));
                    detail.setDealerId(rs.getInt("DealerID"));
                    detail.setReceivedDate(rs.getDate("ReceivedDate"));
                    detail.setStatus(rs.getString("InventoryStatus"));
                    detail.setVehicleId(rs.getInt("VehicleID"));
                    detail.setEngineNumber(rs.getString("EngineNumber"));
                    detail.setVersionId(rs.getInt("VersionID"));
                    detail.setVersionName(rs.getString("VersionName"));
                    detail.setEngine(rs.getString("Engine"));
                    detail.setTransmission(rs.getString("Transmission"));
                    detail.setModelId(rs.getInt("ModelID"));
                    detail.setModelName(rs.getString("ModelName"));
                    detail.setBasePrice(rs.getBigDecimal("BasePrice"));
                    detail.setColorId(rs.getInt("ColorID"));
                    detail.setColorName(rs.getString("ColorName"));
                    int poId = rs.getInt("PurchaseOrderID");
                    if (!rs.wasNull()) detail.setPurchaseOrderId(poId);
                    // ModelImage is binary; we won't load it into memory here for now
                    return java.util.Optional.of(detail);
                }
            }
        } catch (SQLException e) {
            log.error("Error fetching vehicle detail by VIN {}", vin, e);
        }
        return java.util.Optional.empty();
    }

    // Lightweight DTO for detail view
    public static class VehicleInventoryDetail {
        private int dealerInventoryId; private int dealerId; private String vin; private java.util.Date receivedDate; private String status;
        private Integer vehicleId; private String engineNumber;
        private Integer modelId; private String modelName; private java.math.BigDecimal basePrice;
        private Integer versionId; private String versionName; private String engine; private String transmission;
        private Integer colorId; private String colorName; private Integer purchaseOrderId;
        public int getDealerInventoryId(){return dealerInventoryId;} public void setDealerInventoryId(int v){dealerInventoryId=v;}
        public int getDealerId(){return dealerId;} public void setDealerId(int v){dealerId=v;}
        public String getVin(){return vin;} public void setVin(String v){vin=v;}
        public java.util.Date getReceivedDate(){return receivedDate;} public void setReceivedDate(java.util.Date d){receivedDate=d;}
        public String getStatus(){return status;} public void setStatus(String s){status=s;}
        public Integer getVehicleId(){return vehicleId;} public void setVehicleId(Integer v){vehicleId=v;}
        public String getEngineNumber(){return engineNumber;} public void setEngineNumber(String e){engineNumber=e;}
        public Integer getModelId(){return modelId;} public void setModelId(Integer v){modelId=v;}
        public String getModelName(){return modelName;} public void setModelName(String v){modelName=v;}
        public java.math.BigDecimal getBasePrice(){return basePrice;} public void setBasePrice(java.math.BigDecimal b){basePrice=b;}
        public Integer getVersionId(){return versionId;} public void setVersionId(Integer v){versionId=v;}
        public String getVersionName(){return versionName;} public void setVersionName(String v){versionName=v;}
        public String getEngine(){return engine;} public void setEngine(String e){engine=e;}
        public String getTransmission(){return transmission;} public void setTransmission(String t){transmission=t;}
        public Integer getColorId(){return colorId;} public void setColorId(Integer c){colorId=c;}
        public String getColorName(){return colorName;} public void setColorName(String c){colorName=c;}
        public Integer getPurchaseOrderId(){return purchaseOrderId;} public void setPurchaseOrderId(Integer p){purchaseOrderId=p;}
    }

    // ✅ Chỉ thêm inventory khi đơn hàng hãng đã giao thành công
    public boolean addWhenDeliveryCompleted(int purchaseOrderId, int dealerID, int colorID, int versionID, int quantity) {
        String checkDelivered = "SELECT TOP 1 1 FROM Delivery WHERE PurchaseOrderID = ? AND DeliveryStatus = 'DELIVERED'";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkDelivered)) {
            ps.setInt(1, purchaseOrderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    log.warn("Cannot add inventory: PO {} not delivered", purchaseOrderId);
                    return false;
                }
            }
        } catch (SQLException e) {
            log.error("Failed checking delivery status for PO {}", purchaseOrderId, e);
            return false;
        }

        // ✅ Kiểm tra xem đã có xe được tạo cho PO này chưa (tránh tạo trùng)
        String checkExisting = """
            SELECT COUNT(*) as cnt FROM DealerInventory di
            INNER JOIN Vehicle v ON di.VehicleID = v.VehicleID
            INNER JOIN DeliveryDetail dd ON dd.VehicleID = v.VehicleID
            INNER JOIN Delivery d ON d.DeliveryID = dd.DeliveryID
            WHERE d.PurchaseOrderID = ?
        """;
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkExisting)) {
            ps.setInt(1, purchaseOrderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt("cnt") > 0) {
                    log.info("Vehicles already added to inventory for PO {} - skipping duplicate creation", purchaseOrderId);
                    return true; // Already added, không tạo nữa
                }
            }
        } catch (SQLException e) {
            log.error("Failed checking existing inventory for PO {}", purchaseOrderId, e);
            return false;
        }

        return addVehiclesToInventoryForPO(purchaseOrderId, dealerID, colorID, versionID, quantity);
    }

    // ✅ Chỉ xuất inventory khi đơn bán cho khách hàng đã hoàn tất
    public boolean removeWhenSaleCompleted(int saleOrderId, String vin) {
        String checkCompleted = "SELECT TOP 1 1 FROM SaleOrder WHERE SaleOrderID = ? AND Status = 'COMPLETED'";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkCompleted)) {
            ps.setInt(1, saleOrderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    log.warn("Cannot remove inventory: SaleOrder {} not completed", saleOrderId);
                    return false;
                }
            }
        } catch (SQLException e) {
            log.error("Failed checking sale order status {}", saleOrderId, e);
            return false;
        }
        return removeVehicleByVIN(vin);
    }

    // ✅ Xóa xe khỏi Inventory theo VIN (khi SaleOrder được Confirmed) - DEPRECATED
    @Deprecated
    public boolean removeVehicleByVIN(String vin) {
        String sql = "DELETE FROM DealerInventory WHERE VIN = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, vin);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                log.info("Removed vehicle from inventory VIN={}", vin);
            } else {
                log.warn("No inventory record removed VIN={}", vin);
            }
            return ok;
        } catch (SQLException e) {
            log.error("Error removing vehicle from inventory VIN={}", vin, e);
            return false;
        }
    }

    // ✅ Xóa xe khỏi Inventory theo VehicleID (khi SaleOrder được Confirmed)
    public boolean removeVehicleByID(Integer vehicleID) {
        String sql = "DELETE FROM DealerInventory WHERE VehicleID = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vehicleID);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                log.info("Removed vehicle from inventory by VehicleID={}", vehicleID);
            } else {
                log.warn("No inventory record removed for VehicleID={}", vehicleID);
            }
            return ok;
        } catch (SQLException e) {
            log.error("Error removing vehicle from inventory by VehicleID={} ", vehicleID, e);
            return false;
        }
    }

    // ✅ Thêm xe vào Inventory (khi PurchaseOrder được Approved)
    public boolean addVehiclesToInventory(int dealerID, int colorID, int versionID, int quantity) {
        log.info("Adding {} vehicles to inventory dealerID={}, colorID={}, versionID={}", quantity, dealerID, colorID, versionID);
        if (!validateColorAndVersion(colorID, versionID)) {
            log.warn("Validation failed for colorID={} versionID={}", colorID, versionID);
            return false;
        }
        String sqlInsertVehicle = """
                    INSERT INTO Vehicle (ColorID, VersionID, ManufactureYear, EngineNumber, Status, CreatedAt, UpdatedAt) 
                    VALUES (?, ?, YEAR(GETDATE()), ?, 'IN_STOCK', GETDATE(), GETDATE())
                """;
        String sqlInsertInventory = """
                    INSERT INTO DealerInventory (DealerID, VehicleID, VIN, ReceivedDate, Status) 
                    VALUES (?, ?, ?, GETDATE(), 'AVAILABLE')
                """;
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement psVehicle = conn.prepareStatement(sqlInsertVehicle);
             PreparedStatement psInventory = conn.prepareStatement(sqlInsertInventory)) {
            conn.setAutoCommit(false);
            for (int i = 0; i < quantity; i++) {
                String vin = utils.VINUtils.generateVin(colorID, versionID);
                try (PreparedStatement psVehicleGen = conn.prepareStatement(
                        "INSERT INTO Vehicle (ColorID, VersionID, ManufactureYear, EngineNumber, Status, CreatedAt, UpdatedAt) VALUES (?, ?, YEAR(GETDATE()), ?, 'IN_STOCK', GETDATE(), GETDATE())",
                        Statement.RETURN_GENERATED_KEYS)) {
                    psVehicleGen.setInt(1, colorID);
                    psVehicleGen.setInt(2, versionID);
                    psVehicleGen.setString(3, "ENG" + System.currentTimeMillis() + i);
                    psVehicleGen.executeUpdate();
                    Integer vehicleId = null;
                    try (ResultSet rs = psVehicleGen.getGeneratedKeys()) {
                        if (rs.next()) vehicleId = rs.getInt(1);
                    }
                    psInventory.setInt(1, dealerID);
                    if (vehicleId != null) psInventory.setInt(2, vehicleId); else psInventory.setNull(2, Types.INTEGER);
                    psInventory.setString(3, vin);
                    psInventory.executeUpdate();
                }
            }
            conn.commit();
            log.info("Successfully added {} vehicles to inventory dealerID={}", quantity, dealerID);
            return true;
        } catch (SQLException e) {
            log.error("Error adding vehicles to inventory dealerID={}", dealerID, e);
            return false;
        }
    }

    // ✅ Thêm xe vào Inventory và gắn DeliveryDetail tới PO cụ thể
    private boolean addVehiclesToInventoryForPO(int purchaseOrderId, int dealerID, int colorID, int versionID, int quantity) {
        log.info("Adding {} vehicles to inventory dealerID={}, colorID={}, versionID={} for PO {}", quantity, dealerID, colorID, versionID, purchaseOrderId);
        if (!validateColorAndVersion(colorID, versionID)) {
            log.warn("Validation failed for colorID={} versionID={}", colorID, versionID);
            return false;
        }

        // ✅ Lấy UnitPrice (giá sau chiết khấu) từ PurchaseOrderDetail
        java.math.BigDecimal costPrice = getCostPriceFromPO(purchaseOrderId, colorID, versionID);

        String sqlInsertVehicle = """
                    INSERT INTO Vehicle (ColorID, VersionID, ManufactureYear, EngineNumber, Status, CreatedAt, UpdatedAt) 
                    VALUES (?, ?, YEAR(GETDATE()), ?, 'IN_STOCK', GETDATE(), GETDATE())
                """;
        String sqlInsertInventory = """
                    INSERT INTO DealerInventory (DealerID, VehicleID, VIN, ReceivedDate, Status, CostPrice) 
                    VALUES (?, ?, ?, GETDATE(), 'AVAILABLE', ?)
                """;
        String sqlGetLatestDelivery = "SELECT TOP 1 DeliveryID FROM Delivery WHERE PurchaseOrderID = ? ORDER BY DeliveryID DESC";
        String sqlInsertDeliveryDetail = "INSERT INTO DeliveryDetail (DeliveryID, VehicleID) VALUES (?, ?)";
        try (Connection conn = DBUtils.getConnection()) {
            conn.setAutoCommit(false);

            Integer deliveryId = null;
            try (PreparedStatement psGetDel = conn.prepareStatement(sqlGetLatestDelivery)) {
                psGetDel.setInt(1, purchaseOrderId);
                try (ResultSet rs = psGetDel.executeQuery()) {
                    if (rs.next()) deliveryId = rs.getInt("DeliveryID");
                }
            }

            for (int i = 0; i < quantity; i++) {
                String vin = utils.VINUtils.generateVin(colorID, versionID);
                Integer vehicleId = null;
                try (PreparedStatement psVehicleGen = conn.prepareStatement(sqlInsertVehicle, Statement.RETURN_GENERATED_KEYS)) {
                    psVehicleGen.setInt(1, colorID);
                    psVehicleGen.setInt(2, versionID);
                    psVehicleGen.setString(3, "ENG" + System.currentTimeMillis() + i);
                    psVehicleGen.executeUpdate();
                    try (ResultSet rs = psVehicleGen.getGeneratedKeys()) {
                        if (rs.next()) vehicleId = rs.getInt(1);
                    }
                }

                try (PreparedStatement psInventory = conn.prepareStatement(sqlInsertInventory)) {
                    psInventory.setInt(1, dealerID);
                    if (vehicleId != null) psInventory.setInt(2, vehicleId); else psInventory.setNull(2, Types.INTEGER);
                    psInventory.setString(3, vin);
                    // ✅ Lưu giá cost (sau chiết khấu)
                    psInventory.setBigDecimal(4, costPrice);
                    psInventory.executeUpdate();
                }

                if (deliveryId != null && vehicleId != null) {
                    try (PreparedStatement psDelDetail = conn.prepareStatement(sqlInsertDeliveryDetail)) {
                        psDelDetail.setInt(1, deliveryId);
                        psDelDetail.setInt(2, vehicleId);
                        psDelDetail.executeUpdate();
                    }
                }
            }
            conn.commit();
            log.info("Successfully added {} vehicles to inventory dealerID={} for PO {} with costPrice={}", quantity, dealerID, purchaseOrderId, costPrice);
            return true;
        } catch (SQLException e) {
            log.error("Error adding vehicles to inventory dealerID={} for PO {}", dealerID, purchaseOrderId, e);
            return false;
        }
    }

    // ✅ Validate ColorID và VersionID tồn tại trong database
    private boolean validateColorAndVersion(int colorID, int versionID) {
        String sqlCheckColor = "SELECT COUNT(*) FROM VehicleColor WHERE ColorID = ?";
        String sqlCheckVersion = "SELECT COUNT(*) FROM VehicleVersion WHERE VersionID = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement psColor = conn.prepareStatement(sqlCheckColor);
             PreparedStatement psVersion = conn.prepareStatement(sqlCheckVersion)) {
            psColor.setInt(1, colorID);
            try (ResultSet rs = psColor.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    return false;
                }
            }
            psVersion.setInt(1, versionID);
            try (ResultSet rs = psVersion.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    return false;
                }
            }
            return true;
        } catch (SQLException e) {
            log.error("Error validating ColorID={} VersionID={}", colorID, versionID, e);
            return false;
        }
    }

    /**
     * Lấy giá cost (UnitPrice sau chiết khấu) từ PurchaseOrderDetail
     * @param purchaseOrderId ID của Purchase Order
     * @param colorID ColorID của xe
     * @param versionID VersionID của xe
     * @return UnitPrice đã chiết khấu, hoặc BasePrice nếu không tìm thấy
     */
    private java.math.BigDecimal getCostPriceFromPO(int purchaseOrderId, int colorID, int versionID) {
        String sql = """
            SELECT pod.UnitPrice
            FROM PurchaseOrderDetail pod
            WHERE pod.PurchaseOrderID = ?
              AND pod.ColorID = ?
              AND pod.VersionID = ?
        """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, purchaseOrderId);
            ps.setInt(2, colorID);
            ps.setInt(3, versionID);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    java.math.BigDecimal unitPrice = rs.getBigDecimal("UnitPrice");
                    if (unitPrice != null && unitPrice.compareTo(java.math.BigDecimal.ZERO) > 0) {
                        log.debug("Found cost price from PO {}: {}", purchaseOrderId, unitPrice);
                        return unitPrice;
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Error getting cost price from PO {} for color {} version {}", purchaseOrderId, colorID, versionID, e);
        }

        // Fallback: Lấy BasePrice từ VehicleModel
        log.warn("No unit price found in PO {}, falling back to BasePrice", purchaseOrderId);
        return getBasePriceFromVersion(versionID);
    }

    /**
     * Lấy CostPrice của xe từ DealerInventory theo VehicleID
     * Dùng khi tạo Sale Order để lấy giá gốc (đã chiết khấu từ EVM)
     * @param vehicleId ID của xe
     * @return CostPrice, hoặc BasePrice nếu không tìm thấy
     */
    public java.math.BigDecimal getCostPriceByVehicleId(int vehicleId) {
        String sql = """
            SELECT di.CostPrice
            FROM DealerInventory di
            WHERE di.VehicleID = ?
        """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vehicleId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    java.math.BigDecimal costPrice = rs.getBigDecimal("CostPrice");
                    if (costPrice != null && costPrice.compareTo(java.math.BigDecimal.ZERO) > 0) {
                        log.debug("Found cost price for vehicle {}: {}", vehicleId, costPrice);
                        return costPrice;
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Error getting cost price for vehicle {}", vehicleId, e);
        }

        // Fallback: Lấy BasePrice từ VehicleModel qua Vehicle
        log.warn("No cost price found for vehicle {}, falling back to BasePrice", vehicleId);
        return getBasePriceForVehicle(vehicleId);
    }

    /**
     * Lấy BasePrice từ VehicleModel cho một vehicle cụ thể (fallback)
     */
    private java.math.BigDecimal getBasePriceForVehicle(int vehicleId) {
        String sql = """
            SELECT vm.BasePrice
            FROM Vehicle v
            JOIN VehicleVersion vv ON v.VersionID = vv.VersionID
            JOIN VehicleModel vm ON vv.ModelID = vm.ModelID
            WHERE v.VehicleID = ?
        """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vehicleId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("BasePrice");
                }
            }
        } catch (SQLException e) {
            log.error("Error getting base price for vehicle {}", vehicleId, e);
        }

        return java.math.BigDecimal.ZERO;
    }

    /**
     * Lấy BasePrice từ VehicleModel thông qua VersionID (fallback)
     */
    private java.math.BigDecimal getBasePriceFromVersion(int versionID) {
        String sql = """
            SELECT vm.BasePrice
            FROM VehicleVersion vv
            JOIN VehicleModel vm ON vv.ModelID = vm.ModelID
            WHERE vv.VersionID = ?
        """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, versionID);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("BasePrice");
                }
            }
        } catch (SQLException e) {
            log.error("Error getting base price for version {}", versionID, e);
        }

        return java.math.BigDecimal.ZERO;
    }

    // VIN generation moved to utils.VINUtils

    /**
     * ✅ Lấy danh sách VehicleID AVAILABLE từ Inventory theo VersionID và ColorID
     * Dùng cho Sale Order creation - lấy xe từ kho dealer thay vì tạo mới
     * @param dealerID ID của dealer
     * @param versionID Version của xe
     * @param colorID Màu của xe
     * @param limit Số lượng xe cần lấy
     * @return Danh sách VehicleID available trong inventory
     */
    public List<Integer> getAvailableVehicleIdsFromInventory(int dealerID, int versionID, int colorID, int limit) {
        List<Integer> vehicleIds = new ArrayList<>();
        if (limit <= 0) {
            log.warn("Invalid limit {} for getAvailableVehicleIdsFromInventory", limit);
            return vehicleIds;
        }

        String sql = """
            SELECT TOP (?) di.VehicleID
            FROM DealerInventory di
            JOIN Vehicle v ON di.VehicleID = v.VehicleID
            WHERE di.DealerID = ?
              AND di.Status = 'AVAILABLE'
              AND v.VersionID = ?
              AND v.ColorID = ?
            ORDER BY di.ReceivedDate ASC
        """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, dealerID);
            ps.setInt(3, versionID);
            ps.setInt(4, colorID);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    vehicleIds.add(rs.getInt("VehicleID"));
                }
            }

            log.info("Found {} available vehicles in inventory for dealer={} version={} color={}",
                     vehicleIds.size(), dealerID, versionID, colorID);

        } catch (SQLException e) {
            log.error("Error getting available vehicles from inventory dealer={} version={} color={}",
                      dealerID, versionID, colorID, e);
        }

        return vehicleIds;
    }

    /**
     * ✅ Reserve xe trong inventory (đánh dấu là RESERVED khi tạo Sale Order)
     * @param vehicleID ID của xe cần reserve
     * @return true nếu thành công
     */
    public boolean reserveVehicle(int vehicleID) {
        String sql = "UPDATE DealerInventory SET Status = 'RESERVED' WHERE VehicleID = ? AND Status = 'AVAILABLE'";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vehicleID);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                log.info("✅ Reserved vehicle in inventory VehicleID={}", vehicleID);
                return true;
            } else {
                log.warn("⚠️ Failed to reserve vehicle VehicleID={} (may not be AVAILABLE)", vehicleID);
                return false;
            }
        } catch (SQLException e) {
            log.error("Error reserving vehicle VehicleID={}", vehicleID, e);
            return false;
        }
    }

    /**
     * ✅ Hoàn trả xe về inventory (đánh dấu lại AVAILABLE khi Sale Order bị CANCEL)
     * @param vehicleID ID của xe cần hoàn trả
     * @return true nếu thành công
     */
    public boolean returnVehicleToInventory(int vehicleID) {
        String sql = "UPDATE DealerInventory SET Status = 'AVAILABLE' WHERE VehicleID = ? AND Status IN ('RESERVED', 'SOLD')";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vehicleID);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                log.info("✅ Returned vehicle to inventory VehicleID={}", vehicleID);
                return true;
            } else {
                log.warn("⚠️ Failed to return vehicle VehicleID={} (may not exist in inventory)", vehicleID);
                return false;
            }
        } catch (SQLException e) {
            log.error("Error returning vehicle to inventory VehicleID={}", vehicleID, e);
            return false;
        }
    }

    /**
     * ✅ Đánh dấu xe là SOLD trong inventory (khi Sale Order hoàn thành)
     * @param vehicleID ID của xe
     * @return true nếu thành công
     */
    public boolean markVehicleAsSold(int vehicleID) {
        String sql = "UPDATE DealerInventory SET Status = 'SOLD' WHERE VehicleID = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vehicleID);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                log.info("✅ Marked vehicle as SOLD VehicleID={}", vehicleID);
                return true;
            } else {
                log.warn("⚠️ Failed to mark vehicle as SOLD VehicleID={}", vehicleID);
                return false;
            }
        } catch (SQLException e) {
            log.error("Error marking vehicle as SOLD VehicleID={}", vehicleID, e);
            return false;
        }
    }

    /**
     * ✅ Lấy VIN của xe từ inventory theo VehicleID
     * @param vehicleID ID của xe
     * @return VIN hoặc null nếu không tìm thấy
     */
    public String getVINByVehicleId(int vehicleID) {
        String sql = "SELECT VIN FROM DealerInventory WHERE VehicleID = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vehicleID);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("VIN");
                }
            }
        } catch (SQLException e) {
            log.error("Error getting VIN for VehicleID={}", vehicleID, e);
        }

        return null;
    }
}
