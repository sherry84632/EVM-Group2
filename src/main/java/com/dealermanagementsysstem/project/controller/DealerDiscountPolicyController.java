package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DAODealer;
import com.dealermanagementsysstem.project.Model.DAODiscountPolicy;
import com.dealermanagementsysstem.project.Model.DTODiscountPolicy;
import com.dealermanagementsysstem.project.Model.DiscountPolicyStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/discount-policy")
public class DealerDiscountPolicyController {

    private final DAODiscountPolicy daoPolicy = new DAODiscountPolicy();

    // ✅ Trang hiển thị list + search + form
    @GetMapping
    public String showDiscountPolicyPage(
            @RequestParam(value = "keyword", required = false) String keyword,
            Model model
    ) {
        List<DTODiscountPolicy> policies;

        if (keyword != null && !keyword.trim().isEmpty()) {
            policies = daoPolicy.searchPolicyByName(keyword);
            model.addAttribute("keyword", keyword);
        } else {
            policies = daoPolicy.getAllPolicies();
        }

        model.addAttribute("policies", policies);
        model.addAttribute("newPolicy", new DTODiscountPolicy());
        return "evmPage/evmDiscountPolicyManagement"; // ⚙️ file HTML
    }

    // ✅ Tạo mới Discount Policy
    @PostMapping("/create")
    public String createDiscountPolicy(
            @RequestParam("policyName") String policyName,
            @RequestParam("description") String description,
            @RequestParam("hangPercent") double hangPercent,
            @RequestParam("dailyPercent") double dailyPercent,
            @RequestParam("status") String status,
            @RequestParam("dealerId") int dealerId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model
    ) throws SQLException {

        DAODealer daoDealer = new DAODealer();

        DTODiscountPolicy dto = new DTODiscountPolicy();
        dto.setDealer(daoDealer.getDealerById(dealerId));
        dto.setPolicyName(policyName);
        dto.setDescription(description);
        dto.setHangPercent(BigDecimal.valueOf(hangPercent));
        dto.setDailyPercent(BigDecimal.valueOf(dailyPercent));
        dto.setStatus(DiscountPolicyStatus.valueOf(status.toUpperCase()));
        dto.setStartDate(startDate);
        dto.setEndDate(endDate);

        boolean success = daoPolicy.createDiscountPolicy(dto);

        if (success) {
            model.addAttribute("message", "✅ Created Discount Policy successfully!");
        } else {
            model.addAttribute("error", "❌ Failed to create Discount Policy. Check data or Dealer ID!");
        }

        List<DTODiscountPolicy> policies = daoPolicy.getAllPolicies();
        model.addAttribute("policies", policies);
        return "evmPage/evmDiscountPolicyManagement";
    }
}
