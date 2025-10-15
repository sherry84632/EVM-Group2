package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DAOVehicle;
import com.dealermanagementsysstem.project.Model.DTOVehicle;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UpdateVehicleController {

    // ✅ Hiển thị form update
    @GetMapping("/updateVehicle")
    public String showUpdateForm(@RequestParam("vin") String vin, Model model) {
        DAOVehicle daoVehicle = new DAOVehicle();
        DTOVehicle vehicle = daoVehicle.getVehicleByVIN(vin);
        model.addAttribute("vehicle", vehicle);
        return "evmPage/updateVehicle"; // HTML hiển thị form
    }

    // ✅ Xử lý cập nhật khi nhấn Save
    @PostMapping("/updateVehicle")
    public String updateVehicle(@ModelAttribute DTOVehicle vehicle, RedirectAttributes redirectAttributes) {
        DAOVehicle daoVehicle = new DAOVehicle();
        boolean success = daoVehicle.updateVehicle(vehicle);

        if (success) {
            redirectAttributes.addFlashAttribute("message", "Vehicle updated successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Failed to update vehicle!");
        }

        return "redirect:/vehicleList"; // quay về danh sách xe
    }
}
