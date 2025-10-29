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

    // 🔹 Xử lý đơn hàng (phê duyệt / từ chối)
    @PostMapping("/process/{id}")
    public String processOrder(@PathVariable int id,
                               @RequestParam("actionType") String actionType,
                               RedirectAttributes redirectAttributes) {

        System.out.println("🔍 Processing order ID: " + id);
        System.out.println("📋 Action Type: " + actionType);

        // ✅ Map action type to correct enum value (case insensitive)
        PurchaseOrderStatus newStatus;
        if (actionType.equalsIgnoreCase("Approve") || actionType.equalsIgnoreCase("APPROVED")) {
            newStatus = PurchaseOrderStatus.APPROVED;
        } else if (actionType.equalsIgnoreCase("Reject") || actionType.equalsIgnoreCase("REJECTED") || actionType.equalsIgnoreCase("Cancel")) {
            newStatus = PurchaseOrderStatus.CANCELLED;
        } else {
            System.out.println("❌ Invalid action type: " + actionType);
            redirectAttributes.addFlashAttribute("message", "❌ Invalid action!");
            redirectAttributes.addFlashAttribute("statusType", "error");
            return "redirect:/evm/orders/list";
        }

        // ✅ Lấy chi tiết đơn hàng TRƯỚC KHI update status
        DTOPurchaseOrder order = purchaseOrderDAO.getPurchaseOrderById(id);

        if (order == null) {
            System.out.println("❌ Không tìm thấy đơn hàng với ID: " + id);
            redirectAttributes.addFlashAttribute("message", "❌ Order not found!");
            redirectAttributes.addFlashAttribute("statusType", "error");
            return "redirect:/evm/orders/list";
        }

        System.out.println("📦 Order found - DealerID: " + order.getDealer().getDealerID() + ", Status: " + order.getStatus());

        // Update status
        order.setStatus(newStatus);
        purchaseOrderDAO.updatePurchaseOrderStatus(order.getPurchaseOrderId(), newStatus);
        System.out.println("✅ Updated status to: " + newStatus);

        // ✅ Nếu đơn hàng được Approved, thêm xe vào inventory của dealer
        if (newStatus == PurchaseOrderStatus.APPROVED) {
            System.out.println("🚗 Bắt đầu thêm xe vào inventory...");
            DAODealerInventory inventoryDAO = new DAODealerInventory();

            if (order.getOrderDetails() == null || order.getOrderDetails().isEmpty()) {
                System.out.println("⚠️ CẢNH BÁO: OrderDetails là NULL hoặc RỖNG!");
                System.out.println("⚠️ Không có xe nào để thêm vào inventory!");
            } else {
                System.out.println("📋 Tìm thấy " + order.getOrderDetails().size() + " chi tiết đơn hàng");

                int successCount = 0;
                for (DTOPurchaseOrderDetail detail : order.getOrderDetails()) {
                    // ✅ Check for null values before accessing nested properties
                    if (detail.getVersion() == null) {
                        System.out.println("  ⚠️ CẢNH BÁO: Version is NULL for detail ID " + detail.getPoDetailId());
                        continue;
                    }

                    if (detail.getVersion().getModel() == null) {
                        System.out.println("  ⚠️ CẢNH BÁO: Model is NULL for version ID " + detail.getVersion().getVersionID());
                        continue;
                    }

                    if (detail.getColor() == null) {
                        System.out.println("  ⚠️ CẢNH BÁO: Color is NULL for detail ID " + detail.getPoDetailId());
                        continue;
                    }

                    System.out.println("  ➤ Thêm xe: ModelID=" + detail.getVersion().getModel().getModelID()
                        + ", ColorID=" + detail.getColor().getColorID()
                        + ", Quantity=" + detail.getQuantity());

                    boolean added = inventoryDAO.addVehiclesToInventory(
                        order.getDealer().getDealerID(),
                        detail.getColor().getColorID(),
                        detail.getVersion().getVersionID(),
                        detail.getQuantity()
                    );

                    if (added) {
                        successCount++;
                        System.out.println("  ✅ Thành công!");
                    } else {
                        System.out.println("  ❌ THẤT BẠI - Không thể thêm xe vào inventory!");
                    }
                }
                System.out.println("📊 Kết quả: " + successCount + "/" + order.getOrderDetails().size() + " thành công");
            }

            ensureDeliveryCreated(order.getPurchaseOrderId());
        }

        // 🔹 Gửi flash message về lại evmOrderList
        String msg = (newStatus == PurchaseOrderStatus.APPROVED)
                ? "✅ The order has been approved successfully!"
                : "❌ The order has been rejected!";
        redirectAttributes.addFlashAttribute("message", msg);
        redirectAttributes.addFlashAttribute("statusType", newStatus.name());

        return "redirect:/evm/orders/list";
    }

    @PostMapping("/delivery/{orderId}/status")
    public String updateDeliveryStatus(@PathVariable int orderId,
                                       @RequestParam("newStatus") String newStatus,
                                       RedirectAttributes redirectAttributes){
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
                        for (DTOPurchaseOrderDetail d : po.getOrderDetails()) {
                            int dealerId = po.getDealer().getDealerID();
                            int colorId = d.getColor() != null ? d.getColor().getColorID() : 0;
                            int versionId = d.getVersion() != null ? d.getVersion().getVersionID() : 0;
                            int qty = d.getQuantity();
                            if (colorId > 0 && versionId > 0 && qty > 0) {
                                inventoryDAO.addWhenDeliveryCompleted(orderId, dealerId, colorId, versionId, qty);
                            }
                        }
                    }
                } catch (Exception ex) {
                    // log and continue
                    System.out.println("Failed to add inventory on delivery completion: " + ex.getMessage());
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
}
