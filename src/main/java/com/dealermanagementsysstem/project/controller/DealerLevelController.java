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
            return "dealerPage/errorPage"; // assume exists
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

        // Tier thresholds (based on business rules)
        int nextThreshold;
        String currentTierName;
        String nextTierName;
        if (soldQty >= 100) { // Platinum
            currentTierName = "Platinum Dealer";
            nextThreshold = -1; // no next
            nextTierName = null;
        } else if (soldQty >= 50) {
            currentTierName = "Gold Dealer";
            nextThreshold = 100;
            nextTierName = "Platinum Dealer";
        } else if (soldQty >= 20) {
            currentTierName = "Silver Dealer";
            nextThreshold = 50;
            nextTierName = "Gold Dealer";
        } else { // <20
            currentTierName = "Bronze Dealer";
            nextThreshold = 20;
            nextTierName = "Silver Dealer";
        }
        int remaining = nextThreshold > 0 ? Math.max(0, nextThreshold - soldQty) : 0;
        double progressPercent = nextThreshold > 0 ? (soldQty * 100.0 / nextThreshold) : 100.0;
        if (progressPercent > 100.0) progressPercent = 100.0;

        // Current DB level name (in case different naming) attempt match
        String dbLevelName = currentTierName;
        java.util.List<DTODealerLevel> levels = daoDealer.getAllDealerLevels();
        for (DTODealerLevel lvl : levels) {
            if (lvl.getLevelID() == dealer.getLevelID() && lvl.getLevelName() != null) {
                dbLevelName = lvl.getLevelName();
                break;
            }
        }

        // ===== NORMALIZE & RANK HELPERS =====
        java.util.function.Function<String,String> normalize = name -> name == null ? "" : name.toLowerCase().replace(" dealer"," ").trim();
        java.util.function.Function<String,Integer> rankOf = name -> {
            String n = normalize.apply(name);
            if (n.startsWith("platinum")) return 4;
            if (n.startsWith("gold")) return 3;
            if (n.startsWith("silver")) return 2;
            if (n.startsWith("bronze")) return 1;
            return 0;
        };
        int earnedRank = rankOf.apply(currentTierName);      // rank from soldQty thresholds
        int dbRank     = rankOf.apply(dbLevelName);          // current DB stored rank

        // ===== DETERMINE TARGET LEVEL ID BY NORMALIZED NAME =====
        Integer targetLevelID = null;
        for (DTODealerLevel lvl : levels) {
            if (rankOf.apply(lvl.getLevelName()) == earnedRank) {
                targetLevelID = lvl.getLevelID();
                break;
            }
        }

        boolean upgraded = false;
        // Only upgrade if earned rank strictly higher than DB rank
        if (targetLevelID != null && earnedRank > dbRank) {
            if (daoDealer.updateDealerLevel(dealerId, targetLevelID)) {
                upgraded = true;
                DTODealer refreshed = null;
                try { refreshed = daoDealer.getDealerById(dealerId); } catch (Exception ignored) {}
                if (refreshed != null) {
                    dealer = refreshed;
                    dbLevelName = currentTierName; // reflect upgrade using tier display name
                    dbRank = earnedRank;
                }
            }
        }
        model.addAttribute("upgraded", upgraded);
        model.addAttribute("earnedRank", earnedRank);
        model.addAttribute("dbRank", dbRank);
        // ===== END AUTO UPGRADE LOGIC (rank-based) =====

        model.addAttribute("soldQty", soldQty);
        model.addAttribute("currentTierName", currentTierName);
        model.addAttribute("dbLevelName", dbLevelName);
        model.addAttribute("nextTierName", nextTierName);
        model.addAttribute("nextThreshold", nextThreshold > 0 ? nextThreshold : null);
        model.addAttribute("remainingToNext", remaining);
        model.addAttribute("progressPercent", progressPercent);
        model.addAttribute("levels", levels);
        return "dealerPage/dealerLevelDashboard";
    }
}
