package com.dealermanagementsysstem.project.controller;


import com.dealermanagementsysstem.project.Model.DAOPurchaseOrder;
import com.dealermanagementsysstem.project.Model.DTOPurchaseOrder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class EVMMainController {
    private final DAOPurchaseOrder purchaseOrderDAO = new DAOPurchaseOrder();

    @GetMapping("/evmOrderList")
    public String showEvmOrders(Model model) {
        List<DTOPurchaseOrder> orders = purchaseOrderDAO.getAllPurchaseOrders();
        model.addAttribute("orders", orders);
        return "evmPage/evmOrderList";
    }
}
