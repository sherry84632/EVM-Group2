package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DAOCustomer;
import com.dealermanagementsysstem.project.Model.DAOVehicle;
import com.dealermanagementsysstem.project.Model.DTOVehicle;
import com.dealermanagementsysstem.project.Model.VehicleStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Controller
public class VehicleController {

    private static final Logger log = LoggerFactory.getLogger(VehicleController.class);

    @Autowired
    private DAOCustomer daoCustomer;

    @GetMapping("/getVehicleList")
    public String vehicleList(Model model) {
        DAOVehicle daoVehicle = new DAOVehicle();
        List<DTOVehicle> vehicle = getTemplateVehicles(daoVehicle);
        model.addAttribute("vehicleList", vehicle);
        return "evmPage/vehicleList";
    }

    @GetMapping("/getVehicleListToOrder")
    public String vehicleList2(Model model) {
        DAOVehicle daoVehicle = new DAOVehicle();
        List<DTOVehicle> vehicle = getTemplateVehicles(daoVehicle);
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
    public String vehicleList3(Model model) {
        DAOVehicle daoVehicle = new DAOVehicle();
        List<DTOVehicle> vehicle = getTemplateVehicles(daoVehicle);
        model.addAttribute("vehicleList", vehicle);
        // Load customers for multi-select quotation creation using injected DAO
        model.addAttribute("customerList", daoCustomer.getAllCustomers());
        return "dealerPage/dealerVehicleList";
    }

    /**
     * Helper method to get TEMPLATE vehicles (catalog) with fallback to all vehicles
     * for backward compatibility during migration
     */
    private List<DTOVehicle> getTemplateVehicles(DAOVehicle daoVehicle) {
        // Try to get TEMPLATE vehicles first
        List<DTOVehicle> vehicles = daoVehicle.getVehiclesByStatus(VehicleStatus.TEMPLATE);

        // Fallback to all vehicles if no TEMPLATE vehicles exist (backward compatibility)
        if (vehicles == null || vehicles.isEmpty()) {
            log.warn("No TEMPLATE vehicles found, falling back to all vehicles for backward compatibility");
            vehicles = daoVehicle.getVehicles();
        }

        return vehicles;
    }
}
