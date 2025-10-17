package com.dealermanagementsysstem.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class OrderVehicleController {

    @GetMapping("/orderVehicle")
    public String showOrderVehiclePage(@RequestParam("model") String model, Model modelAttr) {
        // Gửi tên xe được chọn sang trang HTML
        modelAttr.addAttribute("vehicleName", model);
        return "dealerPage/createACustomerOrder"; // Tên file HTML bạn đang dùng
    }
}
