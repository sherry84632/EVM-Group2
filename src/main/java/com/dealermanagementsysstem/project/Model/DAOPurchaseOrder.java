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
               d.DealerID, d.DealerName,
               ds.StaffID, ds.FullName AS StaffName
        FROM PurchaseOrder po
        LEFT JOIN Dealer d ON po.DealerID = d.DealerID
        LEFT JOIN DealerStaff ds ON po.StaffID = ds.StaffID
        ORDER BY po.PurchaseOrderID DESC
        """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                DTOPurchaseOrder dto = new DTOPurchaseOrder();
                dto.setPurchaseOrderId(rs.getInt("PurchaseOrderID"));
                dto.setCreatedAt(rs.getTimestamp("CreatedAt"));
                dto.setStatus(PurchaseOrderStatus.valueOf(rs.getString("Status")));
                dto.setTotalAmount(rs.getBigDecimal("TotalAmount"));
                dto.setEvmID(rs.getInt("EvmID"));

                // Dealer info
                DTODealer dealer = new DTODealer();
                dealer.setDealerID(rs.getInt("DealerID"));
                dealer.setDealerName(rs.getString("DealerName"));
                dto.setDealer(dealer);

                // Staff info
                DTODealerStaff staff = new DTODealerStaff();
                staff.setStaffID(rs.getInt("StaffID"));
                staff.setFullName(rs.getString("StaffName"));
                dto.setStaff(staff);

                list.add(dto);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 🔹 Lấy 1 đơn hàng theo ID (kèm chi tiết)
    public DTOPurchaseOrder getPurchaseOrderById(int id) {
        String sqlOrder = """
        SELECT po.PurchaseOrderID, po.DealerID, po.StaffID, po.CreatedAt, po.Status, po.TotalAmount, po.EvmID,
               d.DealerID, d.DealerName, d.Address AS DealerAddress, d.Phone AS DealerPhone, d.Email AS DealerEmail,
               ds.StaffID, ds.FullName AS StaffName, ds.Position AS StaffPosition
        FROM PurchaseOrder po
        LEFT JOIN Dealer d ON po.DealerID = d.DealerID
        LEFT JOIN DealerStaff ds ON po.StaffID = ds.StaffID
        WHERE po.PurchaseOrderID = ?
        """;

        String sqlDetail = """
        SELECT pod.PODetailID, pod.PurchaseOrderID, pod.ColorID, pod.VersionID, pod.UnitPrice, pod.Quantity, pod.Subtotal,
               vc.ColorID, vc.ColorName, vv.VersionID, vv.VersionName
        FROM PurchaseOrderDetail pod
        LEFT JOIN VehicleColor vc ON pod.ColorID = vc.ColorID
        LEFT JOIN VehicleVersion vv ON pod.VersionID = vv.VersionID
        WHERE pod.PurchaseOrderID = ?
        """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement psOrder = conn.prepareStatement(sqlOrder)) {

            psOrder.setInt(1, id);
            try (ResultSet rs = psOrder.executeQuery()) {
                if (rs.next()) {
                    DTOPurchaseOrder dto = new DTOPurchaseOrder();
                    dto.setPurchaseOrderId(rs.getInt("PurchaseOrderID"));
                    dto.setCreatedAt(rs.getTimestamp("CreatedAt"));
                    dto.setStatus(PurchaseOrderStatus.valueOf(rs.getString("Status")));
                    dto.setTotalAmount(rs.getBigDecimal("TotalAmount"));
                    dto.setEvmID(rs.getInt("EvmID"));

                    // Dealer info
                    DTODealer dealer = new DTODealer();
                    dealer.setDealerID(rs.getInt("DealerID"));
                    dealer.setDealerName(rs.getString("DealerName"));
                    dealer.setAddress(rs.getString("DealerAddress"));
                    dealer.setEmail(rs.getString("DealerEmail"));
                    dealer.setPhone(rs.getString("DealerPhone"));
                    dto.setDealer(dealer);

                    // Staff info
                    DTODealerStaff staff = new DTODealerStaff();
                    staff.setStaffID(rs.getInt("StaffID"));
                    staff.setFullName(rs.getString("StaffName"));
                    staff.setPosition(rs.getString("StaffPosition"));
                    dto.setStaff(staff);

                    // 🔹 Lấy danh sách chi tiết đơn hàng
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

                                // Color
                                if (drs.getString("ColorName") != null) {
                                    DTOVehicleColor color = new DTOVehicleColor();
                                    color.setColorID(drs.getInt("ColorID"));
                                    color.setColorName(drs.getString("ColorName"));
                                    d.setColor(color);
                                }

                                // Version
                                if (drs.getString("VersionName") != null) {
                                    DTOVehicleVersion version = new DTOVehicleVersion();
                                    version.setVersionID(drs.getInt("VersionID"));
                                    version.setVersionName(drs.getString("VersionName"));
                                    d.setVersion(version);
                                }

                                details.add(d);
                            }
                            dto.setOrderDetails(details);
                        }
                    }

                    return dto;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
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

    // ✅ Lấy DealerID theo email
    public int getDealerIdByEmail(String email) {
        String selectSql = "SELECT DealerID FROM Dealer WHERE Email = ?";
        String insertSql = "INSERT INTO Dealer (EvmID, LevelID, PolicyID, Address, DealerName, Email, Phone) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtils.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt("DealerID");
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, 1);
                ps.setInt(2, 1);
                ps.setInt(3, 1);
                ps.setString(4, "Unknown Address");
                ps.setString(5, email.split("@")[0]);
                ps.setString(6, email);
                ps.setString(7, "000-000-0000");
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // ✅ Lấy StaffID theo email
    public int getStaffIdByEmail(String email) {
        String selectSql = "SELECT StaffID FROM DealerStaff WHERE Email = ?";
        String insertSql = "INSERT INTO DealerStaff (DealerID, FullName, Position, Email) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBUtils.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt("StaffID");
                }
            }
            int dealerId = getDealerIdByEmail(email);
            if (dealerId > 0) {
                try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, dealerId);
                    ps.setString(2, "Staff " + email.split("@")[0]);
                    ps.setString(3, "Sales");
                    ps.setString(4, email);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // 🔹 Lấy danh sách đơn hàng theo DealerID
    public List<DTOPurchaseOrder> getPurchaseOrdersByDealerId(int dealerId) {
        List<DTOPurchaseOrder> list = new ArrayList<>();
        String sql = """
    SELECT po.PurchaseOrderID, po.DealerID, po.StaffID, po.CreatedAt, po.Status, po.TotalAmount, po.EvmID,
           d.DealerID, d.DealerName,
           ds.StaffID, ds.FullName AS StaffName
    FROM PurchaseOrder po
    LEFT JOIN Dealer d ON po.DealerID = d.DealerID
    LEFT JOIN DealerStaff ds ON po.StaffID = ds.StaffID
    WHERE po.DealerID = ?
    ORDER BY po.PurchaseOrderID DESC
    """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, dealerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DTOPurchaseOrder dto = new DTOPurchaseOrder();
                    dto.setPurchaseOrderId(rs.getInt("PurchaseOrderID"));
                    dto.setCreatedAt(rs.getTimestamp("CreatedAt"));
                    dto.setStatus(PurchaseOrderStatus.valueOf(rs.getString("Status")));
                    dto.setTotalAmount(rs.getBigDecimal("TotalAmount"));
                    dto.setEvmID(rs.getInt("EvmID"));

                    DTODealer dealer = new DTODealer();
                    dealer.setDealerID(rs.getInt("DealerID"));
                    dealer.setDealerName(rs.getString("DealerName"));
                    dto.setDealer(dealer);

                    DTODealerStaff staff = new DTODealerStaff();
                    staff.setStaffID(rs.getInt("StaffID"));
                    staff.setFullName(rs.getString("StaffName"));
                    dto.setStaff(staff);

                    list.add(dto);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 🔹 Lấy BasePrice của Model
    public BigDecimal getBasePriceByModelId(int modelId) {
        String sql = "SELECT BasePrice FROM VehicleModel WHERE ModelID = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, modelId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getBigDecimal("BasePrice");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO;
    }

}