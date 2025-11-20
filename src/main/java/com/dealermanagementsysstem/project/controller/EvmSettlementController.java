package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DAOSaleOrder;
import com.dealermanagementsysstem.project.Model.DTOSaleOrder;
import com.dealermanagementsysstem.project.Model.DTOSaleOrderDetail;
import com.dealermanagementsysstem.project.Model.SaleOrderStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/evm/settlement")
public class EvmSettlementController {

    private final DAOSaleOrder saleOrderDAO = new DAOSaleOrder();
    private final com.dealermanagementsysstem.project.Model.DAODealer dealerDAO = new com.dealermanagementsysstem.project.Model.DAODealer();

    @GetMapping
    public String settlementPage(@RequestParam(value = "status", required = false) String statusFilter,
                                 @RequestParam(value = "dealerID", required = false) Integer dealerFilter,
                                 Model model) {
        List<DTOSaleOrder> all = saleOrderDAO.getAllSaleOrders();

        // Filter sale orders that use manufacturer discount/policy
        List<DTOSaleOrder> withManufacturer = all.stream().filter(o -> {
            if (o.getDetail() == null) return false;
            return o.getDetail().stream().anyMatch(d ->
                    (d.getPromoDiscountPercent() != null && d.getPromoDiscountPercent() > 0) ||
                    (d.getPromoDiscountAmount() != null && d.getPromoDiscountAmount().compareTo(BigDecimal.ZERO) > 0) ||
                    d.getDiscountPolicy() != null || d.getPromoPolicyID() != null);
        }).collect(Collectors.toList());

        if (statusFilter != null && !statusFilter.isBlank()) {
            withManufacturer = withManufacturer.stream()
                    .filter(o -> o.getStatus() != null && o.getStatus().name().equalsIgnoreCase(statusFilter))
                    .collect(Collectors.toList());
        }

        if (dealerFilter != null && dealerFilter > 0) {
            withManufacturer = withManufacturer.stream()
                    .filter(o -> o.getDealer() != null && o.getDealer().getDealerID() == dealerFilter)
                    .collect(Collectors.toList());
        }

        // Global aggregates
        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;
        BigDecimal totalManufacturerDiscount = BigDecimal.ZERO;
        BigDecimal totalDealerDiscount = BigDecimal.ZERO; // new
        BigDecimal totalBaseQuotationDiscount = BigDecimal.ZERO; // new
        BigDecimal totalDealerLevelShare = BigDecimal.ZERO; // new aggregate

        // Policy aggregate container
        class PolicyAgg { BigDecimal discount = BigDecimal.ZERO; int orders = 0; int vehicles = 0; String name; }
        Map<Integer, PolicyAgg> policyMap = new LinkedHashMap<>();

        // Per-order rows for display
        List<Map<String,Object>> orderRows = new ArrayList<>();

        for (DTOSaleOrder order : withManufacturer) {
            BigDecimal orderGross = BigDecimal.ZERO;
            BigDecimal orderManufacturerDiscount = BigDecimal.ZERO;
            BigDecimal orderDealerDiscount = BigDecimal.ZERO; // per order
            BigDecimal orderBaseDiscount = BigDecimal.ZERO;   // per order
            BigDecimal orderDealerLevelShare = BigDecimal.ZERO;
            Double levelSharePct = null;
            Set<String> policyNames = new LinkedHashSet<>();
            Set<Integer> policiesInOrder = new HashSet<>();

            if (order.getDetail() != null) {
                for (DTOSaleOrderDetail d : order.getDetail()) {
                    int qty = d.getQuantity() != null ? d.getQuantity() : 1;
                    BigDecimal grossUnit = d.getGrossUnitPrice() != null ? d.getGrossUnitPrice() : (d.getPrice() != null ? d.getPrice() : BigDecimal.ZERO);
                    orderGross = orderGross.add(grossUnit.multiply(BigDecimal.valueOf(qty)));
                    BigDecimal dealerLine = d.getDealerDiscountAmountPerUnit().multiply(BigDecimal.valueOf(qty));
                    orderDealerDiscount = orderDealerDiscount.add(dealerLine);
                    BigDecimal manufLine = d.getPromoDiscountAmountPerUnit().multiply(BigDecimal.valueOf(qty));
                    orderManufacturerDiscount = orderManufacturerDiscount.add(manufLine);
                    BigDecimal baseLine = d.getBaseQuotationDiscountAmountPerUnit() != null ? d.getBaseQuotationDiscountAmountPerUnit().multiply(BigDecimal.valueOf(qty)) : BigDecimal.ZERO;
                    orderBaseDiscount = orderBaseDiscount.add(baseLine);

                    Integer pid = d.getDiscountPolicy() != null ? d.getDiscountPolicy().getPolicyID() : d.getPromoPolicyID();
                    if (pid != null) {
                        policiesInOrder.add(pid);
                        PolicyAgg agg = policyMap.get(pid);
                        if (agg == null) { agg = new PolicyAgg(); policyMap.put(pid, agg); }
                        agg.discount = agg.discount.add(manufLine);
                        agg.vehicles += qty;
                        if (agg.name == null && d.getDiscountPolicy() != null) {
                            agg.name = d.getDiscountPolicy().getPolicyName();
                        }
                        if (d.getDiscountPolicy() != null) {
                            policyNames.add(d.getDiscountPolicy().getPolicyName());
                        }
                    }
                }
            }
            // increment order counts for policies present in this order
            for (Integer pid : policiesInOrder) {
                PolicyAgg agg = policyMap.get(pid);
                if (agg != null) agg.orders += 1;
            }

            double percent = orderGross.compareTo(BigDecimal.ZERO) > 0 ?
                    orderManufacturerDiscount.divide(orderGross, 6, java.math.RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).doubleValue() : 0.0;

            if (order.getDealer() != null && order.getDealer().getLevelID() > 0) {
                var lvlObj = dealerDAO.getDealerLevelById(order.getDealer().getLevelID());
                if (lvlObj != null && lvlObj.getDiscountSharePercent() != null) {
                    levelSharePct = lvlObj.getDiscountSharePercent();
                }
            }
            if (levelSharePct != null && levelSharePct > 0 && orderManufacturerDiscount.compareTo(BigDecimal.ZERO) > 0) {
                // share calculated on manufacturer discount portion
                orderDealerLevelShare = orderManufacturerDiscount.multiply(BigDecimal.valueOf(levelSharePct / 100.0));
            }

            Map<String,Object> row = new LinkedHashMap<>();
            row.put("order", order);
            row.put("gross", orderGross);
            row.put("net", order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO);
            row.put("manufDiscount", orderManufacturerDiscount);
            row.put("manufPercent", percent);
            row.put("policyNames", policyNames);
            row.put("dealerDiscount", orderDealerDiscount); // per-order dealer discount
            row.put("baseDiscount", orderBaseDiscount);     // per-order base discount
            row.put("dealerLevelShare", orderDealerLevelShare);
            row.put("dealerLevelSharePercent", levelSharePct != null ? levelSharePct : 0.0);
            // New: total saved (dealer + manufacturer + base)
            row.put("savedTotal", orderDealerDiscount.add(orderManufacturerDiscount).add(orderBaseDiscount));
            orderRows.add(row);

            totalGross = totalGross.add(orderGross);
            totalNet = totalNet.add(order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO);
            totalManufacturerDiscount = totalManufacturerDiscount.add(orderManufacturerDiscount);
            totalDealerDiscount = totalDealerDiscount.add(orderDealerDiscount);
            totalBaseQuotationDiscount = totalBaseQuotationDiscount.add(orderBaseDiscount);
            totalDealerLevelShare = totalDealerLevelShare.add(orderDealerLevelShare);
        }

        double effectivePercent = totalGross.compareTo(BigDecimal.ZERO) > 0 ?
                totalManufacturerDiscount.divide(totalGross, 6, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue() : 0.0;

        // build policy breakdown list
        List<Map<String,Object>> policyBreakdown = new ArrayList<>();
        for (Map.Entry<Integer, PolicyAgg> e : policyMap.entrySet()) {
            Map<String,Object> row = new LinkedHashMap<>();
            row.put("policyID", e.getKey());
            row.put("policyName", e.getValue().name != null ? e.getValue().name : ("Policy #" + e.getKey()));
            row.put("totalDiscount", e.getValue().discount);
            row.put("orders", e.getValue().orders);
            row.put("vehicles", e.getValue().vehicles);
            policyBreakdown.add(row);
        }
        policyBreakdown.sort((a,b)-> ((BigDecimal)b.get("totalDiscount")).compareTo((BigDecimal)a.get("totalDiscount")) );

        model.addAttribute("ordersData", orderRows);
        model.addAttribute("totalGross", totalGross);
        model.addAttribute("totalNet", totalNet);
        model.addAttribute("totalManufacturerDiscount", totalManufacturerDiscount);
        model.addAttribute("effectivePercent", effectivePercent);
        model.addAttribute("policyBreakdown", policyBreakdown);
        model.addAttribute("statusFilter", statusFilter);
        model.addAttribute("dealerFilter", dealerFilter);
        model.addAttribute("statuses", SaleOrderStatus.values());
        model.addAttribute("dealers", dealerDAO.getAllDealers());
        model.addAttribute("totalDealerDiscount", totalDealerDiscount);
        model.addAttribute("totalBaseQuotationDiscount", totalBaseQuotationDiscount);
        model.addAttribute("totalSaved", totalDealerDiscount.add(totalManufacturerDiscount).add(totalBaseQuotationDiscount));
        model.addAttribute("totalDealerLevelShare", totalDealerLevelShare);

        double aggregatedDealerLevelSharePercent = (totalManufacturerDiscount.compareTo(BigDecimal.ZERO) > 0)
                ? totalDealerLevelShare.divide(totalManufacturerDiscount, 6, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;
        model.addAttribute("aggregatedDealerLevelSharePercent", aggregatedDealerLevelSharePercent);

        return "evmPage/evmManufacturerSettlement";
    }
}
