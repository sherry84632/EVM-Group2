package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.configuration.BusinessConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Settings Controller
 * Manages system configuration settings like VAT rate
 */
@Controller
@RequestMapping("/settings")
public class SettingsController {

    @Autowired
    private BusinessConfig businessConfig;

    /**
     * Show settings page
     */
    @GetMapping
    public String showSettings(Model model) {
        model.addAttribute("currentVatRate", businessConfig.getVat().getRate());
        return "evmPage/settings";
    }

    /**
     * Update VAT rate
     */
    @PostMapping("/vat/update")
    public String updateVatRate(
            @RequestParam("vatRate") Double vatRate,
            RedirectAttributes redirectAttributes) {

        try {
            // Validate VAT rate
            if (vatRate == null || vatRate < 0 || vatRate > 100) {
                redirectAttributes.addFlashAttribute("errorMessage",
                    "❌ Invalid VAT rate! Must be between 0 and 100.");
                return "redirect:/settings";
            }

            // Update VAT rate in runtime
            businessConfig.getVat().setRate(vatRate);

            redirectAttributes.addFlashAttribute("successMessage",
                "✅ VAT rate updated successfully to " + vatRate + "%");

            // Log the change
            System.out.println("✅ VAT rate changed to: " + vatRate + "%");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "❌ Error updating VAT rate: " + e.getMessage());
            e.printStackTrace();
        }

        return "redirect:/settings";
    }

    /**
     * Reset VAT to default (10%)
     */
    @PostMapping("/vat/reset")
    public String resetVatRate(RedirectAttributes redirectAttributes) {
        try {
            businessConfig.getVat().setRate(10.0);
            redirectAttributes.addFlashAttribute("successMessage",
                "✅ VAT rate reset to default (10%)");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "❌ Error resetting VAT rate: " + e.getMessage());
        }
        return "redirect:/settings";
    }
}

