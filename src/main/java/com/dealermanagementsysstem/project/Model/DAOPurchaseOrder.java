package com.dealermanagementsysstem.project.Model;

import org.springframework.stereotype.Repository;
import utils.DBUtils;

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
                       vc.ColorID, vc.ColorName, 
                       vv.VersionID, vv.VersionName, vv.ModelID,
                       vm.ModelID, vm.ModelName
                FROM PurchaseOrderDetail pod
                LEFT JOIN VehicleColor vc ON pod.ColorID = vc.ColorID
                LEFT JOIN VehicleVersion vv ON pod.VersionID = vv.VersionID
                LEFT JOIN VehicleModel vm ON vv.ModelID = vm.ModelID
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
                    dto.setDealer(dealer);

                    // Staff info
                    DTODealerStaff staff = new DTODealerStaff();
                    staff.setStaffID(rs.getInt("StaffID"));
                    staff.setFullName(rs.getString("StaffName"));
                    dto.setStaff(staff);

                    // 🔹 Lấy danh sách chi tiết đơn hàng
                    try (PreparedStatement psDetail = conn.prepareStatement(sqlDetail)) {
                        psDetail.setInt(1, id);
                        try (ResultSet drs = psDetail.executeQuery()) {
                            List<DTOPurchaseOrderDetail> details = new ArrayList<>();
                            while (drs.next()) {
                                DTOPurchaseOrderDetail d = new DTOPurchaseOrderDetail();
                                d.setPoDetailId(drs.getInt("PODetailID"));
                                d.setPurchaseOrder(dto); // Set the parent purchase order
                                d.setUnitPrice(drs.getBigDecimal("UnitPrice"));
                                d.setQuantity(drs.getInt("Quantity"));
                                d.setSubtotal(drs.getBigDecimal("Subtotal"));
                                
                                // Set color relationship if available
                                if (drs.getString("ColorName") != null) {
                                    DTOVehicleColor color = new DTOVehicleColor();
                                    color.setColorID(drs.getInt("ColorID"));
                                    color.setColorName(drs.getString("ColorName"));
                                    d.setColor(color);
                                }
                                
                                // Set version relationship with model if available
                                if (drs.getString("VersionName") != null) {
                                    // Create VehicleModel first
                                    DTOVehicleModel model = new DTOVehicleModel();
                                    model.setModelID(drs.getInt("ModelID"));
                                    model.setModelName(drs.getString("ModelName"));

                                    // Create VehicleVersion with model
                                    DTOVehicleVersion version = new DTOVehicleVersion();
                                    version.setVersionID(drs.getInt("VersionID"));
                                    version.setVersionName(drs.getString("VersionName"));
                                    version.setModel(model);

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

    // ✅ Lấy DealerID theo email (từ DealerStaff)
    public int getDealerIdByEmail(String email) {
        // Tìm trong DealerStaff trước (vì user đăng nhập là staff)
        String selectStaffSql = "SELECT DealerID FROM DealerStaff WHERE Email = ?";

        try (Connection conn = DBUtils.getConnection()) {
            // 🔍 Tìm Staff theo email
            try (PreparedStatement ps = conn.prepareStatement(selectStaffSql)) {
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int dealerId = rs.getInt("DealerID");
                        if (dealerId > 0) {
                            return dealerId;
                        }
                    }
                }
            }

            // Nếu không tìm thấy trong DealerStaff, thử tìm trong Dealer
            String selectDealerSql = "SELECT DealerID FROM Dealer WHERE Email = ?";
            try (PreparedStatement ps = conn.prepareStatement(selectDealerSql)) {
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("DealerID");
                    }
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

        try (Connection conn = DBUtils.getConnection()) {
            // 🔍 Tìm Staff theo email
            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("StaffID");
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
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

}
