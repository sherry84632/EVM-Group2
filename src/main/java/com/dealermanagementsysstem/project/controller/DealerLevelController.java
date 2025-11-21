package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dealer/level")
public class DealerLevelController {

    private final DAOAccount daoAccount = new DAOAccount();
    private final DAODealer daoDealer = new DAODealer();
    private final DAOSaleOrder daoSaleOrder = new DAOSaleOrder();

    @GetMapping
    public String showLevelDashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            model.addAttribute("error", "Bạn chưa đăng nhập.");
            return "dealerPage/errorPage";
        }
        DTOAccount account = daoAccount.findAccountByEmail(auth.getName());
        if (account == null || account.getDealerStaff() == null || account.getDealerStaff().getDealer() == null) {
            model.addAttribute("error", "Tài khoản không thuộc dealer.");
            return "dealerPage/errorPage";
        }
        int dealerId = account.getDealerStaff().getDealer().getDealerID();
        DTODealer dealer = null;
        try { dealer = daoDealer.getDealerById(dealerId); } catch (Exception ignored) {}
        if (dealer == null) {
            model.addAttribute("error", "Không tìm thấy dealer.");
            return "dealerPage/errorPage";
        }
        int soldQty = daoSaleOrder.getTotalCompletedQuantityByDealer(dealerId);

        // ===== Load dynamic dealer levels =====
        java.util.List<DTODealerLevel> levels = daoDealer.getAllDealerLevels();
        // Sort ascending by vehiclesRequired (fallback 0 if missing)
        levels.sort((a,b) -> Integer.compare(a.getVehiclesRequired(), b.getVehiclesRequired()));
        // Filter out any negative values just in case
        java.util.List<DTODealerLevel> validLevels = new java.util.ArrayList<>();
        for (DTODealerLevel lvl : levels) {
            if (lvl.getVehiclesRequired() >= 0) validLevels.add(lvl);
        }

        DTODealerLevel achieved = null;
        DTODealerLevel next = null;
        for (DTODealerLevel lvl : validLevels) {
            if (soldQty >= lvl.getVehiclesRequired()) {
                achieved = lvl; // highest achieved as we go ascending
            } else {
                // first level whose requirement we haven't met yet is next
                if (next == null) next = lvl;
            }
        }
        // If nothing achieved but there are levels, current is first level (not yet reached)
        String currentTierName;
        if (achieved != null) {
            currentTierName = achieved.getLevelName() != null ? achieved.getLevelName() + " Dealer" : "Dealer Level";
        } else if (!validLevels.isEmpty()) {
            currentTierName = "Chưa đạt cấp đầu tiên";
        } else {
            currentTierName = "Không có cấu hình cấp";
        }

        Integer nextThreshold = next != null ? next.getVehiclesRequired() : null;
        String nextTierName = next != null && next.getLevelName()!=null ? next.getLevelName() + " Dealer" : null;
        int remaining = nextThreshold != null ? Math.max(0, nextThreshold - soldQty) : 0;
        double progressPercent = nextThreshold != null && nextThreshold > 0 ? (soldQty * 100.0 / nextThreshold) : 100.0;
        if (progressPercent > 100.0) progressPercent = 100.0;

        // Auto upgrade dealer's stored LevelID if achieved level differs
        boolean upgraded = false;
        if (achieved != null && dealer.getLevelID() != achieved.getLevelID()) {
            if (daoDealer.updateDealerLevel(dealerId, achieved.getLevelID())) {
                upgraded = true;
                try { dealer = daoDealer.getDealerById(dealerId); } catch (Exception ignored) {}
            }
        }

        // Use DB stored level name after potential upgrade
        String dbLevelName = null;
        for (DTODealerLevel lvl : validLevels) {
            if (lvl.getLevelID() == dealer.getLevelID()) {
                dbLevelName = lvl.getLevelName();
                break;
            }
        }
        if (dbLevelName == null) dbLevelName = achieved != null ? achieved.getLevelName() : currentTierName;

        // Share percent for current tier (prefer explicit sharePercent)
        Double sharePercent = achieved != null ? achieved.getSharePercent()!=null? achieved.getSharePercent().doubleValue() : achieved.getDiscountSharePercent() : null;

        model.addAttribute("upgraded", upgraded);
        model.addAttribute("soldQty", soldQty);
        model.addAttribute("currentTierName", currentTierName);
        model.addAttribute("dbLevelName", dbLevelName);
        model.addAttribute("nextTierName", nextTierName);
        model.addAttribute("nextThreshold", nextThreshold);
        model.addAttribute("remainingToNext", remaining);
        model.addAttribute("progressPercent", progressPercent);
        model.addAttribute("levels", validLevels);
        model.addAttribute("currentSharePercent", sharePercent);
        model.addAttribute("achievedVehiclesRequired", achieved != null ? achieved.getVehiclesRequired() : null);
        return "dealerPage/dealerLevelDashboard";
    }
}
