package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DAODealerInventory;
import com.dealermanagementsysstem.project.Model.DAOVehicle;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/inventory")
public class InventoryController {

    private final DAODealerInventory dealerInventoryDao = new DAODealerInventory();
    private final DAOVehicle vehicleDao = new DAOVehicle();

    @GetMapping("/dealer")
    public String dealerInventory(@RequestParam("dealerId") int dealerId, Model model) {
        model.addAttribute("items", dealerInventoryDao.listByDealer(dealerId));
        return "evmPage/dealerInventory";
    }

    @GetMapping("/evm")
    public String evmInventory(Model model) {
        model.addAttribute("vehicles", vehicleDao.listCompanyInventory());
        return "evmPage/evmCompanyInventory";
    }
}


