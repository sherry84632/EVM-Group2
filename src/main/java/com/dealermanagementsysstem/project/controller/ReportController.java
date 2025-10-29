package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DAOReport;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/evm/report")
public class ReportController {

    private final DAOReport dao = new DAOReport();

    @GetMapping
    public String showReport(Model model,
                             @RequestParam(value = "dealerId", required = false) Integer dealerId,
                             @RequestParam(value = "month", required = false) String month,
                             @RequestParam(value = "from", required = false) String from,
                             @RequestParam(value = "to", required = false) String to) {

        LocalDate fromDate = null; LocalDate toDate = null;
        try {
            if (month != null && !month.isBlank()) {
                YearMonth ym = YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM"));
                fromDate = ym.atDay(1);
                toDate = ym.atEndOfMonth();
            } else {
                if (from != null && !from.isBlank()) fromDate = LocalDate.parse(from);
                if (to != null && !to.isBlank()) toDate = LocalDate.parse(to);
            }
        } catch (Exception ignored) { }

        java.sql.Date fromSql = fromDate != null ? java.sql.Date.valueOf(fromDate) : null;
        java.sql.Date toSql = toDate != null ? java.sql.Date.valueOf(toDate) : null;

        model.addAttribute("dealers", dao.getDealers());
        model.addAttribute("selectedDealerId", dealerId);
        model.addAttribute("selectedMonth", month);
        model.addAttribute("selectedFrom", fromDate);
        model.addAttribute("selectedTo", toDate);

        model.addAttribute("dealerCount", dao.getDealerCount());
        model.addAttribute("inventoryTotals", dao.getInventoryTotals());
        model.addAttribute("purchaseOrderStats", dao.getPurchaseOrderStats());
        model.addAttribute("saleOrderStats", dao.getSaleOrderStats());

        model.addAttribute("kpis", dao.getKpis(fromSql, toSql, dealerId));
        model.addAttribute("dealerAggregates", dao.getDealerAggregates(fromSql, toSql, dealerId));
        model.addAttribute("topDealersRevenue", dao.getTopDealersByRevenue(fromSql, toSql, dealerId));
        model.addAttribute("revenueShareByModel", dao.getRevenueShareByModel(fromSql, toSql, dealerId));
        return "evmPage/evmReport";
    }
}


