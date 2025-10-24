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
                SELECT po.PurchaseOrderID, po.DealerID, po.StaffID, po.CreatedAt, po.Status,
                       d.DealerName, ds.FullName AS StaffName
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
                dto.setDealerId(rs.getInt("DealerID"));
                dto.setStaffId(rs.getInt("StaffID"));
                dto.setDealerName(rs.getString("DealerName"));
                dto.setStaffName(rs.getString("StaffName"));
                dto.setCreatedAt(rs.getTimestamp("CreatedAt"));
                dto.setStatus(rs.getString("Status"));
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
                SELECT po.PurchaseOrderID, po.DealerID, po.StaffID, po.CreatedAt, po.Status,
                       d.DealerName, d.Address AS DealerAddress, d.Phone AS DealerPhone, d.Email AS DealerEmail,
                       ds.FullName AS StaffName, ds.Position AS StaffPosition
                FROM PurchaseOrder po
                LEFT JOIN Dealer d ON po.DealerID = d.DealerID
                LEFT JOIN DealerStaff ds ON po.StaffID = ds.StaffID
                WHERE po.PurchaseOrderID = ?
                """;

        String sqlDetail = """
                SELECT pod.PODetailID, pod.PurchaseOrderID, pod.ColorID, pod.Quantity, pod.ModelID, pod.Version,
                       vm.ModelName, vc.ColorName
                FROM PurchaseOrderDetail pod
                LEFT JOIN VehicleModel vm ON pod.ModelID = vm.ModelID
                LEFT JOIN VehicleColor vc ON pod.ColorID = vc.ColorID
                WHERE pod.PurchaseOrderID = ?
                """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement psOrder = conn.prepareStatement(sqlOrder)) {

            psOrder.setInt(1, id);
            try (ResultSet rs = psOrder.executeQuery()) {
                if (rs.next()) {
                    DTOPurchaseOrder dto = new DTOPurchaseOrder();
                    dto.setPurchaseOrderId(rs.getInt("PurchaseOrderID"));
                    dto.setDealerId(rs.getInt("DealerID"));
                    dto.setStaffId(rs.getInt("StaffID"));
                    dto.setDealerName(rs.getString("DealerName"));
                    dto.setStaffName(rs.getString("StaffName"));
                    dto.setCreatedAt(rs.getTimestamp("CreatedAt"));
                    dto.setStatus(rs.getString("Status"));

                    // 🔹 Lấy danh sách chi tiết đơn hàng
                    try (PreparedStatement psDetail = conn.prepareStatement(sqlDetail)) {
                        psDetail.setInt(1, id);
                        try (ResultSet drs = psDetail.executeQuery()) {
                            List<DTOPurchaseOrderDetail> details = new ArrayList<>();
                            while (drs.next()) {
                                DTOPurchaseOrderDetail d = new DTOPurchaseOrderDetail();
                                d.setPoDetailId(drs.getInt("PODetailID"));
                                d.setPurchaseOrderId(drs.getInt("PurchaseOrderID"));
                                d.setColorId(drs.getInt("ColorID"));
                                d.setQuantity(drs.getInt("Quantity"));
                                d.setModelId(drs.getInt("ModelID"));
                                d.setVersion(drs.getString("Version"));
                                d.setModelName(drs.getString("ModelName"));
                                d.setColorName(drs.getString("ColorName"));
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
    public boolean updatePurchaseOrderStatus(int id, String newStatus) {
        String sql = "UPDATE PurchaseOrder SET Status = ? WHERE PurchaseOrderID = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
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
        String sql = "INSERT INTO PurchaseOrder (DealerID, StaffID, CreatedAt, Status) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, order.getDealerId());
            ps.setInt(2, order.getStaffId());
            ps.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            ps.setString(4, order.getStatus());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // ✅ Lấy DealerID theo email (tự động tạo nếu chưa có)
    public int getDealerIdByEmail(String email) {
        String selectSql = "SELECT DealerID FROM Dealer WHERE Email = ?";
        String insertSql = "INSERT INTO Dealer (dealerName, address, phone, email, EvmID, AccountID, LevelID, PolicyID) " +
                "VALUES (?, NULL, NULL, ?, NULL, NULL, 1, NULL)";

        try (Connection conn = DBUtils.getConnection()) {
            // 🔍 Tìm Dealer trước
            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt("DealerID");
                }
            }

            // ⚙️ Nếu chưa có thì tạo mới Dealer
            try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, email.split("@")[0]); // dealerName theo email
                ps.setString(2, email);
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

    // ✅ Lấy StaffID theo email (tự động tạo nếu chưa có)
    public int getStaffIdByEmail(String email) {
        String selectSql = "SELECT StaffID FROM DealerStaff WHERE Email = ?";
        String insertSql = "INSERT INTO DealerStaff (DealerID, FullName, Position, Email) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBUtils.getConnection()) {
            // 🔍 Tìm Staff trước
            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt("StaffID");
                }
            }

            // ⚙️ Nếu chưa có thì tạo Staff mới (gắn với Dealer tương ứng)
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
            SELECT po.PurchaseOrderID, po.DealerID, po.StaffID, po.CreatedAt, po.Status,
                   d.DealerName, ds.FullName AS StaffName
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
                    dto.setDealerId(rs.getInt("DealerID"));
                    dto.setStaffId(rs.getInt("StaffID"));
                    dto.setDealerName(rs.getString("DealerName"));
                    dto.setStaffName(rs.getString("StaffName"));
                    dto.setCreatedAt(rs.getTimestamp("CreatedAt"));
                    dto.setStatus(rs.getString("Status"));
                    list.add(dto);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

}
