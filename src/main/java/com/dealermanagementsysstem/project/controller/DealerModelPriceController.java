package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/dealer/model-prices")
public class DealerModelPriceController {
    private final DAODealerModelPrice daoDealerModelPrice;
    private final DAOVehicleModel daoVehicleModel;

    public DealerModelPriceController(DAODealerModelPrice dmp, DAOVehicleModel vm){ this.daoDealerModelPrice=dmp; this.daoVehicleModel=vm; }

    @GetMapping
    public String list(Model model, HttpSession session){
        DTOAccount acc = (DTOAccount) session.getAttribute("loggedInAccount");
        if(acc==null || acc.getDealerStaff()==null || acc.getDealerStaff().getDealer()==null){
            model.addAttribute("error","No dealer context"); return "dealerPage/dealerModelPriceList"; }
        int dealerId = acc.getDealerStaff().getDealer().getDealerID();
        model.addAttribute("models", daoDealerModelPrice.listDealerModels(dealerId));
        model.addAttribute("canEdit", acc.getRole()==Role.DEALER); // only Manager (Role.DEALER)
        return "dealerPage/dealerModelPriceList";
    }

    @PostMapping("/update")
    public String update(@RequestParam int modelId, @RequestParam String price, HttpSession session, RedirectAttributes ra){
        DTOAccount acc = (DTOAccount) session.getAttribute("loggedInAccount");
        if(acc==null || acc.getDealerStaff()==null || acc.getDealerStaff().getDealer()==null){ ra.addFlashAttribute("error","No dealer context"); return "redirect:/dealer/model-prices"; }
        if(acc.getRole()!=Role.DEALER){ ra.addFlashAttribute("error","Only Manager can update prices"); return "redirect:/dealer/model-prices"; }
        int dealerId = acc.getDealerStaff().getDealer().getDealerID();
        try{
            java.math.BigDecimal p = new java.math.BigDecimal(price);
            if(p.compareTo(java.math.BigDecimal.ZERO)<0){ ra.addFlashAttribute("error","Price must be >= 0"); return "redirect:/dealer/model-prices"; }
            boolean ok = daoDealerModelPrice.upsertPrice(dealerId, modelId, p);
            daoDealerModelPrice.propagateToInventory(dealerId, modelId, p);
            if(ok) ra.addFlashAttribute("message","Updated price for model="+modelId); else ra.addFlashAttribute("error","Update failed");
        }catch(Exception e){ ra.addFlashAttribute("error","Invalid price: "+e.getMessage()); }
        return "redirect:/dealer/model-prices";
    }
}

