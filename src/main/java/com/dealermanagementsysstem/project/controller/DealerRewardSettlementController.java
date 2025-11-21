package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Collectors;
import java.util.Map;

@Controller
@RequestMapping("/evm/reward-settlement")
public class DealerRewardSettlementController {
    private final DAODealerRewardSettlement rewardDAO = new DAODealerRewardSettlement();
    private final DAODealer daoDealer = new DAODealer();
    private final DAOPurchaseOrder daoPurchaseOrder = new DAOPurchaseOrder();
    private final DAOAccount daoAccount = new DAOAccount();

    private boolean hasEvmRole(Authentication auth){
        if(auth==null) return false;
        for(GrantedAuthority ga: auth.getAuthorities()){
            String r = ga.getAuthority();
            if("ROLE_ADMIN".equals(r) || "ROLE_EVM".equals(r)) return true;
        }
        return false;
    }

    @GetMapping
    public String list(@RequestParam(required=false) Integer dealerId,
                       @RequestParam(required=false) Integer year,
                       @RequestParam(required=false) Integer month,
                       @RequestParam(required=false) String status,
                       Model model){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(!hasEvmRole(auth)){ model.addAttribute("error","Access denied"); return "dealerPage/errorPage"; }
        var list = rewardDAO.filter(dealerId, year, month, status);
        model.addAttribute("settlements", list);
        model.addAttribute("filterDealerId", dealerId);
        model.addAttribute("filterYear", year);
        model.addAttribute("filterMonth", month);
        model.addAttribute("filterStatus", status);
        model.addAttribute("dealerLevels", daoDealer.getAllDealerLevels());
        java.util.List<DTODealer> dealers = daoDealer.getAllDealers();
        Map<Integer,String> dealerMap = dealers.stream().collect(Collectors.toMap(DTODealer::getDealerID, DTODealer::getDealerName));
        model.addAttribute("dealers", dealers);
        model.addAttribute("dealerMap", dealerMap);

        // Attach purchase orders summary to model for each settlement (map by settlement ID)
        java.util.Map<Integer, java.util.List<DTOPurchaseOrder>> poMap = new java.util.HashMap<>();
        java.util.Map<Integer, java.math.BigDecimal> aggregatedImportValueMap = new java.util.HashMap<>();
        for(DTODealerRewardSettlement s : list){
            java.util.List<DTOPurchaseOrder> pos = getMonthlyDeliveredPurchaseOrders(s.getDealerID(), s.getPeriodYear(), s.getPeriodMonth());
            poMap.put(s.getRewardSettlementID(), pos);
            java.math.BigDecimal sum = java.math.BigDecimal.ZERO;
            for(DTOPurchaseOrder po : pos){
                if(po.getTotalAmount()!=null) sum = sum.add(po.getTotalAmount());
            }
            aggregatedImportValueMap.put(s.getRewardSettlementID(), sum);
        }
        model.addAttribute("purchaseOrdersBySettlement", poMap);
        model.addAttribute("aggregatedImportValueMap", aggregatedImportValueMap);

        return "evmPage/dealerRewardSettlement";
    }

    @PostMapping("/generate")
    public String generate(@RequestParam Integer dealerId,
                           @RequestParam Integer year,
                           @RequestParam Integer month,
                           Model model){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(!hasEvmRole(auth)){ model.addAttribute("error","Access denied"); return "dealerPage/errorPage"; }
        DTODealer dealer = null;
        try { dealer = daoDealer.getDealerById(dealerId); } catch (Exception ignored) {}
        if (dealer == null){ model.addAttribute("error","Dealer not found"); return "redirect:/evm/reward-settlement"; }
        int importedQtyMonth = getImportedQuantityForMonth(dealerId, year, month);
        java.math.BigDecimal importedValueMonth = getImportedValueForMonth(dealerId, year, month);
        DTODealerLevel lvl = daoDealer.getDealerLevelById(dealer.getLevelID());
        double pct = (lvl!=null? lvl.getRewardPercent():0.0);
        BigDecimal rewardPercent = BigDecimal.valueOf(pct);
        BigDecimal rewardAmount = importedValueMonth.multiply(rewardPercent).divide(BigDecimal.valueOf(100));
        DTODealerRewardSettlement existing = rewardDAO.getByDealerAndPeriod(dealerId, year, month);
        if(existing==null){
            rewardDAO.create(dealerId, year, month, importedQtyMonth, rewardPercent, rewardAmount);
        } else {
            // update amounts but keep status
            rewardDAO.updateStatus(existing.getRewardSettlementID(), existing.getStatus(), rewardAmount, existing.getNotes());
        }
        return "redirect:/evm/reward-settlement?dealerId="+dealerId+"&year="+year+"&month="+month;
    }

    @PostMapping("/auto-generate-current")
    public String autoGenerateCurrent(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(!hasEvmRole(auth)) return "redirect:/evm/reward-settlement";
        java.time.LocalDate now = java.time.LocalDate.now();
        int year = now.getYear(); int month = now.getMonthValue();
        for(DTODealer d : daoDealer.getAllDealers()){
            int dealerId = d.getDealerID();
            int qty = getImportedQuantityForMonth(dealerId, year, month);
            java.math.BigDecimal val = getImportedValueForMonth(dealerId, year, month);
            DTODealerLevel lvl = daoDealer.getDealerLevelById(d.getLevelID());
            double pct = (lvl!=null? lvl.getRewardPercent():0.0);
            BigDecimal rewardPercent = BigDecimal.valueOf(pct);
            BigDecimal rewardAmount = val.multiply(rewardPercent).divide(BigDecimal.valueOf(100));
            DTODealerRewardSettlement existing = rewardDAO.getByDealerAndPeriod(dealerId, year, month);
            if(existing==null){
                rewardDAO.create(dealerId, year, month, qty, rewardPercent, rewardAmount);
            } else {
                rewardDAO.updateStatus(existing.getRewardSettlementID(), existing.getStatus(), rewardAmount, existing.getNotes());
            }
        }
        return "redirect:/evm/reward-settlement?year="+year+"&month="+month;
    }

    @PostMapping("/update-status")
    public String updateStatus(@RequestParam Integer id,
                               @RequestParam String status,
                               @RequestParam(required=false) String notes){
        DTODealerRewardSettlement dto = rewardDAO.getById(id);
        if(dto!=null){
            rewardDAO.updateStatus(id, status, dto.getRewardAmount(), notes);
        }
        return "redirect:/evm/reward-settlement";
    }

    private int getImportedQuantityForMonth(int dealerId, int year, int month){
        // Simplified: sum quantity of delivered/completed purchase orders created in that month
        String sql = "SELECT ISNULL(SUM(pod.Quantity),0) AS Qty FROM PurchaseOrder po JOIN PurchaseOrderDetail pod ON po.PurchaseOrderID=pod.PurchaseOrderID WHERE po.DealerID=? AND po.Status IN ('DELIVERED','COMPLETED') AND YEAR(po.CreatedAt)=? AND MONTH(po.CreatedAt)=?";
        try(java.sql.Connection c=utils.DBUtils.getConnection(); java.sql.PreparedStatement ps=c.prepareStatement(sql)){
            ps.setInt(1,dealerId); ps.setInt(2,year); ps.setInt(3,month); try(java.sql.ResultSet rs=ps.executeQuery()){ if(rs.next()) return rs.getInt("Qty"); }
        } catch(Exception e){ e.printStackTrace(); }
        return 0;
    }

    private java.math.BigDecimal getImportedValueForMonth(int dealerId, int year, int month){
        String sql = "SELECT ISNULL(SUM(pod.Subtotal),0) AS Val FROM PurchaseOrder po JOIN PurchaseOrderDetail pod ON po.PurchaseOrderID=pod.PurchaseOrderID WHERE po.DealerID=? AND po.Status IN ('DELIVERED','COMPLETED') AND YEAR(po.CreatedAt)=? AND MONTH(po.CreatedAt)=?";
        try(java.sql.Connection c=utils.DBUtils.getConnection(); java.sql.PreparedStatement ps=c.prepareStatement(sql)){
            ps.setInt(1,dealerId); ps.setInt(2,year); ps.setInt(3,month); try(java.sql.ResultSet rs=ps.executeQuery()){ if(rs.next()) return rs.getBigDecimal("Val"); }
        } catch(Exception e){ e.printStackTrace(); }
        return java.math.BigDecimal.ZERO;
    }

    private java.util.List<DTOPurchaseOrder> getMonthlyDeliveredPurchaseOrders(int dealerId, int year, int month){
        String sql = "SELECT po.PurchaseOrderID, po.DealerID, po.StaffID, po.CreatedAt, po.Status, po.TotalAmount, po.EvmID FROM PurchaseOrder po WHERE po.DealerID=? AND po.Status IN ('DELIVERED','COMPLETED') AND YEAR(po.CreatedAt)=? AND MONTH(po.CreatedAt)=? ORDER BY po.PurchaseOrderID";
        java.util.List<DTOPurchaseOrder> list = new java.util.ArrayList<>();
        try(java.sql.Connection c=utils.DBUtils.getConnection(); java.sql.PreparedStatement ps=c.prepareStatement(sql)){
            ps.setInt(1,dealerId); ps.setInt(2,year); ps.setInt(3,month); try(java.sql.ResultSet rs=ps.executeQuery()){
                while(rs.next()){ DTOPurchaseOrder po = new DTOPurchaseOrder(); po.setPurchaseOrderId(rs.getInt("PurchaseOrderID")); po.setCreatedAt(rs.getTimestamp("CreatedAt")); po.setStatus(PurchaseOrderStatus.valueOf(rs.getString("Status"))); po.setTotalAmount(rs.getBigDecimal("TotalAmount")); list.add(po);} }
        } catch(Exception e){ e.printStackTrace(); }
        return list;
    }
}
