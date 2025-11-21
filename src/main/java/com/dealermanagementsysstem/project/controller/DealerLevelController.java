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
        levels.sort((a,b) -> Integer.compare(a.getVehiclesRequired(), b.getVehiclesRequired()));
        java.util.List<DTODealerLevel> validLevels = new java.util.ArrayList<>();
        for (DTODealerLevel lvl : levels) { if (lvl.getVehiclesRequired() >= 0) validLevels.add(lvl); }

        // Find record matching dealer's stored level (respect manual assignment)
        DTODealerLevel currentLevelRecord = null;
        for (DTODealerLevel lvl : validLevels) {
            if (lvl.getLevelID() == dealer.getLevelID()) { currentLevelRecord = lvl; break; }
        }
        if (currentLevelRecord == null && !validLevels.isEmpty()) {
            // Fallback: first level if none matches
            currentLevelRecord = validLevels.get(0);
        }

        // Achieved starts at current stored level; only upgrade upwards if soldQty meets higher requirements
        DTODealerLevel achieved = currentLevelRecord;
        for (DTODealerLevel lvl : validLevels) {
            if (lvl.getVehiclesRequired() >= achieved.getVehiclesRequired() && soldQty >= lvl.getVehiclesRequired()) {
                // qualifies for this higher (or equal) tier
                if (lvl.getVehiclesRequired() > achieved.getVehiclesRequired()) {
                    achieved = lvl; // upgrade candidate
                }
            }
        }

        // Next tier: first level with requirement greater than achieved
        DTODealerLevel next = null;
        for (DTODealerLevel lvl : validLevels) {
            if (lvl.getVehiclesRequired() > achieved.getVehiclesRequired()) { next = lvl; break; }
        }

        String currentTierName = achieved != null && achieved.getLevelName()!=null ? achieved.getLevelName() + " Dealer" : "Dealer Level";
        Integer nextThreshold = next != null ? next.getVehiclesRequired() : null;
        String nextTierName = next != null && next.getLevelName()!=null ? next.getLevelName() + " Dealer" : null;
        int remaining = nextThreshold != null ? Math.max(0, nextThreshold - soldQty) : 0;

        // Progress between achieved tier and next tier (segment progress)
        double progressPercent;
        if (next != null) {
            int baseReq = achieved.getVehiclesRequired();
            int range = next.getVehiclesRequired() - baseReq;
            int progressSegment = Math.max(0, soldQty - baseReq);
            progressPercent = range > 0 ? (progressSegment * 100.0 / range) : 100.0;
            if (progressPercent > 100.0) progressPercent = 100.0;
        } else {
            progressPercent = 100.0; // top tier
        }

        // Upgrade dealer level in DB only if achieved is higher than stored (prevent downgrade)
        boolean upgraded = false;
        if (achieved != null && currentLevelRecord != null && achieved.getLevelID() != dealer.getLevelID() && achieved.getVehiclesRequired() > currentLevelRecord.getVehiclesRequired()) {
            if (daoDealer.updateDealerLevel(dealerId, achieved.getLevelID())) {
                upgraded = true;
                try { dealer = daoDealer.getDealerById(dealerId); } catch (Exception ignored) {}
            }
        }

        // dbLevelName based on (possibly updated) dealer level
        String dbLevelName = achieved != null ? achieved.getLevelName() : currentTierName;
        if (dealer.getLevelID() != achieved.getLevelID()) { // if not upgraded yet keep stored name
            for (DTODealerLevel lvl : validLevels) {
                if (lvl.getLevelID() == dealer.getLevelID()) { dbLevelName = lvl.getLevelName(); break; }
            }
        }

        Double sharePercent = achieved != null ? (achieved.getSharePercent()!=null? achieved.getSharePercent().doubleValue() : achieved.getDiscountSharePercent()) : null;

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
