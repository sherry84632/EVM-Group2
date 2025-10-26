package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.sql.Timestamp;

@Controller
public class CreateController {

    @PostMapping("/createVehicle")
    public String createVehicle(
            @RequestParam("VIN") String VIN,
            @RequestParam("colorName") String colorName,
            @RequestParam("modelName") String modelName,
            @RequestParam("manufactureYear") int manufactureYear,
            @RequestParam("engineNumber") String engineNumber,
            @RequestParam("ownerID") int ownerID,
            @RequestParam("currentDealerID") int currentDealerID,
            @RequestParam("versionID") int versionID,
            @RequestParam("status") String status,
            Model model
    ) throws SQLException {
        DAOVehicle daoVehicle = new DAOVehicle();
        DAOColor daoColor = new DAOColor();
        DAOCustomer daoCustomer = new DAOCustomer();
        DAODealer daoDealer = new DAODealer();
        DAOVehicleVersion daoVersion = new DAOVehicleVersion();

        // ✅ Lấy các entity objects
        DTOVehicleColor color = daoColor.getColorByColorName(colorName);
        DTOCustomer owner = daoCustomer.getCustomerById(ownerID);
        DTODealer currentDealer = daoDealer.getDealerById(currentDealerID);
        DTOVehicleVersion version = daoVersion.getVersionById(versionID);

        // ⚠️ Validation: Kiểm tra các entity tồn tại
        if (color == null) {
            model.addAttribute("error", "⚠️ ColorName không tồn tại trong hệ thống.");
            return "evmPage/createANewVehicleToList";
        }
        if (owner == null) {
            model.addAttribute("error", "⚠️ OwnerID không tồn tại trong hệ thống.");
            return "evmPage/createANewVehicleToList";
        }
        if (currentDealer == null) {
            model.addAttribute("error", "⚠️ CurrentDealerID không tồn tại trong hệ thống.");
            return "evmPage/createANewVehicleToList";
        }
        if (version == null) {
            model.addAttribute("error", "⚠️ VersionID không tồn tại trong hệ thống.");
            return "evmPage/createANewVehicleToList";
        }

        // ✅ Tạo đối tượng Vehicle với schema mới
        DTOVehicle v = new DTOVehicle();
        v.setVIN(VIN);
        v.setColor(color);
        v.setVersion(version);
        v.setManufactureYear(manufactureYear);
        v.setEngineNumber(engineNumber);
        v.setOwner(owner);
        v.setCurrentDealer(currentDealer);
        v.setStatus(VehicleStatus.valueOf(status));
        v.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        v.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        // ✅ Lưu vào DB
        daoVehicle.insertVehicle(v);

        // ✅ Redirect sang VehicleController hiển thị danh sách xe
        return "redirect:/getVehicleList";
    }
}
