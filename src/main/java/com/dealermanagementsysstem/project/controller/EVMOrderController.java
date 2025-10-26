package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/evm/orders")
public class EVMOrderController {

    private final DAOPurchaseOrder purchaseOrderDAO = new DAOPurchaseOrder();
    //private final DAOEVMOrderProcessing processDAO = new DAOEVMOrderProcessing();

    // 🔹 Hiển thị toàn bộ danh sách đơn hàng (EVM xem)
    @GetMapping("/evmOrderList")
    public String showAllOrders(Model model,
                                @ModelAttribute("message") String message,
                                @ModelAttribute("statusType") String statusType) {

        List<DTOPurchaseOrder> orders = purchaseOrderDAO.getAllPurchaseOrders();
        model.addAttribute("orders", orders);

        if (message != null && !message.isEmpty()) {
            model.addAttribute("message", message);
            model.addAttribute("statusType", statusType);
        }

        return "evmPage/evmOrderList";
    }

    // 🔹 Hiển thị chi tiết đơn hàng
    @GetMapping("/detail/{id}")
    public String showOrderDetail(@PathVariable("id") int orderId, Model model) {
        DTOPurchaseOrder order = purchaseOrderDAO.getPurchaseOrderById(orderId);
        if (order != null) {
            model.addAttribute("order", order);
            return "evmPage/orderDetail";
        } else {
            return "redirect:/evm/orders/evmOrderList?error=Order not found";
        }
    }

    // 🔹 Xử lý đơn hàng (phê duyệt / từ chối)
    @PostMapping("/process/{id}")
    public String processOrder(@PathVariable int id,
                               @ModelAttribute("process") DTOEVMOrderProcessing process,
                               RedirectAttributes redirectAttributes) {

        System.out.println("🔍 Processing order ID: " + id);

        //process.setPurchaseOrderId(id);
        //process.setEvmStaffId(1); // demo
        //processDAO.addProcessing(process);

        String newStatus = process.getActionType().equalsIgnoreCase("Approve") ? "Approved" : "Rejected";

        // ✅ Lấy chi tiết đơn hàng TRƯỚC KHI update status
        DTOPurchaseOrder order = purchaseOrderDAO.getPurchaseOrderById(id);

        if (order == null) {
            System.out.println("❌ Không tìm thấy đơn hàng với ID: " + id);
            redirectAttributes.addFlashAttribute("message", "❌ Order not found!");
            redirectAttributes.addFlashAttribute("statusType", "error");
            return "redirect:/evm/orders/evmOrderList";
        }

        System.out.println("📦 Order found - DealerID: " + order.getDealer().getDealerID() + ", Status: " + order.getStatus());

        // Update status
        order.setStatus(PurchaseOrderStatus.valueOf(newStatus));
        purchaseOrderDAO.updatePurchaseOrderStatus(order.getPurchaseOrderId(), order.getStatus());
        System.out.println("✅ Updated status to: " + newStatus);

        // ✅ Nếu đơn hàng được Approved, thêm xe vào inventory của dealer
        if ("Approved".equals(newStatus)) {
            System.out.println("🚗 Bắt đầu thêm xe vào inventory...");
            DAODealerInventory inventoryDAO = new DAODealerInventory();

            if (order.getOrderDetails() == null || order.getOrderDetails().isEmpty()) {
                System.out.println("⚠️ CẢNH BÁO: OrderDetails là NULL hoặc RỖNG!");
                System.out.println("⚠️ Không có xe nào để thêm vào inventory!");
            } else {
                System.out.println("📋 Tìm thấy " + order.getOrderDetails().size() + " chi tiết đơn hàng");

                int successCount = 0;
                for (DTOPurchaseOrderDetail detail : order.getOrderDetails()) {
                    System.out.println("  ➤ Thêm xe: ModelID=" + detail.getVersion().getModel()
                        + ", ColorID=" + detail.getColor().getColorID()
                        + ", Quantity=" + detail.getQuantity());

                    boolean added = inventoryDAO.addVehiclesToInventory(
                        order.getDealer().getDealerID(),
                        detail.getVersion().getModel().getModelID(),
                        detail.getColor().getColorID(),
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
        }

        // 🔹 Gửi flash message về lại evmOrderList
        String msg = newStatus.equals("Approved")
                ? "✅ The order has been approved successfully!"
                : "❌ The order has been rejected!";
        redirectAttributes.addFlashAttribute("message", msg);
        redirectAttributes.addFlashAttribute("statusType", newStatus);

        return "redirect:/evm/orders/evmOrderList";
    }
}
