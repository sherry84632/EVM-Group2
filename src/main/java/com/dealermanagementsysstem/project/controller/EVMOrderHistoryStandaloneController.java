package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Standalone controller for /evmOrderHistory route
 * Provides backward compatibility with existing template links
 */
@Controller
public class EVMOrderHistoryStandaloneController {

    private final DAOPurchaseOrder purchaseOrderDAO = new DAOPurchaseOrder();

    /**
     * Handle /evmOrderHistory (root level, not under /evm/orders)
     * This matches links in templates like vehicleDistributionManagement.html
     */
    @GetMapping("/evmOrderHistory")
    public String showOrderHistory(Model model,
                                   @RequestParam(required = false) String keyword) {

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

        return "evmPage/evmOrderHistory";
    }
}

