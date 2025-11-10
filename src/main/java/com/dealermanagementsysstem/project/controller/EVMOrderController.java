package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.sql.Date;
import java.util.List;

@Controller
@RequestMapping("/evm/orders")
public class EVMOrderController {

    @Autowired
    private DAOPurchaseOrder purchaseOrderDAO; // Use Spring-managed bean with enriched logic

    @Autowired
    private DAOPurchaseOrderDetail daoPurchaseOrderDetail;

    // 🔹 Hiển thị toàn bộ danh sách đơn hàng (EVM xem)
    @GetMapping("/list") // changed from /evmOrderList to /list under /evm/orders
    public String showAllOrders(Model model,
                                @ModelAttribute("message") String message,
                                @ModelAttribute("statusType") String statusType) {

        List<DTOPurchaseOrder> orders = purchaseOrderDAO.getAllPurchaseOrders();
        model.addAttribute("orders", orders);

        if (message != null && !message.isEmpty()) {
            model.addAttribute("message", message);
            model.addAttribute("statusType", statusType);
        }

        return "evmPage/evmOrderList"; // keep same template name
    }

    // 🔹 Hiển thị lịch sử đơn hàng (chỉ các đơn đã xử lý)
    @GetMapping("/history")
    public String showOrderHistory(Model model,
                                   @RequestParam(required = false) String keyword,
                                   @ModelAttribute("message") String message,
                                   @ModelAttribute("statusType") String statusType) {

        List<DTOPurchaseOrder> allOrders = purchaseOrderDAO.getAllPurchaseOrders();

        // Filter only processed orders (not REQUESTED)
        List<DTOPurchaseOrder> historyOrders = allOrders.stream()
            .filter(order -> order.getStatus() != PurchaseOrderStatus.REQUESTED)
            .toList();

        // Apply keyword search if provided
        if (keyword != null && !keyword.trim().isEmpty()) {
            String searchKeyword = keyword.toLowerCase().trim();
            historyOrders = historyOrders.stream()
                .filter(order -> {
                    String dealerName = order.getDealerName() != null ? order.getDealerName().toLowerCase() : "";
                    String status = order.getStatus() != null ? order.getStatus().toString().toLowerCase() : "";
                    return dealerName.contains(searchKeyword) || status.contains(searchKeyword);
                })
                .toList();
        }

        model.addAttribute("orders", historyOrders);
        model.addAttribute("keyword", keyword);

        if (message != null && !message.isEmpty()) {
            model.addAttribute("message", message);
            model.addAttribute("statusType", statusType);
        }

        return "evmPage/evmOrderHistory";
    }

    // 🔹 Hiển thị chi tiết đơn hàng
    @GetMapping("/detail/{id}")
    public String showOrderDetail(@PathVariable("id") int orderId, Model model) {
        DTOPurchaseOrder order = purchaseOrderDAO.getPurchaseOrderById(orderId);
        if (order != null) {
            model.addAttribute("order", order);

            // Calculate payment summary
            if (order.getOrderDetails() != null && !order.getOrderDetails().isEmpty()) {
                long totalItems = order.getOrderDetails().size();
                long paidItems = order.getOrderDetails().stream()
                        .filter(d -> "PAID".equals(d.getPaymentStatus()))
                        .count();
                long unpaidItems = totalItems - paidItems;
                boolean allPaid = unpaidItems == 0;

                model.addAttribute("paymentTotalItems", totalItems);
                model.addAttribute("paymentPaidItems", paidItems);
                model.addAttribute("paymentUnpaidItems", unpaidItems);
                model.addAttribute("paymentAllPaid", allPaid);
            }

            return "evmPage/orderDetail";
        } else {
            return "redirect:/evm/orders/list?error=Order not found";
        }
    }

    private void ensureDeliveryCreated(int purchaseOrderId) {
        DAODelivery daoDelivery = new DAODelivery();
        DTODelivery del = new DTODelivery();
        DTOPurchaseOrder po = new DTOPurchaseOrder();
        po.setPurchaseOrderId(purchaseOrderId);
        del.setPurchaseOrder(po);
        del.setDeliveryDate(Date.valueOf(LocalDate.now().plusDays(7))); // planned +7 days
        del.setDeliveryStatus(DeliveryStatus.CREATED); // use existing enum value
        daoDelivery.createDelivery(del); // ignore result
    }

    // Process order (approve / reject) with improved error handling
    @PostMapping("/process/{id}")
    public String processOrder(@PathVariable int id,
                               @RequestParam("actionType") String actionType,
                               RedirectAttributes redirectAttributes) {

        System.out.println("🔍 Processing order ID: " + id + ", Action: " + actionType);

        try {
            // Validate action type
            PurchaseOrderStatus newStatus;
            if (actionType.equalsIgnoreCase("Approve") || actionType.equalsIgnoreCase("APPROVED")) {
                newStatus = PurchaseOrderStatus.APPROVED;
            } else if (actionType.equalsIgnoreCase("Reject") || actionType.equalsIgnoreCase("REJECTED") || actionType.equalsIgnoreCase("Cancel")) {
                newStatus = PurchaseOrderStatus.CANCELLED;
            } else {
                System.out.println("❌ Invalid action type: " + actionType);
                redirectAttributes.addFlashAttribute("message", "Invalid action type!");
                redirectAttributes.addFlashAttribute("statusType", "error");
                return "redirect:/evm/orders/list";
            }

            // Fetch order
            DTOPurchaseOrder order = purchaseOrderDAO.getPurchaseOrderById(id);
            if (order == null) {
                System.out.println("❌ Order not found with ID: " + id);
                redirectAttributes.addFlashAttribute("message", "Order not found!");
                redirectAttributes.addFlashAttribute("statusType", "error");
                return "redirect:/evm/orders/list";
            }

            // Check if order can be processed (not already approved/cancelled)
            if (order.getStatus() == PurchaseOrderStatus.APPROVED) {
                System.out.println("⚠️ Order already approved: " + id);
                redirectAttributes.addFlashAttribute("message", "Order is already approved!");
                redirectAttributes.addFlashAttribute("statusType", "warning");
                return "redirect:/evm/orders/list";
            }

            if (order.getStatus() == PurchaseOrderStatus.CANCELLED) {
                System.out.println("⚠️ Order already cancelled: " + id);
                redirectAttributes.addFlashAttribute("message", "Order is already cancelled!");
                redirectAttributes.addFlashAttribute("statusType", "warning");
                return "redirect:/evm/orders/list";
            }

            System.out.println("📦 Order found - DealerID: " + order.getDealer().getDealerID() + ", Current Status: " + order.getStatus());


            // Update status
            boolean updated = purchaseOrderDAO.updatePurchaseOrderStatus(id, newStatus);
            if (!updated) {
                System.out.println("❌ Failed to update order status");
                redirectAttributes.addFlashAttribute("message", "Failed to update order status!");
                redirectAttributes.addFlashAttribute("statusType", "error");
                return "redirect:/evm/orders/list";
            }

            System.out.println("✅ Updated status to: " + newStatus);

            // Create delivery record if approved
            if (newStatus == PurchaseOrderStatus.APPROVED) {
                try {
                    System.out.println("📦 Order approved - creating delivery record");
                    ensureDeliveryCreated(id);
                    System.out.println("✅ Delivery record created successfully");
                } catch (Exception e) {
                    System.out.println("⚠️ Warning: Failed to create delivery record - " + e.getMessage());
                    e.printStackTrace();
                    // Don't fail the whole operation, just log warning
                }
            }

            // Success message
            String msg = (newStatus == PurchaseOrderStatus.APPROVED)
                    ? "✅ Order has been approved successfully!"
                    : "❌ Order has been rejected!";
            redirectAttributes.addFlashAttribute("message", msg);
            redirectAttributes.addFlashAttribute("statusType", newStatus.name());

            return "redirect:/evm/orders/list";

        } catch (Exception e) {
            System.out.println("❌ ERROR processing order " + id + ": " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("message", "Error processing order: " + e.getMessage());
            redirectAttributes.addFlashAttribute("statusType", "error");
            return "redirect:/evm/orders/list";
        }
    }

    @PostMapping("/delivery/{orderId}/status")
    public String updateDeliveryStatus(@PathVariable int orderId,
                                       @RequestParam("newStatus") String newStatus,
                                       RedirectAttributes redirectAttributes){

        // ✅ Check payment status before allowing delivery status update
        boolean allPaid = daoPurchaseOrderDetail.areAllDetailsPaid(orderId);
        if (!allPaid) {
            String paymentSummary = daoPurchaseOrderDetail.getPaymentSummary(orderId);
            System.out.println("❌ Cannot update delivery status - payment not completed: " + paymentSummary);
            redirectAttributes.addFlashAttribute("message", "❌ Cannot update delivery status! Payment must be confirmed first. (" + paymentSummary + ")");
            redirectAttributes.addFlashAttribute("statusType", "error");
            return "redirect:/evm/orders/detail/" + orderId;
        }

        DAODelivery daoDelivery = new DAODelivery();
        if(!daoDelivery.existsDelivery(orderId)){
            ensureDeliveryCreated(orderId); // create if missing
        }
        DeliveryStatus target;
        try { target = DeliveryStatus.valueOf(newStatus.toUpperCase()); } catch(IllegalArgumentException ex){
            redirectAttributes.addFlashAttribute("message","❌ Invalid delivery status");
            redirectAttributes.addFlashAttribute("statusType","error");
            return "redirect:/evm/orders/detail/"+orderId;
        }
        java.util.Date now = new java.util.Date();
        boolean ok = daoDelivery.updateDeliveryStatusByPurchaseOrderId(orderId,target, (target==DeliveryStatus.IN_TRANSIT||target==DeliveryStatus.CREATED)? daoDelivery.getLatestByPurchaseOrderId(orderId)!=null? daoDelivery.getLatestByPurchaseOrderId(orderId).getDeliveryDate(): now : now);
        if(ok){
            // sync purchase order status
            if(target==DeliveryStatus.IN_TRANSIT){ purchaseOrderDAO.updatePurchaseOrderStatus(orderId, PurchaseOrderStatus.IN_PROCESS); }
            if(target==DeliveryStatus.DELIVERED){
                purchaseOrderDAO.updatePurchaseOrderStatus(orderId, PurchaseOrderStatus.DELIVERED);
                // ✅ Add vehicles to dealer inventory when delivery is completed
                try {
                    DTOPurchaseOrder po = purchaseOrderDAO.getPurchaseOrderById(orderId);
                    if (po != null && po.getOrderDetails() != null && po.getDealer() != null) {
                        DAODealerInventory inventoryDAO = new DAODealerInventory();
                        int totalDetails = po.getOrderDetails().size();
                        int successCount = 0;

                        System.out.println("📦 Processing delivery for PO #" + orderId + " with " + totalDetails + " detail(s)");

                        for (DTOPurchaseOrderDetail d : po.getOrderDetails()) {
                            int dealerId = po.getDealer().getDealerID();
                            int colorId = d.getColor() != null ? d.getColor().getColorID() : 0;
                            int versionId = d.getVersion() != null ? d.getVersion().getVersionID() : 0;
                            int qty = d.getQuantity();

                            System.out.println("  → Detail: ColorID=" + colorId + " VersionID=" + versionId + " Qty=" + qty);

                            if (colorId > 0 && versionId > 0 && qty > 0) {
                                boolean added = inventoryDAO.addWhenDeliveryCompleted(orderId, dealerId, colorId, versionId, qty);
                                if (added) {
                                    successCount++;
                                    System.out.println("  ✅ Added " + qty + " vehicle(s) to inventory");
                                } else {
                                    System.out.println("  ❌ Failed to add vehicles to inventory");
                                }
                            } else {
                                System.out.println("  ⚠️ Skipped: Invalid ColorID/VersionID/Qty");
                            }
                        }

                        System.out.println("✅ Inventory update complete: " + successCount + "/" + totalDetails + " details processed");
                    }
                } catch (Exception ex) {
                    // log and continue
                    System.out.println("❌ Failed to add inventory on delivery completion: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
            if(target==DeliveryStatus.CANCELLED){ purchaseOrderDAO.updatePurchaseOrderStatus(orderId, PurchaseOrderStatus.CANCELLED); }
            redirectAttributes.addFlashAttribute("message","✅ Delivery status updated to "+target);
            redirectAttributes.addFlashAttribute("statusType",target.name());
        } else {
            redirectAttributes.addFlashAttribute("message","❌ Failed to update delivery status");
            redirectAttributes.addFlashAttribute("statusType","error");
        }
        return "redirect:/evm/orders/detail/"+orderId;
    }

    // 🔹 NEW: Cập nhật Planned Delivery Date (ngày dự kiến giao) thủ công
    @PostMapping("/delivery/{orderId}/date")
    public String updatePlannedDeliveryDate(@PathVariable int orderId,
                                            @RequestParam("plannedDate") String plannedDate,
                                            RedirectAttributes redirectAttributes){
        try {
            DAODelivery daoDelivery = new DAODelivery();
            if(!daoDelivery.existsDelivery(orderId)) {
                ensureDeliveryCreated(orderId);
            }
            java.util.Date date = null;
            if(plannedDate!=null && !plannedDate.isBlank()){
                date = java.sql.Date.valueOf(plannedDate);
            }
            boolean ok = daoDelivery.updateDeliveryDateByPurchaseOrderId(orderId,date);
            if(ok){
                redirectAttributes.addFlashAttribute("message","✅ Planned delivery date updated");
                redirectAttributes.addFlashAttribute("statusType","UPDATED");
            } else {
                redirectAttributes.addFlashAttribute("message","❌ Failed to update planned delivery date");
                redirectAttributes.addFlashAttribute("statusType","error");
            }
        } catch (Exception ex){
            redirectAttributes.addFlashAttribute("message","❌ Invalid date format");
            redirectAttributes.addFlashAttribute("statusType","error");
        }
        return "redirect:/evm/orders/detail/"+orderId;
    }

    /**
     * Confirm payment received from dealer
     * EVM staff marks that payment has been received for all items in the order
     */
    @PostMapping("/payment/{orderId}/confirm")
    public String confirmPaymentReceived(@PathVariable int orderId,
                                         RedirectAttributes redirectAttributes) {
        try {
            // Verify order exists
            DTOPurchaseOrder order = purchaseOrderDAO.getPurchaseOrderById(orderId);
            if (order == null) {
                redirectAttributes.addFlashAttribute("message", "❌ Order not found!");
                redirectAttributes.addFlashAttribute("statusType", "error");
                return "redirect:/evm/orders/list";
            }

            // Allow payment confirmation for any order status (not just REQUESTED)
            // Payment tracking is independent of order workflow

            // Update all order details to PAID
            boolean updated = daoPurchaseOrderDetail.updatePaymentStatusByPurchaseOrderId(orderId, "PAID");

            if (updated) {
                redirectAttributes.addFlashAttribute("message", "✅ Payment confirmed! All items marked as PAID.");
                redirectAttributes.addFlashAttribute("statusType", "PAID");
                System.out.println("✅ Payment confirmed for order #" + orderId);
            } else {
                redirectAttributes.addFlashAttribute("message", "❌ Failed to confirm payment!");
                redirectAttributes.addFlashAttribute("statusType", "error");
                System.out.println("❌ Failed to confirm payment for order #" + orderId);
            }

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("message", "⚠️ Error confirming payment: " + e.getMessage());
            redirectAttributes.addFlashAttribute("statusType", "error");
        }

        return "redirect:/evm/orders/detail/" + orderId;
    }

    // 🔹 NEW: Tạo hợp đồng (Contract) từ PurchaseOrder sau khi Approved
    @PostMapping("/contract/{orderId}/create")
    public String createContractFromOrder(@PathVariable int orderId, RedirectAttributes redirectAttributes){
        DTOPurchaseOrder order = purchaseOrderDAO.getPurchaseOrderById(orderId);
        if(order==null){
            redirectAttributes.addFlashAttribute("message","❌ Order not found");
            redirectAttributes.addFlashAttribute("statusType","error");
            return "redirect:/evm/orders/list";
        }
        if(order.getStatus()!=PurchaseOrderStatus.APPROVED && order.getStatus()!=PurchaseOrderStatus.DELIVERED && order.getStatus()!=PurchaseOrderStatus.IN_PROCESS){
            redirectAttributes.addFlashAttribute("message","⚠️ Order must be approved before creating contract");
            redirectAttributes.addFlashAttribute("statusType","error");
            return "redirect:/evm/orders/detail/"+orderId;
        }
        // Placeholder logic: In thực tế sẽ gọi DAOContract.create(...)
        // Ở đây chỉ flash message xác nhận.
        redirectAttributes.addFlashAttribute("message","✅ Contract created for order #"+orderId+" (placeholder) ");
        redirectAttributes.addFlashAttribute("statusType","CONTRACT_CREATED");
        return "redirect:/evm/orders/detail/"+orderId;
    }
}
