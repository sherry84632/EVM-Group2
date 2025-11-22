package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.configuration.BusinessConfig;
import com.dealermanagementsysstem.project.Model.DAODealerLevel;
import com.dealermanagementsysstem.project.Model.DAOBusinessSetting;
import com.dealermanagementsysstem.project.Model.DTODealerLevel;
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

    @Autowired
    private DAODealerLevel daoDealerLevel;

    @Autowired
    private DAOBusinessSetting daoBusinessSetting;

    /**
     * Show settings page
     */
    @GetMapping
    public String showSettings(Model model) {
        model.addAttribute("currentVatRate", businessConfig.getVat().getRate());
        model.addAttribute("dealerLevels", daoDealerLevel.getAllDealerLevels());
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
                    "Invalid VAT rate! Must be between 0 and 100.");
                return "redirect:/settings";
            }

            // Update VAT rate in runtime
            businessConfig.getVat().setRate(vatRate);

            // Persist to DB so it survives restarts
            try { daoBusinessSetting.upsertDecimalSetting("VAT_RATE", vatRate); } catch (Exception ignore) {}

            redirectAttributes.addFlashAttribute("successMessage",
                "VAT rate updated successfully to " + vatRate + "%");

            // Log the change
            System.out.println("VAT rate changed to: " + vatRate + "%");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Error updating VAT rate: " + e.getMessage());
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
            try { daoBusinessSetting.upsertDecimalSetting("VAT_RATE", 10.0); } catch (Exception ignore) {}
            redirectAttributes.addFlashAttribute("successMessage",
                "VAT rate reset to default (10%)");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Error resetting VAT rate: " + e.getMessage());
        }
        return "redirect:/settings";
    }

    /**
     * Update dealer level
     */
    @PostMapping("/dealer-level/update")
    public String updateDealerLevel(@RequestParam("levelID") int levelID,
                                    @RequestParam("levelName") String levelName,
                                    @RequestParam("vehiclesRequired") Integer vehiclesRequired,
                                    @RequestParam("sharePercent") Double sharePercent,
                                    RedirectAttributes redirectAttributes) {
        try {
            if (levelName == null || levelName.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Level name cannot be empty");
                return "redirect:/settings";
            }
            if (vehiclesRequired == null || vehiclesRequired < 0) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vehicles required must be >= 0");
                return "redirect:/settings";
            }
            if (sharePercent == null || sharePercent < 0 || sharePercent > 100) {
                redirectAttributes.addFlashAttribute("errorMessage", "Share percent must be between 0 and 100");
                return "redirect:/settings";
            }
            DTODealerLevel lvl = daoDealerLevel.getDealerLevelById(levelID);
            if (lvl == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Dealer level not found");
                return "redirect:/settings";
            }
            lvl.setLevelName(levelName.trim());
            lvl.setVehiclesRequired(vehiclesRequired);
            lvl.setSharePercent(java.math.BigDecimal.valueOf(sharePercent));
            boolean ok = daoDealerLevel.updateDealerLevel(lvl);
            if (ok) {
                redirectAttributes.addFlashAttribute("successMessage", "Updated level " + levelName + " successfully");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Failed to update level");
            }
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating level: " + e.getMessage());
        }
        return "redirect:/settings";
    }

    /**
     * Create new dealer level
     */
    @PostMapping("/dealer-level/create")
    public String createDealerLevel(@RequestParam("newLevelName") String levelName,
                                    @RequestParam("newVehiclesRequired") Integer vehiclesRequired,
                                    @RequestParam("newSharePercent") Double sharePercent,
                                    RedirectAttributes redirectAttributes) {
        try {
            if (levelName == null || levelName.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Level name cannot be empty");
                return "redirect:/settings";
            }
            if (vehiclesRequired == null || vehiclesRequired < 0) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vehicles required must be >= 0");
                return "redirect:/settings";
            }
            if (sharePercent == null || sharePercent < 0 || sharePercent > 100) {
                redirectAttributes.addFlashAttribute("errorMessage", "Share percent must be between 0 and 100");
                return "redirect:/settings";
            }
            DTODealerLevel lvl = new DTODealerLevel();
            lvl.setLevelName(levelName.trim());
            lvl.setVehiclesRequired(vehiclesRequired);
            lvl.setSharePercent(java.math.BigDecimal.valueOf(sharePercent));
            int id = daoDealerLevel.createDealerLevel(lvl);
            if (id > 0) {
                redirectAttributes.addFlashAttribute("successMessage", "Created level '" + levelName + "' (ID=" + id + ")");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Failed to create level");
            }
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating level: " + e.getMessage());
        }
        return "redirect:/settings";
    }
}
