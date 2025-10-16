package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.*;
import com.dealermanagementsysstem.project.Model.DAOQuotation;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.List;

@Controller
@RequestMapping("/quotation")
public class QuotationController {

    private final DAOQuotation dao = new DAOQuotation();

    // ✅ 1. Hiển thị danh sách tất cả báo giá
    @GetMapping
    public String listQuotations(Model model) {
        List<DTOQuotation> list = dao.getAllQuotations();
        model.addAttribute("quotations", list);
        return "quotation-list"; // -> tên file HTML hiển thị danh sách
    }

    // ✅ 2. Hiển thị form thêm mới báo giá
    @GetMapping("/new")
    public String showNewForm(Model model) {
        DTOQuotation quotation = new DTOQuotation();
        model.addAttribute("quotation", quotation);
        return "quotation-form"; // -> form thêm mới
    }

    // ✅ 3. Thêm mới báo giá
    @PostMapping("/insert")
    public String insertQuotation(
            @RequestParam("dealerId") int dealerId,
            @RequestParam("customerId") int customerId,
            @RequestParam("vehicleVin") String vehicleVin,
            @RequestParam(value = "discountPolicyId", required = false) Integer discountPolicyId,
            @RequestParam(value = "status", required = false, defaultValue = "Pending") String status
    ) {
        DTOQuotation q = new DTOQuotation();

        // Tạo các đối tượng liên kết
        DTODealer dealer = new DTODealer();
        dealer.setDealerID(dealerId);
        q.setDealer(dealer);

        DTOCustomer customer = new DTOCustomer();
        customer.setCustomerID(customerId);
        q.setCustomer(customer);

        DTOVehicle vehicle = new DTOVehicle();
        vehicle.setVIN(vehicleVin);
        q.setVehicle(vehicle);

        if (discountPolicyId != null) {
            DTODiscountPolicy dp = new DTODiscountPolicy();
            dp.setPolicyID(discountPolicyId);
            q.setDiscountPolicy(dp);
        }

        q.setStatus(status);
        q.setCreatedAt(new Timestamp(System.currentTimeMillis()));

        // Gọi DAO
        boolean success = dao.insertQuotation(q);
        return success ? "redirect:/quotation" : "redirect:/quotation/new?error=1";
    }

    // ✅ 4. Tìm kiếm báo giá theo từ khóa
    @GetMapping("/search")
    public String searchQuotation(@RequestParam("keyword") String keyword, Model model) {
        List<DTOQuotation> list = dao.searchQuotation(keyword);
        model.addAttribute("quotations", list);
        model.addAttribute("keyword", keyword);
        return "quotation-list";
    }

    // ✅ 5. Cập nhật trạng thái báo giá
    @PostMapping("/update-status")
    public String updateStatus(@RequestParam("quotationId") int quotationId,
                               @RequestParam("newStatus") String newStatus) {
        boolean success = dao.updateQuotationStatus(quotationId, newStatus);
        return "redirect:/quotation";
    }

}
