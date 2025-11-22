package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.*;
import com.dealermanagementsysstem.project.configuration.BusinessConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
            model.addAttribute("message", "Lỗi khi tải danh sách xe: " + e.getMessage());
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

            System.out.println("DEBUG: Logged in email = " + email);

            // Lấy DealerID dựa theo email đăng nhập (qua Account → DealerStaff → Dealer)
            int dealerId = daoPurchaseOrder.getDealerIdByEmail(email);
            System.out.println("DEBUG: DealerID found = " + dealerId);

            if (dealerId <= 0) {
                model.addAttribute("message", "Không tìm thấy Dealer tương ứng với tài khoản đăng nhập (" + email + "). " +
                    "Vui lòng liên hệ admin để được gán vào một dealer.");
                model.addAttribute("orders", List.of());
                return "dealerPage/orderStatusList";
            }

            // Lấy danh sách đơn hàng theo DealerID
            List<DTOPurchaseOrder> orders = daoPurchaseOrder.getPurchaseOrdersByDealerId(dealerId);
            System.out.println("DEBUG: Number of orders found = " + (orders != null ? orders.size() : 0) + " for DealerID=" + dealerId);

            model.addAttribute("orders", orders);
            model.addAttribute("dealerId", dealerId); // Add for debugging
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("message", "Lỗi khi tải danh sách đơn hàng: " + e.getMessage());
            model.addAttribute("orders", List.of());
        }

        return "dealerPage/orderStatusList";
    }

    /**
     * 🔹 Order History for Dealer (filtered by dealer ID, shows completed/delivered orders)
     */
    @GetMapping("/history")
    public String showOrderHistory(Model model,
                                   @RequestParam(required = false) String keyword) {
        try {
            // Get logged in dealer
            org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            org.springframework.security.core.userdetails.User user =
                    (org.springframework.security.core.userdetails.User) auth.getPrincipal();
            String email = user.getUsername();

            int dealerId = daoPurchaseOrder.getDealerIdByEmail(email);
            
            if (dealerId <= 0) {
                model.addAttribute("message", "Không tìm thấy Dealer tương ứng với tài khoản đăng nhập.");
                model.addAttribute("orders", List.of());
                return "dealerPage/orderHistoryList";
            }

            // Get all orders for this dealer
            List<DTOPurchaseOrder> allOrders = daoPurchaseOrder.getPurchaseOrdersByDealerId(dealerId);

            // Filter only completed/delivered orders (history)
            List<DTOPurchaseOrder> historyOrders = allOrders.stream()
                .filter(order -> order.getStatus() == PurchaseOrderStatus.DELIVERED 
                              || order.getStatus() == PurchaseOrderStatus.CANCELLED)
                .toList();

            // Apply keyword search if provided
            if (keyword != null && !keyword.trim().isEmpty()) {
                String searchKeyword = keyword.toLowerCase().trim();
                historyOrders = historyOrders.stream()
                    .filter(order -> {
                        String status = order.getStatus() != null ? order.getStatus().toString().toLowerCase() : "";
                        String orderId = String.valueOf(order.getPurchaseOrderId());
                        return status.contains(searchKeyword) || orderId.contains(searchKeyword);
                    })
                    .toList();
            }

            model.addAttribute("orders", historyOrders);
            model.addAttribute("keyword", keyword);

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("message", "Lỗi khi tải lịch sử đơn hàng: " + e.getMessage());
            model.addAttribute("orders", List.of());
        }

        return "dealerPage/orderHistoryList";
    }


    @PostMapping("/createMultiple")
    public String createMultipleOrders(
            @RequestParam(name = "vehicleIds") List<Integer> vehicleIds,
            @RequestParam(name = "quantities") List<Integer> quantities,
            RedirectAttributes redirectAttributes) {

        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            var user = (org.springframework.security.core.userdetails.User) auth.getPrincipal();
            String email = user.getUsername();

            System.out.println("DEBUG PurchaseOrder: Logged in email = " + email);

            int dealerId = daoPurchaseOrder.getDealerIdByEmail(email);
            int staffId = daoPurchaseOrder.getStaffIdByEmail(email);

            System.out.println("DEBUG PurchaseOrder: DealerID = " + dealerId + ", StaffID = " + staffId);

            if (dealerId <= 0) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy Dealer tương ứng với tài khoản (" + email + "). " +
                    "Account của bạn chưa được liên kết với dealer nào. Vui lòng liên hệ admin.");
                return "redirect:/orderdealer";
            }

            if (staffId <= 0) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy Staff tương ứng với tài khoản (" + email + "). " +
                    "Account của bạn chưa có thông tin DealerStaff. Vui lòng liên hệ admin.");
                return "redirect:/orderdealer";
            }

            // Validate input
            if (vehicleIds == null || vehicleIds.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không có xe nào được chọn!");
                return "redirect:/orderdealer";
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
                redirectAttributes.addFlashAttribute("errorMessage", "Không có xe hợp lệ để đặt hàng!");
                return "redirect:/orderdealer";
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

                redirectAttributes.addFlashAttribute("successMessage", "Đặt hàng thành công! Đơn hàng #" + newOrderId + " với " + successCount + " loại xe.");
                redirectAttributes.addFlashAttribute("orderId", newOrderId);
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Không thể tạo đơn hàng!");
            }

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống: " + e.getMessage());
        }

        return "redirect:/orderdealer";
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
            Double vatRateConfig = businessConfig.getVat().getRate();
            model.addAttribute("vatRate", vatRateConfig);
            List<DTODealerInventory> inventoryVehicles = daoDealerInventory.getInventoryByPurchaseOrderId(id);
            model.addAttribute("inventoryVehicles", inventoryVehicles);
            if (order.getOrderDetails() != null && !order.getOrderDetails().isEmpty()) {
                long totalItems = order.getOrderDetails().size();
                long paidItems = order.getOrderDetails().stream().filter(d -> "PAID".equals(d.getPaymentStatus())).count();
                long unpaidItems = totalItems - paidItems; boolean allPaid = unpaidItems == 0;
                model.addAttribute("paymentTotalItems", totalItems);
                model.addAttribute("paymentPaidItems", paidItems);
                model.addAttribute("paymentUnpaidItems", unpaidItems);
                model.addAttribute("paymentAllPaid", allPaid);
            }
            // Invoice calculations
            java.math.BigDecimal gross = java.math.BigDecimal.ZERO;
            java.math.BigDecimal net = order.getTotalAmount() != null ? order.getTotalAmount() : java.math.BigDecimal.ZERO; // stored pre-VAT discounted total
            if (order.getOrderDetails() != null) {
                for (DTOPurchaseOrderDetail d : order.getOrderDetails()) {
                    java.math.BigDecimal base = d.getBasePrice();
                    int qty = d.getQuantity();
                    if (base != null) gross = gross.add(base.multiply(java.math.BigDecimal.valueOf(qty)));
                    else if (d.getSubtotal() != null) gross = gross.add(d.getSubtotal());
                }
            }
            if (gross.compareTo(net) < 0) gross = net;
            Double discountPercent = order.getPolicyDiscountPercent();
            java.math.BigDecimal discountAmount = gross.subtract(net);
            if (discountPercent == null && gross.compareTo(java.math.BigDecimal.ZERO) > 0 && discountAmount.compareTo(java.math.BigDecimal.ZERO) > 0) {
                discountPercent = discountAmount.multiply(java.math.BigDecimal.valueOf(100)).divide(gross, java.math.MathContext.DECIMAL64).doubleValue();
            }
            model.addAttribute("invoiceGross", gross);
            model.addAttribute("invoiceNet", net);
            model.addAttribute("invoiceDiscountAmount", discountAmount);
            model.addAttribute("invoiceDiscountPercent", discountPercent != null ? discountPercent : 0.0);
            // Compute total with VAT for clarity (net + VAT)
            double vatPercent = vatRateConfig != null ? vatRateConfig : 10.0;
            java.math.BigDecimal vatAmount = net.multiply(java.math.BigDecimal.valueOf(vatPercent / 100.0));
            java.math.BigDecimal totalWithVat = net.add(vatAmount);
            model.addAttribute("invoiceVatAmount", vatAmount);
            model.addAttribute("invoiceTotalWithVat", totalWithVat);

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

    /**
     * Force manual recalculation of order detail prices
     */
    @GetMapping("/detail/{id}/recalc")
    public String forceRecalc(@PathVariable int id, RedirectAttributes redirectAttributes) {
        try {
            int fixed = daoPurchaseOrder.recalcDetailPrices(id);
            redirectAttributes.addFlashAttribute("fixMessage", "Manual price recalculation applied: " + fixed + " detail line(s) updated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("fixMessage", "Recalc error: " + e.getMessage());
        }
        return "redirect:/orderdealer/detail/" + id;
    }


}
