package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/quotation")
public class QuotationController {

    private final DAOQuotation dao = new DAOQuotation();

    // ✅ Hiển thị form báo giá
    @GetMapping("/new")
    public String showQuotationForm(
            @RequestParam("vin") String vin,
            HttpSession session,
            Model model
    ) {
        System.out.println("🧾 [DEBUG] Mở form báo giá cho VIN: " + vin);

        // 1️⃣ Lấy thông tin xe
        DTOVehicle vehicle = dao.getVehicleByVIN(vin);
        if (vehicle == null) {
            model.addAttribute("error", "Không tìm thấy thông tin xe.");
            return "dealerPage/errorPage";
        }

        // 2️⃣ Lấy thông tin dealer từ session
        DTOAccount account = (DTOAccount) session.getAttribute("user");
        if (account == null || account.getDealerId() == null) {
            model.addAttribute("error", "Bạn cần đăng nhập bằng tài khoản dealer!");
            return "mainPage/loginPage";
        }

        DTODealer dealer = dao.getDealerByID(account.getDealerId());
        if (dealer == null) {
            model.addAttribute("error", "Không tìm thấy thông tin đại lý.");
            return "dealerPage/errorPage";
        }

        // 3️⃣ Ngày tạo báo giá
        Timestamp createdAt = Timestamp.valueOf(LocalDateTime.now());

        // 4️⃣ Truyền dữ liệu sang view
        model.addAttribute("dealer", dealer);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("createdAt", createdAt);

        return "dealerPage/quotationForm"; // ✅ Tên file HTML của bạn
    }
}
