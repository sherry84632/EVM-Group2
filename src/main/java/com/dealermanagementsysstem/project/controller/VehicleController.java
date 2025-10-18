package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DAOVehicle;
import com.dealermanagementsysstem.project.Model.DTOVehicle;
import com.dealermanagementsysstem.project.util.SecurityUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class VehicleController {
    @GetMapping("/getVehicleList")
    public String vehicleList(Model model)
    {
        DAOVehicle daoVehicle = new DAOVehicle();
        List<DTOVehicle> vehicle = daoVehicle.getVehicles();
        model.addAttribute("vehicleList", vehicle);
        return "evmPage/vehicleList";
    }

    @GetMapping("/getVehicleListToOrder")
    public String vehicleList2(Model model)
    {
        DAOVehicle daoVehicle = new DAOVehicle();
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
    public String vehicleList3(Model model)
    {
        DAOVehicle daoVehicle = new DAOVehicle();
        List<DTOVehicle> vehicle = daoVehicle.getVehicles();
        model.addAttribute("vehicleList", vehicle);
        return "dealerPage/dealerVehicleList";
    }
}
