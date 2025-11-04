package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DAOAccount;
import com.dealermanagementsysstem.project.Model.DAODealerReport;
import com.dealermanagementsysstem.project.util.SecurityUtil;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/dealer/reports")
public class DealerReportController {

    private final DAODealerReport dao = new DAODealerReport();
    private final DAOAccount daoAccount = new DAOAccount();

    private Integer resolveDealerIdOrNull() {
        String email = SecurityUtil.getCurrentUserEmail();
        if (email == null) return null;
        return daoAccount.getDealerIdByEmail(email);
    }

    private java.sql.Date toSql(LocalDate d) { return d != null ? java.sql.Date.valueOf(d) : null; }

    @GetMapping
    public String landing(Model model) { return "dealerPage/reports/index"; }

    // ========== View pages ==========
    @GetMapping("/sales")
    public String salesPage(Model model) { return "dealerPage/reports/sales"; }

    @GetMapping("/inventory")
    public String inventoryPage(Model model) { return "dealerPage/reports/inventory"; }

    @GetMapping("/purchase")
    public String purchasePage(Model model) { return "dealerPage/reports/purchase"; }

    @GetMapping("/discounts")
    public String discountsPage(Model model) { return "dealerPage/reports/discounts"; }

    // ========== Sales Report ==========
    @GetMapping("/sales/kpis")
    @ResponseBody
    public Map<String, Object> salesKpis(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Integer dealerId = resolveDealerIdOrNull();
        return dao.getSalesKpis(dealerId, toSql(from), toSql(to));
    }

    @GetMapping("/sales/by-month")
    @ResponseBody
    public List<Map<String, Object>> salesByMonth(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Integer dealerId = resolveDealerIdOrNull();
        return dao.getSalesByMonth(dealerId, toSql(from), toSql(to));
    }

    @GetMapping("/sales/status-distribution")
    @ResponseBody
    public Map<String, Integer> saleOrderStatus(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Integer dealerId = resolveDealerIdOrNull();
        return dao.getOrderStatusDistribution(dealerId, toSql(from), toSql(to));
    }

    @GetMapping("/sales/top-colors")
    @ResponseBody
    public List<Map<String, Object>> topColors(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "limit", required = false, defaultValue = "5") int limit) {
        Integer dealerId = resolveDealerIdOrNull();
        return dao.getTopVehicleColorsSold(dealerId, toSql(from), toSql(to), limit);
    }

    @GetMapping("/sales/table")
    @ResponseBody
    public List<Map<String, Object>> salesTable(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "status", required = false) String status) {
        Integer dealerId = resolveDealerIdOrNull();
        return dao.getSalesTable(dealerId, toSql(from), toSql(to), status);
    }

    @GetMapping("/sales/export.csv")
    public ResponseEntity<byte[]> exportSalesCsv(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "status", required = false) String status) {
        List<Map<String, Object>> rows = salesTable(from, to, status);
        StringBuilder sb = new StringBuilder();
        sb.append("SaleOrderID,CreatedAt,Status,Quantity,TotalAmount,Customer\n");
        for (Map<String, Object> r : rows) {
            sb.append(r.get("saleOrderId")).append(',')
              .append(r.get("createdAt")).append(',')
              .append(r.get("status")).append(',')
              .append(r.get("quantity")).append(',')
              .append(r.get("totalAmount")).append(',')
              .append(r.get("customerName")).append('\n');
        }
        byte[] data = sb.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sales_report.csv")
                .contentType(MediaType.TEXT_PLAIN)
                .contentLength(data.length)
                .body(data);
    }

    // ========== Inventory Report ==========
    @GetMapping("/inventory/kpis")
    @ResponseBody
    public Map<String, Integer> inventoryKpis(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Integer dealerId = resolveDealerIdOrNull();
        return dao.getInventoryKpis(dealerId, toSql(from), toSql(to));
    }

    @GetMapping("/inventory/by-color")
    @ResponseBody
    public List<Map<String, Object>> inventoryByColor() {
        Integer dealerId = resolveDealerIdOrNull();
        return dao.getInventoryByColor(dealerId);
    }

    @GetMapping("/inventory/by-version")
    @ResponseBody
    public List<Map<String, Object>> inventoryByVersion() {
        Integer dealerId = resolveDealerIdOrNull();
        return dao.getInventoryByVersion(dealerId);
    }

    @GetMapping("/inventory/export.csv")
    public ResponseEntity<byte[]> exportInventoryCsv() {
        List<Map<String, Object>> byColor = inventoryByColor();
        List<Map<String, Object>> byVersion = inventoryByVersion();
        StringBuilder sb = new StringBuilder();
        sb.append("Section,Name,Count\n");
        for (Map<String, Object> r : byColor) {
            sb.append("Color,")
              .append(Optional.ofNullable(r.get("color")).orElse(""))
              .append(',')
              .append(r.get("count"))
              .append('\n');
        }
        for (Map<String, Object> r : byVersion) {
            sb.append("Version,")
              .append(Optional.ofNullable(r.get("version")).orElse(""))
              .append(',')
              .append(r.get("count"))
              .append('\n');
        }
        byte[] data = sb.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=inventory_report.csv")
                .contentType(MediaType.TEXT_PLAIN)
                .contentLength(data.length)
                .body(data);
    }

    // ========== Purchase Orders Report ==========
    @GetMapping("/purchase/kpis")
    @ResponseBody
    public Map<String, Object> purchaseKpis(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Integer dealerId = resolveDealerIdOrNull();
        return dao.getPurchaseOrderKpis(dealerId, toSql(from), toSql(to));
    }

    @GetMapping("/purchase/status-distribution")
    @ResponseBody
    public Map<String, Integer> purchaseStatus(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Integer dealerId = resolveDealerIdOrNull();
        return dao.getPurchaseOrderStatusDistribution(dealerId, toSql(from), toSql(to));
    }

    // ========== Discount Policy Report ==========
    @GetMapping("/discounts/summary")
    @ResponseBody
    public Map<String, Object> discountSummary(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Integer dealerId = resolveDealerIdOrNull();
        return dao.getDiscountEffectiveness(dealerId, toSql(from), toSql(to));
    }
}


