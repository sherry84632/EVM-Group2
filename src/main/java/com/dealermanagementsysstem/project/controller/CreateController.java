package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;

@Controller
public class CreateController {

    @Autowired private DAOVehicle daoVehicle;
    @Autowired private DAOColor daoColor;
    @Autowired private DAOVehicleVersion daoVersion;

    private static final Logger log = LoggerFactory.getLogger(CreateController.class);

    @PostMapping("/createVehicle")
    public String createVehicle(@RequestParam("colorName") String colorName,
                                 @RequestParam("modelName") String modelName,
                                 @RequestParam("manufactureYear") int manufactureYear,
                                 @RequestParam("engineNumber") String engineNumber,
                                 @RequestParam("versionID") int versionID,
                                 @RequestParam("status") String status,
                                 Model model) {
        DTOVehicleColor color = daoColor.getColorByColorName(colorName);
        DTOVehicleVersion version = daoVersion.getVersionById(versionID);
        if (color == null) { model.addAttribute("error", "⚠️ ColorName không tồn tại trong hệ thống."); return "evmPage/createANewVehicleToList"; }
        if (version == null) { model.addAttribute("error", "⚠️ VersionID không tồn tại trong hệ thống."); return "evmPage/createANewVehicleToList"; }
        DTOVehicle v = new DTOVehicle();
        v.setColor(color); v.setVersion(version); v.setManufactureYear(manufactureYear); v.setEngineNumber(engineNumber);
        try { v.setStatus(VehicleStatus.valueOf(status)); } catch (IllegalArgumentException ex) { model.addAttribute("error","⚠️ Trạng thái không hợp lệ."); return "evmPage/createANewVehicleToList"; }
        v.setCreatedAt(new Timestamp(System.currentTimeMillis())); v.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        daoVehicle.insertVehicle(v);
        log.info("✅ Created vehicle modelName={} versionID={} colorName={} year={}", modelName, versionID, colorName, manufactureYear);
        return "redirect:/getVehicleList";
    }
}
