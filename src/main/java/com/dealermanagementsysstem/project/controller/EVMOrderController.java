package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DAOPurchaseOrder;
import com.dealermanagementsysstem.project.Model.DAOEVMOrderProcessing;
import com.dealermanagementsysstem.project.Model.DTOPurchaseOrder;
import com.dealermanagementsysstem.project.Model.DTOEVMOrderProcessing;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/evm/orders")
public class EVMOrderController {

    private final DAOPurchaseOrder purchaseOrderDAO = new DAOPurchaseOrder();
    private final DAOEVMOrderProcessing processDAO = new DAOEVMOrderProcessing();

    // Hiển thị danh sách đơn hàng từ dealer (chưa xử lý)
    @GetMapping("/pending")
    public String showPendingOrders(Model model) {
        List<DTOPurchaseOrder> pendingOrders = purchaseOrderDAO.getAllPurchaseOrders()
                .stream()
                .filter(o -> "Pending".equalsIgnoreCase(o.getStatus()))
                .toList();
        model.addAttribute("orders", pendingOrders);
        return "evmPage/evmPendingOrders";
    }

    // Hiển thị form xử lý đơn hàng
    @GetMapping("/process/{id}")
    public String showProcessForm(@PathVariable int id, Model model) {
        DTOPurchaseOrder order = purchaseOrderDAO.getPurchaseOrderById(id);
        model.addAttribute("order", order);
        model.addAttribute("process", new DTOEVMOrderProcessing());
        return "evmPage/evmProcessOrder";
    }

    // Xử lý đơn hàng (phê duyệt / từ chối)
    @PostMapping("/process/{id}")
    public String processOrder(@PathVariable int id,
                               @ModelAttribute("process") DTOEVMOrderProcessing process) {
        // Giả sử EvmStaffID đang là 1 (sau này lấy từ session)
        process.setPurchaseOrderId(id);
        process.setEvmStaffId(1);

        processDAO.addProcessing(process);

        // Cập nhật trạng thái đơn hàng theo hành động
        String newStatus = process.getActionType().equalsIgnoreCase("Approve") ? "Approved" : "Rejected";
        DTOPurchaseOrder order = purchaseOrderDAO.getPurchaseOrderById(id);
        order.setStatus(newStatus);
        purchaseOrderDAO.updatePurchaseOrder(order);

        return "redirect:/evm/orders/pending";
    }

    // Xem lịch sử xử lý
    @GetMapping("/history/{id}")
    public String viewHistory(@PathVariable int id, Model model) {
        List<DTOEVMOrderProcessing> history = processDAO.getProcessHistoryByOrderId(id);
        model.addAttribute("history", history);
        model.addAttribute("orderId", id);
        return "evmPage/evmOrderHistory";
    }
}
