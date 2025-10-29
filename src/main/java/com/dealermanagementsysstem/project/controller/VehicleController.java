package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DAOAccount;
import com.dealermanagementsysstem.project.Model.DAOCustomer;
import com.dealermanagementsysstem.project.Model.DAOVehicle;
import com.dealermanagementsysstem.project.Model.DTOVehicle;
import com.dealermanagementsysstem.project.controller.base.BaseController;
import com.dealermanagementsysstem.project.service.support.AuthContextService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class VehicleController extends BaseController {
    @Autowired private DAOAccount daoAccount;
    @Autowired private DAOCustomer daoCustomer;
    @Autowired private DAOVehicle daoVehicle; // injected
    @Autowired private AuthContextService authContextService;

    @GetMapping("/getVehicleList")
    public String vehicleList(Model model, HttpSession session) {
        addUserContext(model, session, daoAccount);
        List<DTOVehicle> vehicle = daoVehicle.getVehicles();
        model.addAttribute("vehicleList", vehicle);
        return "evmPage/vehicleList";
    }

    @GetMapping("/getVehicleListToOrder")
    public String vehicleList2(Model model, HttpSession session) {
        addUserContext(model, session, daoAccount);
        List<DTOVehicle> vehicle = daoVehicle.getVehicles();
        model.addAttribute("vehicleList", vehicle);
        
        // Add user information for Spring Security
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            model.addAttribute("currentUser", auth.getName());
            // For testing purposes, set a default dealerStaffId
            // In a real application, you would get this from the database based on the authenticated user
            model.addAttribute("dealerStaffId", 1); // Default test value
        }
        
        return "dealerPage/chooseVehicleToOrder";
    }

    @GetMapping("/getVehicleListToCreateQuotation")
    public String vehicleList3(Model model, HttpSession session) {
        addUserContext(model, session, daoAccount);
        List<DTOVehicle> vehicle = daoVehicle.getVehicles();
        model.addAttribute("vehicleList", vehicle);
        // Load customers for multi-select quotation creation using injected DAO
        model.addAttribute("customerList", daoCustomer.getAllCustomers());
        return "dealerPage/dealerVehicleList";
    }
}
