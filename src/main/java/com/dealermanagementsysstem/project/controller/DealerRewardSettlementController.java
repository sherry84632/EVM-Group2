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
        // For each settlement determine cutoff: previous paid (or previous) settlement's PaidDate/CreatedAt
        java.util.Map<Integer, DTODealerRewardSettlement> previousById = new java.util.HashMap<>();
        // Build ordered list per dealer+period
        java.util.Map<String, java.util.List<DTODealerRewardSettlement>> grouped = new java.util.HashMap<>();
        for(DTODealerRewardSettlement s: list){
            String key=s.getDealerID()+"-"+s.getPeriodYear()+"-"+s.getPeriodMonth();
            grouped.computeIfAbsent(key,k->new java.util.ArrayList<>()).add(s);
        }
        for(var entry: grouped.entrySet()){
            var settlements=entry.getValue(); settlements.sort(java.util.Comparator.comparingInt(DTODealerRewardSettlement::getRewardSettlementID));
            DTODealerRewardSettlement prev=null; for(DTODealerRewardSettlement s: settlements){ previousById.put(s.getRewardSettlementID(), prev); prev=s; }
        }
        for(DTODealerRewardSettlement s : list){
            DTODealerRewardSettlement prev = previousById.get(s.getRewardSettlementID());
            java.sql.Timestamp cutoff=null; if(prev!=null){ cutoff = prev.getPaidDate()!=null? prev.getPaidDate(): prev.getCreatedAt(); }
            java.util.List<DTOPurchaseOrder> pos = getMonthlyDeliveredPurchaseOrdersAfter(s.getDealerID(), s.getPeriodYear(), s.getPeriodMonth(), cutoff);
            poMap.put(s.getRewardSettlementID(), pos);
            aggregatedImportValueMap.put(s.getRewardSettlementID(), sumValue(pos));
        }
        model.addAttribute("purchaseOrdersBySettlement", poMap);
        model.addAttribute("aggregatedImportValueMap", aggregatedImportValueMap);

        // Add aggregates for UI (total reward, paid, remain)
        java.math.BigDecimal totalReward = java.math.BigDecimal.ZERO;
        java.math.BigDecimal totalPaid = java.math.BigDecimal.ZERO;
        for(DTODealerRewardSettlement s : list){
            if(s.getRewardAmount()!=null) totalReward = totalReward.add(s.getRewardAmount());
            if(s.getReimbursedAmount()!=null) totalPaid = totalPaid.add(s.getReimbursedAmount());
        }
        java.math.BigDecimal totalRemain = totalReward.subtract(totalPaid);
        if(totalRemain.compareTo(java.math.BigDecimal.ZERO)<0) totalRemain = java.math.BigDecimal.ZERO;
        double percentPaid = totalReward.compareTo(java.math.BigDecimal.ZERO)>0 ? totalPaid.divide(totalReward,4, java.math.RoundingMode.HALF_UP).multiply(java.math.BigDecimal.valueOf(100)).doubleValue() : 0.0;
        model.addAttribute("totalReward", totalReward);
        model.addAttribute("totalPaid", totalPaid);
        model.addAttribute("totalRemain", totalRemain);
        model.addAttribute("percentPaid", percentPaid);

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
        DTODealerRewardSettlement latest = rewardDAO.getLatestForPeriod(dealerId, year, month);
        if(latest == null){
            // First settlement covers all month to date
            int qtyImported = importedQtyMonth; java.math.BigDecimal valImported = importedValueMonth;
            BigDecimal rewardAmount = valImported.multiply(rewardPercent).divide(BigDecimal.valueOf(100));
            rewardDAO.create(dealerId, year, month, qtyImported, rewardPercent, rewardAmount);
        } else if(latest.isLocked()) {
            // delta after paid
            java.sql.Timestamp cutoff = latest.getPaidDate()!=null? latest.getPaidDate(): latest.getCreatedAt();
            var deltaPos = getMonthlyDeliveredPurchaseOrdersAfter(dealerId, year, month, cutoff);
            int deltaQty = sumQty(deltaPos); java.math.BigDecimal deltaVal = sumValue(deltaPos);
            if(deltaQty>0 && deltaVal.compareTo(java.math.BigDecimal.ZERO)>0){
                BigDecimal rewardAmount = deltaVal.multiply(rewardPercent).divide(BigDecimal.valueOf(100));
                rewardDAO.createNewEvenIfPeriodExists(dealerId, year, month, deltaQty, rewardPercent, rewardAmount);
            }
        } else {
            // Update existing (recompute full set from start of month)
            BigDecimal rewardAmount = importedValueMonth.multiply(rewardPercent).divide(BigDecimal.valueOf(100));
            rewardDAO.safeUpdateStatusAndAmount(latest.getRewardSettlementID(), latest.getStatus(), rewardAmount, latest.getNotes());
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
            DTODealerRewardSettlement latest = rewardDAO.getLatestForPeriod(dealerId, year, month);
            if(latest == null){
                int qtyImported = qty; java.math.BigDecimal valImported = val;
                BigDecimal rewardAmount = valImported.multiply(rewardPercent).divide(BigDecimal.valueOf(100));
                rewardDAO.create(dealerId, year, month, qtyImported, rewardPercent, rewardAmount);
            } else if(latest.isLocked()) {
                java.sql.Timestamp cutoff = latest.getPaidDate()!=null? latest.getPaidDate(): latest.getCreatedAt();
                var deltaPos = getMonthlyDeliveredPurchaseOrdersAfter(dealerId, year, month, cutoff);
                int deltaQty = sumQty(deltaPos); java.math.BigDecimal deltaVal = sumValue(deltaPos);
                if(deltaQty>0 && deltaVal.compareTo(java.math.BigDecimal.ZERO)>0){
                    BigDecimal rewardAmount = deltaVal.multiply(rewardPercent).divide(BigDecimal.valueOf(100));
                    rewardDAO.createNewEvenIfPeriodExists(dealerId, year, month, deltaQty, rewardPercent, rewardAmount);
                }
            } else {
                BigDecimal rewardAmount = val.multiply(rewardPercent).divide(BigDecimal.valueOf(100));
                rewardDAO.safeUpdateStatusAndAmount(latest.getRewardSettlementID(), latest.getStatus(), rewardAmount, latest.getNotes());
            }
        }
        return "redirect:/evm/reward-settlement?year="+year+"&month="+month;
    }

    @PostMapping("/partial-pay")
    public String partialPay(@RequestParam Integer id,
                             @RequestParam(required=false) java.math.BigDecimal payAmount,
                             @RequestParam(required=false) String notes){
        DTODealerRewardSettlement dto = rewardDAO.getById(id);
        if(dto!=null && !dto.isLocked()){
            if(payAmount==null) payAmount = java.math.BigDecimal.ZERO;
            rewardDAO.partialPay(id, payAmount, notes);
        }
        return "redirect:/evm/reward-settlement";
    }
    @PostMapping("/pay-all")
    public String payAll(@RequestParam Integer id,
                         @RequestParam(required=false) String notes){
        System.out.println("[PayAll] Received request - ID: " + id + ", Notes: " + notes);
        DTODealerRewardSettlement dto = rewardDAO.getById(id);
        if(dto == null) {
            System.out.println(" [PayAll] Settlement not found with ID: " + id);
            return "redirect:/evm/reward-settlement";
        }
        System.out.println(" [PayAll] Current settlement - Locked: " + dto.isLocked() +
                         ", Outstanding: " + dto.getOutstanding() +
                         ", Status: " + dto.getStatus());
        if(dto.isLocked()){
            System.out.println("⚠ [PayAll] Settlement is locked, cannot pay");
            return "redirect:/evm/reward-settlement";
        }
        DTODealerRewardSettlement result = rewardDAO.payAll(id, notes);
        if(result != null) {
            System.out.println(" [PayAll] Payment successful - New Status: " + result.getStatus() +
                             ", Reimbursed: " + result.getReimbursedAmount());
        } else {
            System.out.println(" [PayAll] Payment failed");
        }
        return "redirect:/evm/reward-settlement";
    }

    @PostMapping("/update-status")
    public String updateStatus(@RequestParam Integer id,
                               @RequestParam String status,
                               @RequestParam(required=false) String notes){
        DTODealerRewardSettlement dto = rewardDAO.getById(id);
        if(dto!=null){
            if(dto.isLocked()){
                // allow note update only
                rewardDAO.partialPay(id, java.math.BigDecimal.ZERO, notes); // zero pay -> just note
            } else {
                // change status without altering amounts
                rewardDAO.safeUpdateStatusAndAmount(id, status, dto.getRewardAmount(), notes);
            }
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
    private java.util.List<DTOPurchaseOrder> getMonthlyDeliveredPurchaseOrdersAfter(int dealerId, int year, int month, java.sql.Timestamp afterTs){
        StringBuilder sb=new StringBuilder("SELECT po.PurchaseOrderID, po.CreatedAt, po.Status, po.TotalAmount FROM PurchaseOrder po WHERE po.DealerID=? AND po.Status IN ('DELIVERED','COMPLETED') AND YEAR(po.CreatedAt)=? AND MONTH(po.CreatedAt)=?");
        if(afterTs!=null){ sb.append(" AND po.CreatedAt > ?"); }
        sb.append(" ORDER BY po.PurchaseOrderID");
        java.util.List<DTOPurchaseOrder> list=new java.util.ArrayList<>();
        try(java.sql.Connection c=utils.DBUtils.getConnection(); java.sql.PreparedStatement ps=c.prepareStatement(sb.toString())){
            ps.setInt(1,dealerId); ps.setInt(2,year); ps.setInt(3,month); if(afterTs!=null) ps.setTimestamp(4,afterTs);
            try(java.sql.ResultSet rs=ps.executeQuery()){ while(rs.next()){ DTOPurchaseOrder po=new DTOPurchaseOrder(); po.setPurchaseOrderId(rs.getInt(1)); po.setCreatedAt(rs.getTimestamp(2)); po.setStatus(PurchaseOrderStatus.valueOf(rs.getString(3))); po.setTotalAmount(rs.getBigDecimal(4)); list.add(po);} }
        } catch(Exception e){ e.printStackTrace(); }
        return list;
    }
    private int sumQty(java.util.List<DTOPurchaseOrder> pos){
        if(pos==null||pos.isEmpty()) return 0;
        // Need quantity per PO detail; fallback approximate using sum of quantities from details
        int total=0;
        String sql="SELECT ISNULL(SUM(pod.Quantity),0) AS Qty FROM PurchaseOrderDetail pod WHERE pod.PurchaseOrderID=?";
        try(java.sql.Connection c=utils.DBUtils.getConnection()){
            for(DTOPurchaseOrder po: pos){ try(java.sql.PreparedStatement ps=c.prepareStatement(sql)){ ps.setInt(1,po.getPurchaseOrderId()); try(java.sql.ResultSet rs=ps.executeQuery()){ if(rs.next()) total+=rs.getInt(1); } } }
        } catch(Exception e){ e.printStackTrace(); }
        return total;
    }
    private java.math.BigDecimal sumValue(java.util.List<DTOPurchaseOrder> pos){
        java.math.BigDecimal sum=java.math.BigDecimal.ZERO; if(pos==null) return sum;
        String sql="SELECT ISNULL(SUM(pod.Subtotal),0) AS Val FROM PurchaseOrderDetail pod WHERE pod.PurchaseOrderID=?";
        try(java.sql.Connection c=utils.DBUtils.getConnection()){
            for(DTOPurchaseOrder po: pos){ try(java.sql.PreparedStatement ps=c.prepareStatement(sql)){ ps.setInt(1,po.getPurchaseOrderId()); try(java.sql.ResultSet rs=ps.executeQuery()){ if(rs.next()) sum=sum.add(rs.getBigDecimal(1)); } } }
        } catch(Exception e){ e.printStackTrace(); }
        return sum;
    }
}
