package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DAOManufacturerDiscountSettlement;
import com.dealermanagementsysstem.project.Model.DAOSaleOrder;
import com.dealermanagementsysstem.project.Model.DTOSaleOrder;
import com.dealermanagementsysstem.project.Model.DTOSaleOrderDetail;
import com.dealermanagementsysstem.project.Model.SaleOrderStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/evm/settlement")
public class EvmSettlementController {

    private final DAOSaleOrder saleOrderDAO = new DAOSaleOrder();
    private final com.dealermanagementsysstem.project.Model.DAODealer dealerDAO = new com.dealermanagementsysstem.project.Model.DAODealer();
    private final com.dealermanagementsysstem.project.Model.DAOManufacturerDiscountSettlement settlementDAO = new com.dealermanagementsysstem.project.Model.DAOManufacturerDiscountSettlement();

    @GetMapping
    public String settlementPage(@RequestParam(value = "status", required = false) String statusFilter,
                                 @RequestParam(value = "dealerID", required = false) Integer dealerFilter,
                                 @RequestParam(value = "policyID", required = false) Integer policyFilter,
                                 @RequestParam(value = "settlementStatus", required = false) String settlementStatusFilter,
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

        if (policyFilter != null && policyFilter > 0) {
            withManufacturer = withManufacturer.stream().filter(o -> o.getDetail()!=null && o.getDetail().stream().anyMatch(d -> {
                Integer pid = d.getDiscountPolicy()!=null? d.getDiscountPolicy().getPolicyID() : d.getPromoPolicyID();
                return pid != null && pid.equals(policyFilter);
            })).collect(java.util.stream.Collectors.toList());
        }

        // Global aggregates
        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;
        BigDecimal totalManufacturerDiscount = BigDecimal.ZERO;
        BigDecimal totalDiscountSavings = BigDecimal.ZERO; // gross - net
        // NEW aggregates
        BigDecimal totalReimbursed = BigDecimal.ZERO; // tổng đã quyết toán
        BigDecimal totalOutstanding = BigDecimal.ZERO; // tổng còn lại cần quyết toán

        // Policy aggregate container
        class PolicyAgg { BigDecimal discount = BigDecimal.ZERO; int orders = 0; int vehicles = 0; String name; }
        Map<Integer, PolicyAgg> policyMap = new LinkedHashMap<>();

        // Per-order rows for display
        List<Map<String,Object>> orderRows = new ArrayList<>();
        // Reset aggregates; will only count rows passing all filters including settlementStatus
        totalGross = BigDecimal.ZERO; totalNet = BigDecimal.ZERO; totalManufacturerDiscount = BigDecimal.ZERO; totalDiscountSavings = BigDecimal.ZERO;
        for (DTOSaleOrder order : withManufacturer) {
            BigDecimal orderManufacturerDiscount = BigDecimal.ZERO;
            BigDecimal orderGross = BigDecimal.ZERO; // ensure defined top
            Set<String> policyNames = new LinkedHashSet<>();
            Set<Integer> policiesInOrder = new HashSet<>();

            if (order.getDetail() != null) {
                for (DTOSaleOrderDetail d : order.getDetail()) {
                    int qty = d.getQuantity() != null ? d.getQuantity() : 1;
                    BigDecimal grossUnit = d.getGrossUnitPrice() != null ? d.getGrossUnitPrice() : (d.getPrice() != null ? d.getPrice() : BigDecimal.ZERO);
                    orderGross = orderGross.add(grossUnit.multiply(BigDecimal.valueOf(qty)));
                    BigDecimal perUnitManuf = d.getPromoDiscountAmountPerUnit();
                    if (perUnitManuf == null) perUnitManuf = BigDecimal.ZERO;
                    BigDecimal manufLine = perUnitManuf.multiply(BigDecimal.valueOf(qty));
                    orderManufacturerDiscount = orderManufacturerDiscount.add(manufLine);

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

            BigDecimal orderNet = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal orderSavings = orderGross.subtract(orderNet);

            Map<String,Object> row = new LinkedHashMap<>();
            row.put("order", order);
            row.put("gross", orderGross);
            row.put("net", orderNet);
            row.put("manufDiscount", orderManufacturerDiscount);
            row.put("manufPercent", percent);
            row.put("policyNames", policyNames);
            row.put("savings", orderSavings);
            double savingsPct = orderGross.compareTo(BigDecimal.ZERO)>0 ? orderSavings.divide(orderGross,6,java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue() : 0.0;
            row.put("savingsPercent", savingsPct);

            com.dealermanagementsysstem.project.Model.DTOManufacturerDiscountSettlement settlement = null;
            if (orderManufacturerDiscount.compareTo(BigDecimal.ZERO) > 0 && order.getDealer()!=null) {
                settlement = settlementDAO.getBySaleOrderId(order.getSaleOrderID());
                if (settlement == null) {
                    settlement = settlementDAO.create(order.getSaleOrderID(), order.getDealer().getDealerID(), orderManufacturerDiscount);
                } else if (settlement.getTotalManufacturerDiscount()!=null && settlement.getTotalManufacturerDiscount().compareTo(orderManufacturerDiscount)!=0) {
                    try (java.sql.Connection c = utils.DBUtils.getConnection(); java.sql.PreparedStatement ps = c.prepareStatement("UPDATE ManufacturerDiscountSettlement SET TotalManufacturerDiscount=?, UpdatedAt=GETDATE() WHERE SettlementID=?")) {
                        ps.setBigDecimal(1, orderManufacturerDiscount);
                        ps.setInt(2, settlement.getSettlementID());
                        ps.executeUpdate();
                        settlement.setTotalManufacturerDiscount(orderManufacturerDiscount);
                    } catch (Exception ignore) {}
                }
            }
            // Settlement status filter logic
            if (settlementStatusFilter != null && !settlementStatusFilter.isBlank()) {
                if (settlement == null || settlement.getStatus()==null || !settlement.getStatus().equalsIgnoreCase(settlementStatusFilter)) {
                    continue; // skip
                }
            }
            row.put("settlement", settlement);
            orderRows.add(row);

            totalGross = totalGross.add(orderGross);
            totalNet = totalNet.add(orderNet);
            totalManufacturerDiscount = totalManufacturerDiscount.add(orderManufacturerDiscount);
            totalDiscountSavings = totalDiscountSavings.add(orderSavings);
            // NEW aggregate logic per settlement
            if (settlement != null) {
                BigDecimal reimb = settlement.getReimbursedAmount()!=null? settlement.getReimbursedAmount(): BigDecimal.ZERO;
                BigDecimal outstanding = settlement.getOutstanding()!=null? settlement.getOutstanding(): BigDecimal.ZERO;
                // Clamp negative outstanding (in case of over payment)
                if (outstanding.compareTo(BigDecimal.ZERO) < 0) outstanding = BigDecimal.ZERO;
                totalReimbursed = totalReimbursed.add(reimb);
                totalOutstanding = totalOutstanding.add(outstanding);
            }
        }

        double effectivePercent = totalGross.compareTo(BigDecimal.ZERO) > 0 ?
                totalManufacturerDiscount.divide(totalGross, 6, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue() : 0.0;

        double savingsPercentOverall = totalGross.compareTo(BigDecimal.ZERO)>0 ? totalDiscountSavings.divide(totalGross,6,java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue() : 0.0;
        // NEW percent paid (tỷ lệ đã quyết toán trên tổng cần hoàn)
        double percentPaid = totalManufacturerDiscount.compareTo(BigDecimal.ZERO)>0 ?
                totalReimbursed.divide(totalManufacturerDiscount,6,java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue() : 0.0;

        model.addAttribute("totalDiscountSavings", totalDiscountSavings);
        model.addAttribute("discountSavingsPercent", savingsPercentOverall);
        model.addAttribute("reimbursementDue", totalManufacturerDiscount);
        // NEW attributes for UI
        model.addAttribute("totalReimbursed", totalReimbursed);
        model.addAttribute("totalOutstanding", totalOutstanding);
        model.addAttribute("percentPaid", percentPaid);

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
        model.addAttribute("policyFilter", policyFilter);
        model.addAttribute("settlementStatusFilter", settlementStatusFilter);
        model.addAttribute("statuses", SaleOrderStatus.values());
        model.addAttribute("dealers", dealerDAO.getAllDealers());
        model.addAttribute("settlementsPresent", true);

        // Determine if current user can update sale order status (dealer roles only)
        boolean canUpdateOrderStatus = false;
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null) {
                com.dealermanagementsysstem.project.Model.DAOAccount daoAccount = new com.dealermanagementsysstem.project.Model.DAOAccount();
                com.dealermanagementsysstem.project.Model.DTOAccount acc = daoAccount.findAccountByEmail(auth.getName());
                if (acc != null && (acc.getRole() == com.dealermanagementsysstem.project.Model.Role.DEALER || acc.getRole() == com.dealermanagementsysstem.project.Model.Role.DEALERSTAFF)) {
                    canUpdateOrderStatus = true;
                }
            }
        } catch (Exception ignore) {}

        model.addAttribute("canUpdateOrderStatus", canUpdateOrderStatus);

        return "evmPage/evmManufacturerSettlement";
    }

    @PostMapping("/update-status")
    public String updateSettlement(@RequestParam("settlementID") int settlementID,
                                   @RequestParam(value="saleOrderID", required=false) Integer saleOrderID,
                                   @RequestParam("status") String status,
                                   @RequestParam(value="reimbursedAmount", required=false) BigDecimal reimbursedAmount,
                                   @RequestParam(value="manufDiscount", required=false) BigDecimal manufDiscount,
                                   @RequestParam(value="notes", required=false) String notes,
                                   RedirectAttributes ra) {
        try {
            if (reimbursedAmount == null) reimbursedAmount = BigDecimal.ZERO;
            com.dealermanagementsysstem.project.Model.DTOManufacturerDiscountSettlement updated;
            if (settlementID <= 0) {
                if (saleOrderID == null) { ra.addFlashAttribute("error","Missing saleOrderID for new settlement"); return "redirect:/evm/settlement"; }
                DTOSaleOrder so = saleOrderDAO.getSaleOrderById(saleOrderID);
                if (so == null || so.getDealer()==null) { ra.addFlashAttribute("error","Sale order not found for settlement creation"); return "redirect:/evm/settlement"; }
                BigDecimal baseDiscount = manufDiscount != null ? manufDiscount : BigDecimal.ZERO;
                com.dealermanagementsysstem.project.Model.DTOManufacturerDiscountSettlement created = settlementDAO.create(saleOrderID, so.getDealer().getDealerID(), baseDiscount);
                if (created == null) { ra.addFlashAttribute("error","Failed creating settlement record"); return "redirect:/evm/settlement"; }
                settlementID = created.getSettlementID();
                updated = settlementDAO.updateStatus(settlementID, status, reimbursedAmount, notes);
            } else {
                updated = settlementDAO.updateStatus(settlementID, status, reimbursedAmount, notes);
            }
            if (updated != null) {
                ra.addFlashAttribute("message", String.format("Settlement #%d status=%s reimbursed %s / total %s outstanding %s", updated.getSettlementID(), updated.getStatus(), updated.getReimbursedAmount(), updated.getTotalManufacturerDiscount(), updated.getOutstanding()));
            } else {
                ra.addFlashAttribute("error","Settlement update failed");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Update error: " + e.getMessage());
        }
        return "redirect:/evm/settlement";
    }
}
