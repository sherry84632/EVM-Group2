package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DAOVehicle;
import com.dealermanagementsysstem.project.Model.DTOVehicle;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class SearchController {

    @GetMapping("/searchVehicleListController")
    public String searchVehicle(@RequestParam("keyword") String keyword, Model model) {
        DAOVehicle daoVehicle = new DAOVehicle();
        List<DTOVehicle> vehicles = daoVehicle.searchVehiclesByModelName(keyword);
        model.addAttribute("vehicleList", vehicles);
        model.addAttribute("keyword", keyword);
        return "evmPage/vehicleList";
    }
}

