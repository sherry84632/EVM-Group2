package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Controller
public class QuotationController {

    private final DAOQuotation dao;

    public QuotationController() {
        this.dao = new DAOQuotation();
    }

    // ✅ Khi click vào “Tạo báo giá” từ Vehicle List
    @GetMapping("/createQuotationForm")
    public String showQuotationForm(
            @RequestParam("vehicleVIN") String vin,
            HttpSession session,
            Model model
    ) {
        // --- Lấy thông tin Dealer từ session ---
        DTOAccount account = (DTOAccount) session.getAttribute("user");
        if (account == null || account.getDealerId() == null) {
            model.addAttribute("error", "Bạn cần đăng nhập bằng tài khoản đại lý!");
            return "mainPage/loginPage";
        }

        // --- Lấy thông tin xe từ DB ---
        DTOVehicle vehicle = dao.getVehicleByVIN(vin);
        if (vehicle == null) {
            model.addAttribute("error", "Không tìm thấy thông tin xe!");
            return "redirect:/vehiclelist";
        }

        // --- Lấy thông tin Dealer (từ DB hoặc session) ---
        DTODealer dealer = dao.getDealerById(account.getDealerId());

        // --- Ngày tạo báo giá ---
        Timestamp createdAt = Timestamp.valueOf(LocalDateTime.now());

        // --- Truyền dữ liệu sang form báo giá ---
        model.addAttribute("dealer", dealer);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("createdAt", createdAt);

        // Form này chỉ để hiển thị, không lưu DB
        return "dealerPage/quotationForm";
    }
}
