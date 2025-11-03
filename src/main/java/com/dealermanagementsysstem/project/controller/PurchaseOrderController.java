package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.*;
import com.dealermanagementsysstem.project.configuration.BusinessConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/orderdealer")
public class PurchaseOrderController {

    @Autowired
    private DAOPurchaseOrder daoPurchaseOrder;

    @Autowired
    private DAOPurchaseOrderDetail daoPurchaseOrderDetail;

    @Autowired
    private DAOVehicleVersionLookup daoVehicleVersionLookup;

    @Autowired
    private DAOVehicle daoVehicle;

    @Autowired
    private DAODealerInventory daoDealerInventory;

    @Autowired
    private BusinessConfig businessConfig;

    /**
     * 🔹 Trang chọn nhiều xe để đặt hàng (giống getVehicleListToCreateQuotation)
     */
    @GetMapping("/choose")
    public String showChooseVehicle(Model model) {
        try {
            // Get TEMPLATE vehicles (catalog) with fallback
            List<DTOVehicle> vehicles = daoVehicle.getVehiclesByStatus(VehicleStatus.TEMPLATE);

            // Fallback to all vehicles if no TEMPLATE vehicles exist
            if (vehicles == null || vehicles.isEmpty()) {
                vehicles = daoVehicle.getVehicles();
            }

            model.addAttribute("vehicleList", vehicles);
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("message", "⚠️ Lỗi khi tải danh sách xe: " + e.getMessage());
            model.addAttribute("vehicleList", List.of());
        }
        return "dealerPage/dealerOrderVehicleList";
    }

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
     * 🔹 Tạo đơn hàng với nhiều xe cùng lúc (giống addVehiclesWithQty của Quotation)
     */
    @PostMapping("/createMultiple")
    public String createMultipleOrders(
            @RequestParam(name = "vehicleIds") List<Integer> vehicleIds,
            @RequestParam(name = "quantities") List<Integer> quantities,
            Model model) {

        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            var user = (org.springframework.security.core.userdetails.User) auth.getPrincipal();
            String email = user.getUsername();

            int dealerId = daoPurchaseOrder.getDealerIdByEmail(email);
            int staffId = daoPurchaseOrder.getStaffIdByEmail(email);

            if (dealerId <= 0 || staffId <= 0) {
                model.addAttribute("message", "❌ Không tìm thấy Dealer hoặc Staff tương ứng với tài khoản (" + email + ")");
                return "dealerPage/success";
            }

            // Validate input
            if (vehicleIds == null || vehicleIds.isEmpty()) {
                model.addAttribute("message", "❌ Không có xe nào được chọn!");
                return "dealerPage/success";
            }

            // Normalize quantities
            List<Integer> normalizedQty = new ArrayList<>();
            for (int i = 0; i < vehicleIds.size(); i++) {
                int q = 1;
                if (i < quantities.size() && quantities.get(i) != null && quantities.get(i) > 0) {
                    q = quantities.get(i);
                }
                normalizedQty.add(q);
            }

            // Calculate total amount for all vehicles
            BigDecimal totalAmount = BigDecimal.ZERO;
            List<VehicleOrderItem> orderItems = new ArrayList<>();

            for (int i = 0; i < vehicleIds.size(); i++) {
                DTOVehicle vehicle = daoVehicle.getVehicleById(vehicleIds.get(i));
                if (vehicle == null || vehicle.getVersion() == null || vehicle.getColor() == null) {
                    continue; // Skip invalid vehicles
                }

                int versionId = vehicle.getVersion().getVersionID();
                int colorId = vehicle.getColor().getColorID();
                int qty = normalizedQty.get(i);

                // Calculate price with dealer discount
                BigDecimal unitPrice = daoPurchaseOrderDetail.computeUnitPrice(versionId, dealerId);
                if (unitPrice == null) unitPrice = BigDecimal.ZERO;

                BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(qty));
                totalAmount = totalAmount.add(subtotal);

                orderItems.add(new VehicleOrderItem(versionId, colorId, qty, unitPrice, subtotal));
            }

            if (orderItems.isEmpty()) {
                model.addAttribute("message", "❌ Không có xe hợp lệ để đặt hàng!");
                return "dealerPage/success";
            }

            // Create PurchaseOrder
            DTOPurchaseOrder order = new DTOPurchaseOrder();
            DTODealer dealer = new DTODealer();
            dealer.setDealerID(dealerId);
            order.setDealer(dealer);

            DTODealerStaff staff = new DTODealerStaff();
            staff.setStaffID(staffId);
            order.setStaff(staff);

            order.setStatus(PurchaseOrderStatus.REQUESTED);
            order.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            order.setTotalAmount(totalAmount);
            order.setEvmID(1);

            int newOrderId = daoPurchaseOrder.insertPurchaseOrder(order);

            if (newOrderId > 0) {
                // Insert all order details
                int successCount = 0;
                for (VehicleOrderItem item : orderItems) {
                    boolean added = daoPurchaseOrderDetail.insertOrderDetail(
                        newOrderId,
                        item.colorId,
                        item.versionId,
                        item.quantity,
                        item.unitPrice
                    );
                    if (added) successCount++;
                }

                model.addAttribute("message", "✅ Đặt hàng thành công! Đơn hàng #" + newOrderId + " với " + successCount + " loại xe.");
                model.addAttribute("orderId", newOrderId);
            } else {
                model.addAttribute("message", "❌ Không thể tạo đơn hàng!");
            }

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("message", "⚠️ Lỗi hệ thống: " + e.getMessage());
        }

        return "dealerPage/success";
    }

    // Helper class to store order item data
    private static class VehicleOrderItem {
        int versionId;
        int colorId;
        int quantity;
        BigDecimal unitPrice;
        BigDecimal subtotal;

        VehicleOrderItem(int versionId, int colorId, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {
            this.versionId = versionId;
            this.colorId = colorId;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.subtotal = subtotal;
        }
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
     * Show order detail page with inventory vehicles (vehicles received in stock with VIN numbers)
     */
    @GetMapping("/detail/{id}")
    public String showOrderDetail(@PathVariable int id, Model model) {
        try {
            DTOPurchaseOrder order = daoPurchaseOrder.getPurchaseOrderById(id);
            if (order == null) {
                model.addAttribute("message", "Order not found!");
                return "dealerPage/orderStatusList";
            }

            model.addAttribute("order", order);

            // Add VAT rate from configuration (configurable, default 10%)
            model.addAttribute("vatRate", businessConfig.getVat().getRate());

            // Load danh sach xe da ve kho cho don hang nay
            List<DTODealerInventory> inventoryVehicles = daoDealerInventory.getInventoryByPurchaseOrderId(id);
            model.addAttribute("inventoryVehicles", inventoryVehicles);

            System.out.println("Loaded " + (inventoryVehicles != null ? inventoryVehicles.size() : 0)
                             + " inventory vehicles for order #" + id);

            return "dealerPage/orderDetail";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("message", "Error loading order details: " + e.getMessage());
            return "dealerPage/orderStatusList";
        }
    }

    /**
     * API: Get order by ID (JSON)
     */
    @ResponseBody
    @GetMapping("/api/{id}")
    public DTOPurchaseOrder getOrderById(@PathVariable int id) {
        return daoPurchaseOrder.getPurchaseOrderById(id);
    }

    /**
     * API: Update order status
     */
    @ResponseBody
    @PutMapping("/api/{id}/status")
    public String updateStatus(@PathVariable int id, @RequestParam String status) {
        boolean updated = daoPurchaseOrder.updatePurchaseOrderStatus(id, PurchaseOrderStatus.valueOf(status.toUpperCase()));
        return updated ? "Updated successfully" : "Update failed";
    }

    /**
     * API: Delete order
     */
    @ResponseBody
    @DeleteMapping("/api/{id}")
    public String deleteOrder(@PathVariable int id) {
        int result = daoPurchaseOrder.deletePurchaseOrder(id);
        return result > 0 ? "Deleted successfully" : "Delete failed";
    }


}
