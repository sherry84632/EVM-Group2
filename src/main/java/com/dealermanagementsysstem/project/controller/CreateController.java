package com.dealermanagementsysstem.project.controller;
import com.dealermanagementsysstem.project.Model.DAOColor;
import com.dealermanagementsysstem.project.Model.DAOVehicle;
import com.dealermanagementsysstem.project.Model.DAOVehicleModel;
import com.dealermanagementsysstem.project.Model.DTOVehicle;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class CreateController {

    @GetMapping("/createVehicleForm")
    public String showCreateForm(Model model) {
        DAOVehicleModel daoModel = new DAOVehicleModel();
        DAOColor daoColor = new DAOColor();

        model.addAttribute("vehicle", new DTOVehicle());
        model.addAttribute("modelList", daoModel.getAllModels());
        model.addAttribute("colorList", daoColor.getAllColors());
        return "evmPage/createVehicle";
    }

    @PostMapping("/createVehicle")
    public String createVehicle(@ModelAttribute("vehicle") DTOVehicle vehicle) {
        DAOVehicle daoVehicle = new DAOVehicle();
        daoVehicle.insertVehicle(vehicle);
        return "redirect:/vehicleList";
    }
}

