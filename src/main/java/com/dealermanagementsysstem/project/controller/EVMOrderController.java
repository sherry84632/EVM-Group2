package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DAOPurchaseOrder;
import com.dealermanagementsysstem.project.Model.DAOEVMOrderProcessing;
import com.dealermanagementsysstem.project.Model.DAOPurchaseOrderDetail;
import com.dealermanagementsysstem.project.Model.DAODealerInventory;
import com.dealermanagementsysstem.project.Model.DAOVehicle;
import com.dealermanagementsysstem.project.Model.DAODelivery;
import com.dealermanagementsysstem.project.Model.DAODeliveryDetail;
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
    private final DAOPurchaseOrderDetail orderDetailDao = new DAOPurchaseOrderDetail();
    private final DAODealerInventory dealerInventoryDao = new DAODealerInventory();
    private final DAOVehicle vehicleDao = new DAOVehicle();
    private final DAODelivery deliveryDao = new DAODelivery();
    private final DAODeliveryDetail deliveryDetailDao = new DAODeliveryDetail();

    // Hiển thị danh sách đơn hàng từ dealer (chưa xử lý)
    @GetMapping("/pending")
    public String showPendingOrders(Model model) {
        List<DTOPurchaseOrder> pendingOrders = purchaseOrderDAO.getAllPurchaseOrders()
                .stream()
                .filter(o -> "Pending".equalsIgnoreCase(o.getStatus()))
                .collect(java.util.stream.Collectors.toList());
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

        DTOPurchaseOrder order = purchaseOrderDAO.getPurchaseOrderById(id);
        if (process.getActionType().equalsIgnoreCase("Approve")) {
            // Allocate inventory per PurchaseOrderDetail
            java.util.List<com.dealermanagementsysstem.project.Model.DTOPurchaseOrderDetail> details = orderDetailDao.getDetailsByOrderId(id);
            java.util.List<String> allocatedVins = new java.util.ArrayList<>();
            for (com.dealermanagementsysstem.project.Model.DTOPurchaseOrderDetail d : details) {
                java.util.List<String> vins = vehicleDao.getAvailableVINsByColor(d.getColorId(), d.getQuantity());
                if (vins.size() < d.getQuantity()) {
                    // Not enough stock -> mark rejected and stop
                    order.setStatus("Rejected");
                    purchaseOrderDAO.updatePurchaseOrder(order);
                    return "redirect:/evm/orders/pending?error=out_of_stock";
                }
                for (String vin : vins) {
                    dealerInventoryDao.insertInventory(order.getDealerId(), vin, "IN_STOCK");
                    vehicleDao.markVehicleSold(vin);
                    allocatedVins.add(vin);
                }
            }
            // Create delivery record and delivery details
            Integer deliveryId = deliveryDao.createDelivery(order.getPurchaseOrderId(), order.getDealerId(), "Completed");
            if (deliveryId != null) {
                for (String vin : allocatedVins) {
                    deliveryDetailDao.addDeliveryDetail(deliveryId, vin);
                }
            }
            order.setStatus("Approved");
            purchaseOrderDAO.updatePurchaseOrder(order);
        } else {
            order.setStatus("Rejected");
            purchaseOrderDAO.updatePurchaseOrder(order);
        }

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
