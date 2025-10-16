package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DAODiscountPolicy;
import com.dealermanagementsysstem.project.Model.DTODiscountPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/discount-policy")
public class DiscountPolicyController {

    private final DAODiscountPolicy dao;

    @Autowired
    public DiscountPolicyController(DAODiscountPolicy dao) {
        this.dao = dao;
    }

    // 🟢 [GET] /discount-policy
    @GetMapping
    public String listPolicies(Model model) throws SQLException {
        List<DTODiscountPolicy> policies = dao.getAllPolicies();
        model.addAttribute("policies", policies);
        return "templates/evmPage/discountPolicyList"; // bỏ .html vì Thymeleaf tự hiểu
    }

    // 🟢 [GET] /discount-policy/create
    @GetMapping("/create")
    public String showCreateForm() {
        return "templates/evmPage/createDiscountPolicy";
    }

    // 🟢 [POST] /discount-policy
    @PostMapping
    public String createPolicy(
            @RequestParam("dealerId") int dealerId,
            @RequestParam("policyName") String policyName,
            @RequestParam("description") String description,
            @RequestParam("hangPercent") double hangPercent,
            @RequestParam("dailyPercent") double dailyPercent,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) throws SQLException {

        DTODiscountPolicy dto = new DTODiscountPolicy(
                0, dealerId, policyName, description,
                hangPercent, dailyPercent, startDate, endDate, "Active"
        );
        dao.addPolicy(dto);
        return "redirect:/discount-policy";
    }

    // 🟢 [POST] /discount-policy/apply
    @PostMapping("/apply")
    @ResponseBody
    public String applyPolicy(
            @RequestParam("policyId") int policyId,
            @RequestParam("orderDetailId") int orderDetailId
    ) throws SQLException {

        boolean ok = dao.applyPolicyToSaleOrderDetail(orderDetailId, policyId);
        if (ok) {
            return "Đã áp dụng chính sách cho chi tiết đơn hàng.";
        } else {
            return "Áp dụng thất bại.";
        }
    }
}
