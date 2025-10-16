package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DAOVehicle;
import com.dealermanagementsysstem.project.Model.DTOVehicle;
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
