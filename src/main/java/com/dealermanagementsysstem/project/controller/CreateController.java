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

    @PostMapping("/createVehicleController")
    public String createVehicle(
            @RequestParam("VIN") String VIN,
            @RequestParam("colorName") String colorName,
            @RequestParam("modelName") String modelName,
            @RequestParam("manufactureYear") int manufactureYear,
            @RequestParam("engineNumber") String engineNumber,
            @RequestParam("currentOwner") String currentOwner,
            @RequestParam("status") String status,
            Model model
    ) {
        DAOVehicle daoVehicle = new DAOVehicle();
        DAOVehicleModel daoModel = new DAOVehicleModel();
        DAOColor daoColor = new DAOColor();

        Integer modelID = daoVehicle.getModelIdByName(modelName);
        Integer colorID = daoVehicle.getColorIdByName(colorName);

        if (modelID == null || colorID == null) {
            model.addAttribute("error", "⚠️ ModelName hoặc ColorName không tồn tại trong hệ thống.");
            return "evmPage/createVehicle";
        }

        DTOVehicle v = new DTOVehicle();
        v.setVIN(VIN);
        v.setColorID(colorID);
        v.setModelID(modelID);
        v.setManufactureYear(manufactureYear);
        v.setEngineNumber(engineNumber);
        v.setCurrentOwner(currentOwner);
        v.setStatus(status);

        daoVehicle.insertVehicle(v);
        return "redirect:/vehicleList";
    }
}

