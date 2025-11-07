package com.dealermanagementsysstem.project.Model;

import org.springframework.stereotype.Repository;
import utils.DBUtils;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class DAOPurchaseOrder {

    // 🔹 Lấy danh sách tất cả PurchaseOrders
    public List<DTOPurchaseOrder> getAllPurchaseOrders() {
        List<DTOPurchaseOrder> list = new ArrayList<>();
        String sql = """
                SELECT po.PurchaseOrderID, po.DealerID, po.StaffID, po.CreatedAt, po.Status, po.TotalAmount, po.EvmID,
                       d.DealerID, d.DealerName, d.LevelID, d.Phone AS DealerPhone, d.Address AS DealerAddress, d.Email AS DealerEmail,
                       dl.LevelName,
                       ds.StaffID, ds.FullName AS StaffName, ds.Position AS StaffPosition,
                       adj.DiscountPercent AS AdjustmentDiscountPercent, adj.PromotionName AS AdjustmentPromotionName,
                       pol.PolicyName, pol.HangPercent, pol.DailyPercent,
                       dly.DeliveryDate AS LatestDeliveryDate, dly.DeliveryStatus AS LatestDeliveryStatus
                FROM PurchaseOrder po
                LEFT JOIN Dealer d ON po.DealerID = d.DealerID
                LEFT JOIN DealerStaff ds ON po.StaffID = ds.StaffID
                LEFT JOIN DealerLevel dl ON d.LevelID = dl.LevelID
                OUTER APPLY (
                   SELECT TOP 1 DiscountPercent, PromotionName
                   FROM DealerPriceAdjustment p
                   WHERE p.DealerID = d.DealerID
                     AND p.StartDate <= GETDATE()
                     AND (p.EndDate IS NULL OR p.EndDate >= GETDATE())
                   ORDER BY p.StartDate DESC
                ) adj
                OUTER APPLY (
                   SELECT TOP 1 PolicyName, HangPercent, DailyPercent
                   FROM DiscountPolicy dp
                   WHERE dp.DealerID = d.DealerID
                   ORDER BY dp.CreatedAt DESC
                ) pol
                OUTER APPLY (
                   SELECT TOP 1 DeliveryDate, DeliveryStatus
                   FROM Delivery dv
                   WHERE dv.PurchaseOrderID = po.PurchaseOrderID
                   ORDER BY dv.DeliveryID DESC
                ) dly
                ORDER BY po.PurchaseOrderID DESC
                """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                DTOPurchaseOrder dto = new DTOPurchaseOrder();
                dto.setPurchaseOrderId(rs.getInt("PurchaseOrderID"));
                dto.setCreatedAt(rs.getTimestamp("CreatedAt"));
                try { dto.setStatus(PurchaseOrderStatus.valueOf(rs.getString("Status").toUpperCase())); } catch (IllegalArgumentException ex) { dto.setStatus(PurchaseOrderStatus.REQUESTED); }
                dto.setTotalAmount(rs.getBigDecimal("TotalAmount"));
                dto.setEvmID(rs.getInt("EvmID"));
                DTODealer dealer = new DTODealer();
                dealer.setDealerID(rs.getInt("DealerID"));
                dealer.setDealerName(rs.getString("DealerName"));
                dealer.setPhone(rs.getString("DealerPhone"));
                dealer.setAddress(rs.getString("DealerAddress"));
                dealer.setEmail(rs.getString("DealerEmail"));
                dto.setDealer(dealer);
                dto.setDealerName(dealer.getDealerName());
                String levelName = rs.getString("LevelName");
                dto.setDealerLevelName(levelName != null ? levelName : ("Level " + rs.getInt("LevelID"))); // fallback
                DTODealerStaff staff = new DTODealerStaff();
                staff.setStaffID(rs.getInt("StaffID"));
                staff.setFullName(rs.getString("StaffName"));
                staff.setPosition(rs.getString("StaffPosition"));
                dto.setStaff(staff);
                // Policy from DiscountPolicy
                dto.setPolicyName(rs.getString("PolicyName"));

                // Set policy discount percent (fallback strategy since DiscountPercent column may not exist)
                Double adjustmentDiscount = rs.getObject("AdjustmentDiscountPercent", Double.class);
                Double hangPercent = rs.getObject("HangPercent", Double.class);
                Double dailyPercent = rs.getObject("DailyPercent", Double.class);

                // Use adjustment discount if available, else fallback to HangPercent
                if (adjustmentDiscount != null) {
                    dto.setPolicyDiscountPercent(adjustmentDiscount);
                } else if (hangPercent != null) {
                    dto.setPolicyDiscountPercent(hangPercent);
                }

                // Set dealer reward and manufacturer share
                dto.setDealerRewardPercent(dailyPercent != null ? dailyPercent : 5.0);
                dto.setManufacturerSharePercent(hangPercent != null ? hangPercent : 95.0);

                if (dto.getStatus() == PurchaseOrderStatus.APPROVED) dto.setApprovedByStaffName(staff.getFullName());
                // hydrate delivery summary
                Timestamp deliveryDate = rs.getTimestamp("LatestDeliveryDate");
                if (deliveryDate != null) dto.setPlannedDeliveryDate(deliveryDate);
                String delStatus = rs.getString("LatestDeliveryStatus");
                if (delStatus != null) dto.setShippingStatus(delStatus);
                list.add(dto);
            }

        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // 🔹 Lấy 1 đơn hàng theo ID (kèm chi tiết)
    public DTOPurchaseOrder getPurchaseOrderById(int id) {
        String sqlOrder = """
                SELECT po.PurchaseOrderID, po.DealerID, po.StaffID, po.CreatedAt, po.Status, po.TotalAmount, po.EvmID,
                       d.DealerID, d.DealerName, d.Address AS DealerAddress, d.Phone AS DealerPhone, d.Email AS DealerEmail, d.LevelID,
                       dl.LevelName,
                       ds.StaffID, ds.FullName AS StaffName, ds.Position AS StaffPosition,
                       adj.DiscountPercent AS AdjustmentDiscountPercent, adj.PromotionName AS AdjustmentPromotionName,
                       pol.PolicyName, pol.HangPercent, pol.DailyPercent,
                       dly.DeliveryDate AS LatestDeliveryDate, dly.DeliveryStatus AS LatestDeliveryStatus
                FROM PurchaseOrder po
                LEFT JOIN Dealer d ON po.DealerID = d.DealerID
                LEFT JOIN DealerStaff ds ON po.StaffID = ds.StaffID
                LEFT JOIN DealerLevel dl ON d.LevelID = dl.LevelID
                OUTER APPLY (
                   SELECT TOP 1 DiscountPercent, PromotionName
                   FROM DealerPriceAdjustment p
                   WHERE p.DealerID = d.DealerID
                     AND p.StartDate <= GETDATE()
                     AND (p.EndDate IS NULL OR p.EndDate >= GETDATE())
                   ORDER BY p.StartDate DESC
                ) adj
                OUTER APPLY (
                   SELECT TOP 1 PolicyName, HangPercent, DailyPercent
                   FROM DiscountPolicy dp
                   WHERE dp.DealerID = d.DealerID
                   ORDER BY dp.CreatedAt DESC
                ) pol
                OUTER APPLY (
                   SELECT TOP 1 DeliveryDate, DeliveryStatus
                   FROM Delivery dv
                   WHERE dv.PurchaseOrderID = po.PurchaseOrderID
                   ORDER BY dv.DeliveryID DESC
                ) dly
                WHERE po.PurchaseOrderID = ?
                """;

        String sqlDetail = """
                SELECT pod.PODetailID, pod.PurchaseOrderID, pod.ColorID, pod.VersionID, pod.UnitPrice, pod.Quantity, pod.Subtotal, pod.PaymentStatus,
                       vc.ColorID AS DetailColorID, vc.ColorName,
                       vv.VersionID AS DetailVersionID, vv.VersionName,
                       vm.ModelID, vm.ModelName, vm.BasePrice,
                       pol.HangPercent AS DiscountPercent
                FROM PurchaseOrderDetail pod
                LEFT JOIN VehicleColor vc ON pod.ColorID = vc.ColorID
                LEFT JOIN VehicleVersion vv ON pod.VersionID = vv.VersionID
                LEFT JOIN VehicleModel vm ON vv.ModelID = vm.ModelID
                LEFT JOIN PurchaseOrder po ON pod.PurchaseOrderID = po.PurchaseOrderID
                LEFT JOIN Dealer d ON po.DealerID = d.DealerID
                OUTER APPLY (
                   SELECT TOP 1 HangPercent
                   FROM DiscountPolicy dp
                   WHERE dp.DealerID = d.DealerID
                   ORDER BY dp.CreatedAt DESC
                ) pol
                WHERE pod.PurchaseOrderID = ?
                ORDER BY pod.PODetailID ASC
                """;

        try (Connection conn = DBUtils.getConnection(); PreparedStatement psOrder = conn.prepareStatement(sqlOrder)) {

            psOrder.setInt(1, id);
            try (ResultSet rs = psOrder.executeQuery()) {
                if (rs.next()) {
                    DTOPurchaseOrder dto = new DTOPurchaseOrder();
                    dto.setPurchaseOrderId(rs.getInt("PurchaseOrderID"));
                    dto.setCreatedAt(rs.getTimestamp("CreatedAt"));
                    try { dto.setStatus(PurchaseOrderStatus.valueOf(rs.getString("Status").toUpperCase())); } catch (IllegalArgumentException ex){ dto.setStatus(PurchaseOrderStatus.REQUESTED);}
                    dto.setTotalAmount(rs.getBigDecimal("TotalAmount"));
                    dto.setEvmID(rs.getInt("EvmID"));

                    // Dealer info
                    DTODealer dealer = new DTODealer();
                    dealer.setDealerID(rs.getInt("DealerID"));
                    dealer.setDealerName(rs.getString("DealerName"));
                    dealer.setAddress(rs.getString("DealerAddress"));
                    dealer.setPhone(rs.getString("DealerPhone"));
                    dealer.setEmail(rs.getString("DealerEmail"));
                    dto.setDealer(dealer);
                    dto.setDealerName(dealer.getDealerName());
                    String levelName = rs.getString("LevelName");
                    dto.setDealerLevelName(levelName != null ? levelName : ("Level " + rs.getInt("LevelID"))); // fallback

                    // Staff info
                    DTODealerStaff staff = new DTODealerStaff();
                    staff.setStaffID(rs.getInt("StaffID"));
                    staff.setFullName(rs.getString("StaffName"));
                    staff.setPosition(rs.getString("StaffPosition"));
                    dto.setStaff(staff);

                    // Promotion / Policy info
                    dto.setPolicyName(rs.getString("PolicyName"));

                    // Get discount-related percentages (DiscountPercent not in query)
                    Double adjustmentDiscount = rs.getObject("AdjustmentDiscountPercent", Double.class);
                    Double hangPercent = rs.getObject("HangPercent", Double.class);
                    Double dailyPercent = rs.getObject("DailyPercent", Double.class);

                    // Set policyDiscountPercent: Adjustment > HangPercent fallback
                    if (adjustmentDiscount != null) {
                        dto.setPolicyDiscountPercent(adjustmentDiscount);
                    } else if (hangPercent != null) {
                        dto.setPolicyDiscountPercent(hangPercent);
                    }

                    // Set dealer reward and manufacturer share percentages
                    dto.setDealerRewardPercent(dailyPercent != null ? dailyPercent : 5.0);
                    dto.setManufacturerSharePercent(hangPercent != null ? hangPercent : 95.0);

                    if (dto.getStatus() == PurchaseOrderStatus.APPROVED) dto.setApprovedByStaffName(staff.getFullName());

                    // Details
                    try (PreparedStatement psDetail = conn.prepareStatement(sqlDetail)) {
                        psDetail.setInt(1, id);
                        try (ResultSet drs = psDetail.executeQuery()) {
                            List<DTOPurchaseOrderDetail> details = new ArrayList<>();
                            while (drs.next()) {
                                DTOPurchaseOrderDetail d = new DTOPurchaseOrderDetail();
                                d.setPoDetailId(drs.getInt("PODetailID"));
                                d.setPurchaseOrder(dto);
                                d.setUnitPrice(drs.getBigDecimal("UnitPrice"));
                                d.setQuantity(drs.getInt("Quantity"));
                                d.setSubtotal(drs.getBigDecimal("Subtotal"));

                                // ✅ Set PaymentStatus
                                String paymentStatus = drs.getString("PaymentStatus");
                                d.setPaymentStatus(paymentStatus != null ? paymentStatus : "UNPAID");

                                // ✅ Set BasePrice (giá gốc)
                                BigDecimal basePrice = drs.getBigDecimal("BasePrice");
                                d.setBasePrice(basePrice);

                                // ✅ Set DiscountPercent (% chiết khấu)
                                Double discountPct = drs.getObject("DiscountPercent", Double.class);
                                d.setDiscountPercent(discountPct);

                                // ✅ Tính DiscountAmount (số tiền chiết khấu)
                                if (basePrice != null && discountPct != null && discountPct > 0) {
                                    BigDecimal discountAmount = basePrice.multiply(BigDecimal.valueOf(discountPct / 100.0));
                                    d.setDiscountAmount(discountAmount);
                                } else {
                                    d.setDiscountAmount(BigDecimal.ZERO);
                                }

                                // Color
                                if (drs.getString("ColorName") != null) {
                                    DTOVehicleColor color = new DTOVehicleColor();
                                    color.setColorID(drs.getInt("DetailColorID"));
                                    color.setColorName(drs.getString("ColorName"));
                                    d.setColor(color);
                                    d.setColorName(color.getColorName());
                                }
                                // Version + Model
                                if (drs.getString("VersionName") != null) {
                                    DTOVehicleVersion version = new DTOVehicleVersion();
                                    version.setVersionID(drs.getInt("DetailVersionID"));
                                    version.setVersionName(drs.getString("VersionName"));
                                    if (drs.getString("ModelName") != null) {
                                        DTOVehicleModel model = new DTOVehicleModel();
                                        model.setModelID(drs.getInt("ModelID"));
                                        model.setModelName(drs.getString("ModelName"));
                                        model.setBasePrice(drs.getBigDecimal("BasePrice"));
                                        version.setModel(model);
                                        d.setModelName(model.getModelName());
                                    }
                                    d.setVersion(version);
                                    d.setVersionName(version.getVersionName());
                                }
                                details.add(d);
                            }
                            dto.setOrderDetails(details);
                            // Set total quantity
                            int totalQty = details.stream().mapToInt(DTOPurchaseOrderDetail::getQuantity).sum();
                            dto.setTotalQuantity(totalQty);
                            // After setting details and totalQuantity, set primary detail fields:
                            if (!details.isEmpty()) {
                                DTOPurchaseOrderDetail firstDetail = details.get(0);
                                dto.setPrimaryModelName(firstDetail.getModelName() != null ? firstDetail.getModelName() : (firstDetail.getVersion()!=null && firstDetail.getVersion().getModel()!=null ? firstDetail.getVersion().getModel().getModelName() : null));
                                dto.setPrimaryVersionName(firstDetail.getVersionName()!=null ? firstDetail.getVersionName() : (firstDetail.getVersion()!=null ? firstDetail.getVersion().getVersionName() : null));
                                dto.setPrimaryColorName(firstDetail.getColorName()!=null ? firstDetail.getColorName() : (firstDetail.getColor()!=null ? firstDetail.getColor().getColorName() : null));
                                dto.setPrimaryUnitPrice(firstDetail.getUnitPrice());
                                dto.setPrimarySubtotal(firstDetail.getSubtotal());
                            }
                            // Sync totalAmount with sum of subtotals if present
                            BigDecimal sumSubtotal = details.stream()
                                    .map(DTOPurchaseOrderDetail::getSubtotal)
                                    .filter(s -> s != null)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                            if (sumSubtotal.compareTo(BigDecimal.ZERO) > 0 && dto.getTotalAmount() != null && dto.getTotalAmount().compareTo(sumSubtotal) != 0) {
                                dto.setTotalAmount(sumSubtotal); // authoritative discounted total
                            }
                        }
                    }
                    // hydrate delivery info
                    dto.setPlannedDeliveryDate(rs.getTimestamp("LatestDeliveryDate"));
                    dto.setShippingStatus(rs.getString("LatestDeliveryStatus"));
                    if ("DELIVERED".equals(rs.getString("LatestDeliveryStatus"))) {
                        dto.setActualDeliveryDate(rs.getTimestamp("LatestDeliveryDate"));
                        if (dto.getStatus() != PurchaseOrderStatus.CANCELLED) dto.setStatus(PurchaseOrderStatus.DELIVERED);
                    }
                    return dto;
                }
            }

        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    private BigDecimal recalcUnitPrice(DTOPurchaseOrderDetail det) {
        if (det.getVersion() == null || det.getVersion().getVersionID() == 0) return BigDecimal.ZERO;
        String sql = """
            SELECT vm.BasePrice, adj.DiscountPercent
            FROM VehicleVersion vv
            JOIN VehicleModel vm ON vv.ModelID = vm.ModelID
            OUTER APPLY (
                SELECT TOP 1 DiscountPercent
                FROM DealerPriceAdjustment adj
                WHERE adj.ModelID = vm.ModelID
                  AND adj.StartDate <= GETDATE()
                  AND (adj.EndDate IS NULL OR adj.EndDate >= GETDATE())
                ORDER BY adj.StartDate DESC
            ) adj
            WHERE vv.VersionID = ?
        """;
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, det.getVersion().getVersionID());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal base = rs.getBigDecimal("BasePrice");
                    Double disc = rs.getObject("DiscountPercent", Double.class);
                    if (base == null) return BigDecimal.ZERO;
                    if (disc != null && disc > 0) {
                        return base.subtract(base.multiply(BigDecimal.valueOf(disc / 100.0)));
                    }
                    return base;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return BigDecimal.ZERO;
    }

    private void updateDetailPrice(int detailId, BigDecimal unitPrice, BigDecimal subtotal) {
        String sql = "UPDATE PurchaseOrderDetail SET UnitPrice = ?, Subtotal = ? WHERE PODetailID = ?";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, unitPrice);
            ps.setBigDecimal(2, subtotal);
            ps.setInt(3, detailId);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // 🔹 Cập nhật trạng thái đơn hàng
    public boolean updatePurchaseOrderStatus(int id, PurchaseOrderStatus newStatus) {
        String sql = "UPDATE PurchaseOrder SET Status = ? WHERE PurchaseOrderID = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus.toString());
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 🔹 Xoá đơn hàng
    public int deletePurchaseOrder(int id) {
        String sql = "DELETE FROM PurchaseOrder WHERE PurchaseOrderID = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // 🔹 Thêm đơn hàng mới và trả về ID
    public int insertPurchaseOrder(DTOPurchaseOrder order) {
        String sql = "INSERT INTO PurchaseOrder (DealerID, StaffID, CreatedAt, Status, TotalAmount, EvmID) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, order.getDealer().getDealerID());
            ps.setInt(2, order.getStaff().getStaffID());
            ps.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            ps.setString(4, order.getStatus().toString());
            ps.setBigDecimal(5, order.getTotalAmount());
            ps.setInt(6, order.getEvmID());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // ✅ Lấy DealerID theo email từ Account → DealerStaff → Dealer
    // KHÔNG tự động tạo dealer mới (đã fix bug tạo dealer ID=13 thay vì dùng ID=12)
    public int getDealerIdByEmail(String email) {
        // ✅ Tìm theo quan hệ Account → DealerStaff → Dealer (ĐÚNG)
        String sql = """
            SELECT d.DealerID
            FROM Account a
            JOIN DealerStaff ds ON ds.AccountID = a.AccountID
            JOIN Dealer d ON d.DealerID = ds.DealerID
            WHERE a.Email = ?
            """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int dealerId = rs.getInt("DealerID");
                    System.out.println("✅ Found DealerID=" + dealerId + " for email=" + email);
                    return dealerId;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting DealerID for email: " + email);
            e.printStackTrace();
        }

        // ❌ KHÔNG tìm thấy dealer → Trả về -1 (không tự động tạo nữa)
        System.err.println("⚠️ No dealer found for email: " + email + ". Cannot create purchase order.");
        return -1;
    }

    // ✅ Lấy StaffID theo email từ Account → DealerStaff
    // KHÔNG tự động tạo staff mới (đã fix bug)
    public int getStaffIdByEmail(String email) {
        // ✅ Tìm theo quan hệ Account → DealerStaff (ĐÚNG)
        String sql = """
            SELECT ds.StaffID
            FROM Account a
            JOIN DealerStaff ds ON ds.AccountID = a.AccountID
            WHERE a.Email = ?
            """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int staffId = rs.getInt("StaffID");
                    System.out.println("✅ Found StaffID=" + staffId + " for email=" + email);
                    return staffId;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting StaffID for email: " + email);
            e.printStackTrace();
        }

        // ❌ KHÔNG tìm thấy staff → Trả về -1 (không tự động tạo nữa)
        System.err.println("⚠️ No staff found for email: " + email + ". Cannot create purchase order.");
        return -1;
    }

    // 🔹 Lấy danh sách đơn hàng theo DealerID
    public List<DTOPurchaseOrder> getPurchaseOrdersByDealerId(int dealerId) {
        List<DTOPurchaseOrder> list = new ArrayList<>();
        String sql = """
            SELECT po.PurchaseOrderID, po.DealerID, po.StaffID, po.CreatedAt, po.Status, po.TotalAmount, po.EvmID,
                   d.DealerID, d.DealerName, dl.LevelName,
                   ds.StaffID, ds.FullName AS StaffName,
                   promo.DiscountPercent AS PolicyDiscountPercent, promo.PromotionName AS PolicyName
            FROM PurchaseOrder po
            LEFT JOIN Dealer d ON po.DealerID = d.DealerID
            LEFT JOIN DealerStaff ds ON po.StaffID = ds.StaffID
            LEFT JOIN DealerLevel dl ON d.LevelID = dl.LevelID
            OUTER APPLY (
                   SELECT TOP 1 DiscountPercent, PromotionName
                   FROM DealerPriceAdjustment p
                   WHERE p.DealerID = d.DealerID
                     AND p.StartDate <= GETDATE()
                     AND (p.EndDate IS NULL OR p.EndDate >= GETDATE())
                   ORDER BY p.StartDate DESC
            ) promo
            WHERE po.DealerID = ?
            ORDER BY po.PurchaseOrderID DESC
            """;
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dealerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DTOPurchaseOrder dto = new DTOPurchaseOrder();
                    dto.setPurchaseOrderId(rs.getInt("PurchaseOrderID"));
                    dto.setCreatedAt(rs.getTimestamp("CreatedAt"));
                    try { dto.setStatus(PurchaseOrderStatus.valueOf(rs.getString("Status").toUpperCase())); } catch (IllegalArgumentException ex){ dto.setStatus(PurchaseOrderStatus.REQUESTED);}
                    dto.setTotalAmount(rs.getBigDecimal("TotalAmount"));
                    dto.setEvmID(rs.getInt("EvmID"));
                    DTODealer dealer = new DTODealer();
                    dealer.setDealerID(rs.getInt("DealerID"));
                    dealer.setDealerName(rs.getString("DealerName"));
                    dto.setDealer(dealer);
                    dto.setDealerName(dealer.getDealerName());
                    dto.setDealerLevelName(rs.getString("LevelName"));
                    DTODealerStaff staff = new DTODealerStaff();
                    staff.setStaffID(rs.getInt("StaffID"));
                    staff.setFullName(rs.getString("StaffName"));
                    dto.setStaff(staff);
                    dto.setPolicyName(rs.getString("PolicyName"));
                    dto.setPolicyDiscountPercent(rs.getObject("PolicyDiscountPercent", Double.class));
                    if (dto.getStatus() == PurchaseOrderStatus.APPROVED) dto.setApprovedByStaffName(staff.getFullName());
                    list.add(dto);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public BigDecimal getBasePriceByModelId(int modelId) {
        String sql = "SELECT BasePrice FROM VehicleModel WHERE ModelID = ?";
        try (Connection conn = DBUtils.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, modelId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getBigDecimal("BasePrice");
        } catch (SQLException e) { e.printStackTrace(); }
        return BigDecimal.ZERO;
    }
}
