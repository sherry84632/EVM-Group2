package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DAOCustomer;
import com.dealermanagementsysstem.project.Model.DAOVehicle;
import com.dealermanagementsysstem.project.Model.DTOVehicle;
import com.dealermanagementsysstem.project.Model.VehicleStatus;
import com.dealermanagementsysstem.project.Model.DAOAccount;
import com.dealermanagementsysstem.project.Model.DTOCustomer;
import com.dealermanagementsysstem.project.Model.DAODealerModelPrice;
import com.dealermanagementsysstem.project.Model.DTOAccount;
import jakarta.servlet.http.HttpSession;
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

    @Autowired
    private DAOAccount daoAccount;

    @GetMapping("/getVehicleList")
    public String vehicleList(Model model) {
        DAOVehicle daoVehicle = new DAOVehicle();
        List<DTOVehicle> vehicle = getTemplateVehicles(daoVehicle);
        model.addAttribute("vehicleList", vehicle);
        return "evmPage/vehicleList";
    }

    @GetMapping("/getVehicleListToOrder")
    public String vehicleList2(Model model, HttpSession session) {
        DAOVehicle daoVehicle = new DAOVehicle();
        List<DTOVehicle> vehicle = getTemplateVehicles(daoVehicle);
        // Apply dealer model prices if dealer context
        applyDealerPrices(session, vehicle);
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
        DAOVehicle daoVehicle = new DAOVehicle();
        List<DTOVehicle> vehicle = getTemplateVehicles(daoVehicle);
        applyDealerPrices(session, vehicle);
        model.addAttribute("vehicleList", vehicle);

        //  Filter customers by dealerId
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                String email = auth.getName();
                Integer dealerId = daoAccount.getDealerIdByEmail(email);

                List<DTOCustomer> customerList;
                if (dealerId != null && dealerId > 0) {
                    // Dealer user: only show customers belonging to this dealer
                    customerList = daoCustomer.getCustomersByDealerId(dealerId);
                    log.info(" Loaded {} customers for dealerId={}", customerList.size(), dealerId);
                } else {
                    // Admin/EVM user: show all customers
                    customerList = daoCustomer.getAllCustomers();
                    log.info("No dealerId found for email={}, loaded all {} customers", email, customerList.size());
                }
                model.addAttribute("customerList", customerList);
            } else {
                // Not authenticated: show empty list
                log.warn(" User not authenticated, showing empty customer list");
                model.addAttribute("customerList", List.of());
            }
        } catch (Exception e) {
            log.error(" Error loading customers", e);
            model.addAttribute("customerList", List.of());
        }

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

    private void applyDealerPrices(HttpSession session, List<DTOVehicle> vehicles) {
        if (vehicles == null || vehicles.isEmpty()) return;
        Object accObj = session != null ? session.getAttribute("loggedInAccount") : null;
        if (!(accObj instanceof DTOAccount acc) || acc.getDealerStaff()==null || acc.getDealerStaff().getDealer()==null) return;
        int dealerId = acc.getDealerStaff().getDealer().getDealerID();
        DAODealerModelPrice dmp = new DAODealerModelPrice();
        var modelPriceList = dmp.listDealerModels(dealerId);
        java.util.Map<Integer, java.math.BigDecimal> priceMap = new java.util.HashMap<>();
        for (var m : modelPriceList) {
            Object mid = m.get("modelID"); Object p = m.get("dealerPrice");
            if (mid instanceof Integer && p instanceof java.math.BigDecimal) priceMap.put((Integer)mid,(java.math.BigDecimal)p);
        }
        for (DTOVehicle v : vehicles) {
            Integer mid = v.getModelID(); if (mid!=null && priceMap.containsKey(mid)) v.setDealerSellingPrice(priceMap.get(mid));
        }
    }
}
