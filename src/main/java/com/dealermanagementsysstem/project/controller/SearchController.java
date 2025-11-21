package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DAOVehicle;
import com.dealermanagementsysstem.project.Model.DTOVehicle;
import com.dealermanagementsysstem.project.Model.VehicleStatus;
import com.dealermanagementsysstem.project.util.SecurityUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class SearchController {

    @GetMapping("/searchVehicleListController")
    public String searchVehicle(@RequestParam(value="keyword", required=false) String keyword, Model model) {
        DAOVehicle daoVehicle = new DAOVehicle();
        if (keyword == null) keyword = "";
        List<DTOVehicle> vehicles = keyword.isBlank()? daoVehicle.getVehiclesByStatus(VehicleStatus.TEMPLATE) : daoVehicle.searchVehiclesByModelName(keyword);
        model.addAttribute("vehicles", vehicles);
        model.addAttribute("keyword", keyword);
        // set actionRole so create button vẫn hiển thị sau search
        String role = SecurityUtil.getCurrentUserRole();
        if (role != null) {
            if (role.equals("ROLE_EVM") || role.equals("ROLE_EVMSTAFF") || role.equals("ROLE_ADMIN")) {
                model.addAttribute("actionRole", "EVM");
            } else if (role.equals("ROLE_DEALER") || role.equals("ROLE_DEALERSTAFF")) {
                model.addAttribute("actionRole", "DEALER");
            }
        }
        return "evmPage/vehicleList";
    }
}
