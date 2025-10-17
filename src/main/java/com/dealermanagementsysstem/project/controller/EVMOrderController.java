package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DAOPurchaseOrder;
import com.dealermanagementsysstem.project.Model.DAOEVMOrderProcessing;
import com.dealermanagementsysstem.project.Model.DTOPurchaseOrder;
import com.dealermanagementsysstem.project.Model.DTOEVMOrderProcessing;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/evm/orders")
public class EVMOrderController {

    private final DAOPurchaseOrder purchaseOrderDAO = new DAOPurchaseOrder();
    private final DAOEVMOrderProcessing processDAO = new DAOEVMOrderProcessing();

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

    // 🔹 Xử lý đơn hàng (phê duyệt / từ chối)
    @PostMapping("/process/{id}")
    public String processOrder(@PathVariable int id,
                               @ModelAttribute("process") DTOEVMOrderProcessing process,
                               RedirectAttributes redirectAttributes) {

        process.setPurchaseOrderId(id);
        process.setEvmStaffId(1); // demo
        processDAO.addProcessing(process);

        String newStatus = process.getActionType().equalsIgnoreCase("Approve") ? "Approved" : "Rejected";
        DTOPurchaseOrder order = purchaseOrderDAO.getPurchaseOrderById(id);
        order.setStatus(newStatus);
        purchaseOrderDAO.updatePurchaseOrderStatus(order.getPurchaseOrderId(), order.getStatus());

        // 🔹 Gửi flash message về lại evmOrderList
        String msg = newStatus.equals("Approved")
                ? " The order has been approved successfully!"
                : " The order has been rejected!";
        redirectAttributes.addFlashAttribute("message", msg);
        redirectAttributes.addFlashAttribute("statusType", newStatus);

        return "redirect:/evm/orders/evmOrderList";
    }
}
