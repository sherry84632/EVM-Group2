package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.*;
import com.dealermanagementsysstem.project.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DealerDiscountPolicyController - Manages discount policies
 *
 * NOTE: This controller uses deprecated DAODiscountPolicy for backward compatibility.
 * For new promo code features, create a new controller using DAOPromoCode.
 */
@Controller
@RequestMapping("/discount-policy")
@SuppressWarnings("deprecation")
public class DealerDiscountPolicyController {

    private final DAODiscountPolicy daoPolicy = new DAODiscountPolicy();
    private final DAODealer daoDealer = new DAODealer();

    @Autowired
    private DAOAccount daoAccount;

    //  Show list page with search - FILTERED BY DEALER
    @GetMapping
    public String showDiscountPolicyPage(
            @RequestParam(value = "keyword", required = false) String keyword,
            Model model
    ) {
        List<DTODiscountPolicy> policies;

        //  Get dealer ID from logged-in user's email
        Integer dealerIdFilter = getDealerIdFromSession();

        if (dealerIdFilter != null) {
            model.addAttribute("dealerFiltered", true);
            System.out.println(" Filtering discount policies for DealerID: " + dealerIdFilter);
        } else {
            System.out.println(" No dealer found for current user - showing all policies");
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
                //  Only show this dealer's policies
                policies = daoPolicy.getPoliciesByDealerId(dealerIdFilter);
            } else {
                // For EVM/Admin - show all
                policies = daoPolicy.getAllPolicies();
            }
        }

        // Load vehicle models for selection
        try {
            DAOVehicleModel daoVehicleModel = new DAOVehicleModel();
            List<DTOVehicleModel> vehicleModels = daoVehicleModel.getAllModels();
            model.addAttribute("vehicleModels", vehicleModels);
        } catch (Exception e) {
            System.out.println("⚠️ Could not load vehicle models: " + e.getMessage());
        }

        // Add dealer list for dropdown (only for admin/EVM)
        if (dealerIdFilter == null) {
            try {
                List<DTODealer> dealers = daoDealer.getAllDealers();
                model.addAttribute("dealers", dealers);
            } catch (Exception e) {
                System.out.println(" Could not load dealers: " + e.getMessage());
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
                System.out.println(" Could not load dealer: " + e.getMessage());
            }
        }

        // Load dealer info for each policy and store in a Map
        java.util.Map<Integer, DTODealer> dealerMap = new java.util.HashMap<>();
        for (DTODiscountPolicy policy : policies) {
            if (policy.getDealerID() != null && policy.getDealerID() > 0) {
                try {
                    DTODealer dealer = daoDealer.getDealerById(policy.getDealerID());
                    if (dealer != null) {
                        dealerMap.put(policy.getPolicyID(), dealer);
                    }
                } catch (Exception e) {
                    System.out.println("⚠️ Could not load dealer for policy " + policy.getPolicyID());
                }
            }
        }
        model.addAttribute("dealerMap", dealerMap);

        model.addAttribute("policies", policies);
        model.addAttribute("newPolicy", new DTODiscountPolicy());
        return "evmPage/evmDiscountPolicyManagement";
    }

    /**
     * Helper method to get dealer ID from current logged-in user
     * Returns null if user is not associated with a dealer (EVM/Admin)
     */
    private Integer getDealerIdFromSession() {
        String email = SecurityUtil.getCurrentUserEmail();
        if (email == null) {
            System.out.println(" No user email found in session");
            return null;
        }

        Integer dealerId = daoAccount.getDealerIdByEmail(email);
        if (dealerId == null) {
            System.out.println(" No dealer found for email: " + email);
        }
        return dealerId;
    }

    //  CREATE - Create new Discount Policy (Customer Promo Code)
    @PostMapping("/create")
    public String createDiscountPolicy(
            @RequestParam("policyName") String policyName,
            @RequestParam("description") String description,
            @RequestParam("discountPercent") Double discountPercent,
            @RequestParam("dealerId") int dealerId,
            @RequestParam("applicableModels") String applicableModels,
            @RequestParam(value = "selectedModels", required = false) List<Integer> selectedModels,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            RedirectAttributes redirectAttributes
    ) {
        try {
            // Verify dealer exists
            DTODealer dealer = daoDealer.getDealerById(dealerId);
            if (dealer == null) {
                redirectAttributes.addFlashAttribute("error", "❌ Dealer not found!");
                return "redirect:/discount-policy";
            }

            DTODiscountPolicy dto = new DTODiscountPolicy();

            // Set main fields for customer discount
            dto.setDealerID(dealerId);
            dto.setPolicyName(policyName);
            dto.setDescription(description);
            dto.setDiscountPercent(BigDecimal.valueOf(discountPercent));
            dto.setStatus(DiscountPolicyStatus.ACTIVE);
            dto.setStartDate(startDate);
            dto.setEndDate(endDate);
            dto.setCreatedBy(SecurityUtil.getCurrentUserEmail());

            // Handle applicable vehicle models
            if ("SPECIFIC".equals(applicableModels) && selectedModels != null && !selectedModels.isEmpty()) {
                // Convert list of model IDs to comma-separated string
                String modelIds = selectedModels.stream()
                        .map(id -> String.valueOf(id))
                        .reduce((a, b) -> a + "," + b)
                        .orElse("");
                dto.setApplicableToModels(modelIds);
                System.out.println("✅ Policy applies to specific models: " + modelIds);
            } else {
                // Apply to all models
                dto.setApplicableToModels(null);
                System.out.println("✅ Policy applies to ALL vehicle models");
            }

            // Set deprecated fields as null (backward compatibility)
            dto.setHangPercent(null);
            dto.setDailyPercent(null);

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

    //  READ - Get policy detail for editing
    @GetMapping("/detail/{id}")
    public String getPolicyDetail(@PathVariable int id, Model model) {
        DTODiscountPolicy policy = daoPolicy.getPolicyById(id);
        if (policy == null) {
            model.addAttribute("error", "❌ Policy not found!");
            return "redirect:/discount-policy";
        }

        // Load vehicle models for selection
        try {
            DAOVehicleModel daoVehicleModel = new DAOVehicleModel();
            List<DTOVehicleModel> vehicleModels = daoVehicleModel.getAllModels();
            model.addAttribute("vehicleModels", vehicleModels);
        } catch (Exception e) {
            System.out.println("⚠️ Could not load vehicle models: " + e.getMessage());
        }

        // Load dealer info for the policy being edited
        if (policy.getDealerID() != null && policy.getDealerID() > 0) {
            try {
                DTODealer policyDealer = daoDealer.getDealerById(policy.getDealerID());
                model.addAttribute("policyDealer", policyDealer);
            } catch (Exception e) {
                System.out.println("⚠️ Could not load dealer for policy " + id);
            }
        }

        List<DTODealer> dealers = daoDealer.getAllDealers();
        List<DTODiscountPolicy> allPolicies = daoPolicy.getAllPolicies();

        // Load dealer info for all policies and store in a Map
        java.util.Map<Integer, DTODealer> dealerMap = new java.util.HashMap<>();
        for (DTODiscountPolicy p : allPolicies) {
            if (p.getDealerID() != null && p.getDealerID() > 0) {
                try {
                    DTODealer dealer = daoDealer.getDealerById(p.getDealerID());
                    dealerMap.put(p.getPolicyID(), dealer);
                } catch (Exception e) {
                    System.out.println("⚠️ Could not load dealer for policy " + p.getPolicyID());
                }
            }
        }
        model.addAttribute("dealerMap", dealerMap);

        model.addAttribute("dealers", dealers);
        model.addAttribute("policy", policy);
        model.addAttribute("policies", allPolicies);
        return "evmPage/evmDiscountPolicyManagement";
    }

    //  UPDATE - Update existing policy (Customer Discount)
    @PostMapping("/update/{id}")
    public String updateDiscountPolicy(
            @PathVariable int id,
            @RequestParam("policyName") String policyName,
            @RequestParam("description") String description,
            @RequestParam("discountPercent") Double discountPercent,
            @RequestParam("applicableModels") String applicableModels,
            @RequestParam(value = "selectedModels", required = false) List<Integer> selectedModels,
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

            // Update main fields
            policy.setPolicyName(policyName);
            policy.setDescription(description);
            policy.setDiscountPercent(BigDecimal.valueOf(discountPercent));
            policy.setStartDate(startDate);
            policy.setEndDate(endDate);

            // Handle applicable vehicle models
            if ("SPECIFIC".equals(applicableModels) && selectedModels != null && !selectedModels.isEmpty()) {
                // Convert list of model IDs to comma-separated string
                String modelIds = selectedModels.stream()
                        .map(modelId -> String.valueOf(modelId))
                        .reduce((a, b) -> a + "," + b)
                        .orElse("");
                policy.setApplicableToModels(modelIds);
                System.out.println("✅ Policy applies to specific models: " + modelIds);
            } else {
                // Apply to all models
                policy.setApplicableToModels(null);
                System.out.println("✅ Policy applies to ALL vehicle models");
            }

            // Keep deprecated fields as null
            policy.setHangPercent(null);
            policy.setDailyPercent(null);

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

    //  DELETE - Delete policy
    @PostMapping("/delete/{id}")
    public String deleteDiscountPolicy(@PathVariable int id, RedirectAttributes redirectAttributes) {
        try {
            boolean success = daoPolicy.deleteDiscountPolicy(id);

            if (success) {
                redirectAttributes.addFlashAttribute("message", " Deleted Discount Policy successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", " Policy not found!");
            }

        } catch (RuntimeException e) {
            if (e.getMessage().contains("referenced")) {
                redirectAttributes.addFlashAttribute("error",
                    " Cannot delete! This policy is still referenced by purchase orders.");
            } else {
                redirectAttributes.addFlashAttribute("error", " Error: " + e.getMessage());
            }
            e.printStackTrace();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", " Error: " + e.getMessage());
            e.printStackTrace();
        }

        return "redirect:/discount-policy";
    }
}
