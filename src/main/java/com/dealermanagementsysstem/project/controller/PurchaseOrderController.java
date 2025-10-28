package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

@Controller
@RequestMapping("/orderdealer")
public class PurchaseOrderController {

    @Autowired
    private DAOPurchaseOrder daoPurchaseOrder;

    @Autowired
    private DAOPurchaseOrderDetail daoPurchaseOrderDetail;

    @Autowired
    private DAOVehicleVersionLookup daoVehicleVersionLookup; // new lookup DAO

    /**
     * 🔹 Trang danh sách đơn hàng
     */
    /**
     * 🔹 Trang danh sách đơn hàng (chỉ hiển thị đơn của Dealer đang đăng nhập)
     */
    @GetMapping("")
    public String showOrderList(Model model) {
        try {
            // Lấy email của người đăng nhập (Spring Security)
            org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            org.springframework.security.core.userdetails.User user =
                    (org.springframework.security.core.userdetails.User) auth.getPrincipal();
            String email = user.getUsername();

            System.out.println("🔍 DEBUG: Logged in email = " + email);

            // Lấy DealerID dựa theo email đăng nhập
            int dealerId = daoPurchaseOrder.getDealerIdByEmail(email);
            System.out.println("🔍 DEBUG: DealerID found = " + dealerId);

            if (dealerId <= 0) {
                model.addAttribute("message", "❌ Không tìm thấy Dealer tương ứng với tài khoản đăng nhập (" + email + ")");
                model.addAttribute("orders", List.of());
                return "dealerPage/orderStatusList";
            }

            // Lấy danh sách đơn hàng theo DealerID
            List<DTOPurchaseOrder> orders = daoPurchaseOrder.getPurchaseOrdersByDealerId(dealerId);
            System.out.println("🔍 DEBUG: Number of orders found = " + (orders != null ? orders.size() : 0));

            model.addAttribute("orders", orders);
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("message", "⚠️ Lỗi khi tải danh sách đơn hàng: " + e.getMessage());
            model.addAttribute("orders", List.of());
        }

        return "dealerPage/orderStatusList";
    }


    /**
     * 🔹 Khi chọn xe → mở form nhập chi tiết đơn hàng
     */
    @GetMapping("/create")
    public String showCreateForm(@RequestParam(required = false) Integer modelId,
                                 @RequestParam(required = false) Integer colorId,
                                 @RequestParam(required = false) String modelName,
                                 Model model) {

        model.addAttribute("modelId", modelId);
        model.addAttribute("colorId", colorId);
        model.addAttribute("modelName", modelName);
        if(modelId != null){
            List<DTOVehicleVersion> versions = daoVehicleVersionLookup.getVersionsByModelId(modelId);
            model.addAttribute("versions", versions);
        }
        model.addAttribute("order", new DTOPurchaseOrder());
        return "dealerPage/createDealerOrderForm"; // form nhập số lượng + version
    }

    /**
     * 🔹 Xử lý form tạo đơn hàng
     */
    @PostMapping("/create")
    public String createOrder(@RequestParam Integer modelId,
                              @RequestParam Integer colorId,
                              @RequestParam Integer quantity,
                              @RequestParam String version,
                              @RequestParam(required = false) String status,
                              Model model) {

        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            var user = (org.springframework.security.core.userdetails.User) auth.getPrincipal();
            String email = user.getUsername();
            int dealerId = daoPurchaseOrder.getDealerIdByEmail(email);
            int staffId = daoPurchaseOrder.getStaffIdByEmail(email);
            if (dealerId <= 0 || staffId <= 0) {
                model.addAttribute("message", "❌ Không tìm thấy Dealer hoặc Staff tương ứng (" + email + ")");
                return "dealerPage/success";
            }
            int versionId = Integer.parseInt(version.trim());
            Integer realModelId = daoVehicleVersionLookup.getModelIdByVersionId(versionId);
            if(realModelId != null) modelId = realModelId;
            // Dealer-aware unit price
            BigDecimal unitPrice = daoPurchaseOrderDetail.computeUnitPrice(versionId, dealerId);
            if (unitPrice == null) unitPrice = BigDecimal.ZERO;
            BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));

            DTOPurchaseOrder order = new DTOPurchaseOrder();
            DTODealer dealer = new DTODealer(); dealer.setDealerID(dealerId); order.setDealer(dealer);
            DTODealerStaff staff = new DTODealerStaff(); staff.setStaffID(staffId); order.setStaff(staff);
            order.setStatus(PurchaseOrderStatus.valueOf(status != null ? status.toUpperCase() : "REQUESTED"));
            order.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            order.setTotalAmount(totalAmount);
            order.setEvmID(1);

            int newOrderId = daoPurchaseOrder.insertPurchaseOrder(order);
            if (newOrderId > 0) {
                boolean added = daoPurchaseOrderDetail.insertOrderDetailConsistent(newOrderId, colorId, versionId, quantity, dealerId);
                model.addAttribute("message", added ? " ✅ Đặt xe thành công!" : "⚠ Chi tiết chưa ghi!");
            } else {
                model.addAttribute("message", " ❌ Không thể tạo đơn hàng!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("message", " Lỗi hệ thống: " + e.getMessage());
        }

        return "dealerPage/success";
    }

    /**
     * 🔹 Trang success (chỉ khi load trực tiếp)
     */
    @GetMapping("/success")
    public String showSuccessPage(Model model) {
        if (!model.containsAttribute("message")) {
            model.addAttribute("message", " Order processed!");
        }
        return "dealerPage/success";
    }

    /**
     * 🔹 API: Lấy danh sách đơn hàng (JSON)
     */
    @ResponseBody
    @GetMapping("/api")
    public List<DTOPurchaseOrder> getAllOrders() {
        return daoPurchaseOrder.getAllPurchaseOrders();
    }

    /**
     * 🔹 Trang chi tiết đơn hàng
     */
    @GetMapping("/detail/{id}")
    public String showOrderDetail(@PathVariable int id, Model model) {
        try {
            DTOPurchaseOrder order = daoPurchaseOrder.getPurchaseOrderById(id);
            if (order == null) {
                model.addAttribute("message", "❌ Order not found!");
                return "dealerPage/orderStatusList";
            }
            model.addAttribute("order", order);
            return "dealerPage/orderDetail";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("message", "⚠️ Error loading order details: " + e.getMessage());
            return "dealerPage/orderStatusList";
        }
    }

    /**
     * 🔹 API: Lấy đơn hàng theo ID (JSON)
     */
    @ResponseBody
    @GetMapping("/api/{id}")
    public DTOPurchaseOrder getOrderById(@PathVariable int id) {
        return daoPurchaseOrder.getPurchaseOrderById(id);
    }

    /**
     * 🔹 API: Cập nhật trạng thái đơn hàng
     */
    @ResponseBody
    @PutMapping("/api/{id}/status")
    public String updateStatus(@PathVariable int id, @RequestParam String status) {
        boolean updated = daoPurchaseOrder.updatePurchaseOrderStatus(id, PurchaseOrderStatus.valueOf(status.toUpperCase()));
        return updated ? "Updated successfully" : "Update failed";
    }

    /**
     * 🔹 API: Xóa đơn hàng
     */
    @ResponseBody
    @DeleteMapping("/api/{id}")
    public String deleteOrder(@PathVariable int id) {
        int result = daoPurchaseOrder.deletePurchaseOrder(id);
        return result > 0 ? "Deleted successfully" : "Delete failed";
    }



}
