package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.*;
import com.dealermanagementsysstem.project.util.SecurityUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/discount-policy")
public class DealerDiscountPolicyController {

    private final DAODiscountPolicy daoPolicy = new DAODiscountPolicy();
    private final DAODealer daoDealer = new DAODealer();

    @Autowired
    private DAOAccount daoAccount;

    // ✅ Show list page with search - FILTERED BY DEALER
    @GetMapping
    public String showDiscountPolicyPage(
            @RequestParam(value = "keyword", required = false) String keyword,
            Model model,
            HttpSession session
    ) {
        // Get current logged-in account
        DTOAccount loggedInAccount = (DTOAccount) session.getAttribute("loggedInAccount");

        List<DTODiscountPolicy> policies;

        // Determine dealer ID for filtering
        Integer dealerIdFilter = null;
        if (loggedInAccount != null && loggedInAccount.getDealerStaff() != null
            && loggedInAccount.getDealerStaff().getDealer() != null) {
            dealerIdFilter = loggedInAccount.getDealerStaff().getDealer().getDealerID();
            model.addAttribute("dealerFiltered", true);
        }

        // Get policies with optional filtering
        if (keyword != null && !keyword.trim().isEmpty()) {
            if (dealerIdFilter != null) {
                policies = daoPolicy.searchPolicyByNameAndDealer(keyword, dealerIdFilter);
            } else {
                policies = daoPolicy.searchPolicyByName(keyword);
            }
            model.addAttribute("keyword", keyword);
        } else {
            if (dealerIdFilter != null) {
                policies = daoPolicy.getPoliciesByDealerId(dealerIdFilter);
            } else {
                policies = daoPolicy.getAllPolicies();
            }
        }

        // Add dealer list for dropdown (only for admin/EVM)
        if (dealerIdFilter == null) {
            try {
                List<DTODealer> dealers = daoDealer.getAllDealers();
                model.addAttribute("dealers", dealers);
            } catch (Exception e) {
                System.out.println("⚠️ Could not load dealers: " + e.getMessage());
            }
        } else {
            // For dealer staff, only show their dealer
            try {
                DTODealer currentDealer = daoDealer.getDealerById(dealerIdFilter);
                if (currentDealer != null) {
                    model.addAttribute("dealers", List.of(currentDealer));
                    model.addAttribute("currentDealerId", dealerIdFilter);
                }
            } catch (Exception e) {
                System.out.println("⚠️ Could not load dealer: " + e.getMessage());
            }
        }

        model.addAttribute("policies", policies);
        model.addAttribute("newPolicy", new DTODiscountPolicy());
        return "evmPage/evmDiscountPolicyManagement";
    }

    // ✅ CREATE - Create new Discount Policy
    @PostMapping("/create")
    public String createDiscountPolicy(
            @RequestParam("policyName") String policyName,
            @RequestParam("description") String description,
            @RequestParam(value = "discountPercent", required = false, defaultValue = "0") Double discountPercent,
            @RequestParam("hangPercent") Double hangPercent,
            @RequestParam("dailyPercent") Double dailyPercent,
            @RequestParam("dealerId") int dealerId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            RedirectAttributes redirectAttributes
    ) {
        try {
            DTODealer dealer = daoDealer.getDealerById(dealerId);
            if (dealer == null) {
                redirectAttributes.addFlashAttribute("error", "❌ Dealer not found!");
                return "redirect:/discount-policy";
            }

            DTODiscountPolicy dto = new DTODiscountPolicy();
            dto.setDealer(dealer);
            dto.setPolicyName(policyName);
            dto.setDescription(description);
            dto.setDiscountPercent(discountPercent != null ? BigDecimal.valueOf(discountPercent) : null);
            dto.setHangPercent(BigDecimal.valueOf(hangPercent));
            dto.setDailyPercent(BigDecimal.valueOf(dailyPercent));
            dto.setStatus(DiscountPolicyStatus.ACTIVE);
            dto.setStartDate(startDate);
            dto.setEndDate(endDate);

            boolean success = daoPolicy.createDiscountPolicy(dto);

            if (success) {
                redirectAttributes.addFlashAttribute("message", "✅ Created Discount Policy successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "❌ Failed to create Discount Policy!");
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "❌ Error: " + e.getMessage());
            e.printStackTrace();
        }

        return "redirect:/discount-policy";
    }

    // ✅ READ - Get policy detail for editing
    @GetMapping("/detail/{id}")
    public String getPolicyDetail(@PathVariable int id, Model model) {
        DTODiscountPolicy policy = daoPolicy.getPolicyById(id);
        if (policy == null) {
            model.addAttribute("error", "❌ Policy not found!");
            return "redirect:/discount-policy";
        }

        List<DTODealer> dealers = daoDealer.getAllDealers();
        model.addAttribute("dealers", dealers);
        model.addAttribute("policy", policy);
        model.addAttribute("policies", daoPolicy.getAllPolicies());
        return "evmPage/evmDiscountPolicyManagement";
    }

    // ✅ UPDATE - Update existing policy
    @PostMapping("/update/{id}")
    public String updateDiscountPolicy(
            @PathVariable int id,
            @RequestParam("policyName") String policyName,
            @RequestParam("description") String description,
            @RequestParam(value = "discountPercent", required = false, defaultValue = "0") Double discountPercent,
            @RequestParam("hangPercent") Double hangPercent,
            @RequestParam("dailyPercent") Double dailyPercent,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam("status") String status,
            RedirectAttributes redirectAttributes
    ) {
        try {
            DTODiscountPolicy policy = daoPolicy.getPolicyById(id);
            if (policy == null) {
                redirectAttributes.addFlashAttribute("error", "❌ Policy not found!");
                return "redirect:/discount-policy";
            }

            policy.setPolicyName(policyName);
            policy.setDescription(description);
            policy.setDiscountPercent(discountPercent != null ? BigDecimal.valueOf(discountPercent) : null);
            policy.setHangPercent(BigDecimal.valueOf(hangPercent));
            policy.setDailyPercent(BigDecimal.valueOf(dailyPercent));
            policy.setStartDate(startDate);
            policy.setEndDate(endDate);

            try {
                policy.setStatus(DiscountPolicyStatus.valueOf(status.toUpperCase()));
            } catch (IllegalArgumentException e) {
                policy.setStatus(DiscountPolicyStatus.ACTIVE);
            }

            boolean success = daoPolicy.updateDiscountPolicy(policy);

            if (success) {
                redirectAttributes.addFlashAttribute("message", "✅ Updated Discount Policy successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "❌ Failed to update Discount Policy!");
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "❌ Error: " + e.getMessage());
            e.printStackTrace();
        }

        return "redirect:/discount-policy";
    }

    // ✅ DELETE - Delete policy
    @PostMapping("/delete/{id}")
    public String deleteDiscountPolicy(@PathVariable int id, RedirectAttributes redirectAttributes) {
        try {
            boolean success = daoPolicy.deleteDiscountPolicy(id);

            if (success) {
                redirectAttributes.addFlashAttribute("message", "✅ Deleted Discount Policy successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "❌ Policy not found!");
            }

        } catch (RuntimeException e) {
            if (e.getMessage().contains("referenced")) {
                redirectAttributes.addFlashAttribute("error",
                    "❌ Cannot delete! This policy is still referenced by purchase orders.");
            } else {
                redirectAttributes.addFlashAttribute("error", "❌ Error: " + e.getMessage());
            }
            e.printStackTrace();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "❌ Error: " + e.getMessage());
            e.printStackTrace();
        }

        return "redirect:/discount-policy";
    }
}
