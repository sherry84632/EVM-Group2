package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.*;
import com.dealermanagementsysstem.project.controller.base.BaseController;
import com.dealermanagementsysstem.project.service.support.AuthContextService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Date;

@Controller
@RequestMapping("/contract")
public class SaleContractController extends BaseController {

    @Autowired private DAOSaleContract daoContract; // DI
    @Autowired private DAOSaleOrder daoSaleOrder; // DI
    @Autowired private AuthContextService authContextService;
    @Autowired private DAOAccount daoAccount;

    @Override protected AuthContextService authService() { return authContextService; }

    @PostMapping("/create")
    public String createFromSaleOrder(@RequestParam("saleOrderID") int saleOrderID, RedirectAttributes ra) {
        DTOSaleOrder so = daoSaleOrder.getSaleOrderById(saleOrderID);
        if (so == null) { ra.addFlashAttribute("error","Sale order not found"); return "redirect:/saleorder/detail/"+saleOrderID; }
        DTOSaleContract existing = daoContract.getSaleContractBySaleOrderId(saleOrderID);
        if (existing != null) { ra.addFlashAttribute("message","Contract already exists"); return "redirect:/contract/detail/"+ existing.getContractID(); }
        DTOSaleContract c = new DTOSaleContract();
        c.setSaleOrder(so);
        c.setContractDate(new java.util.Date());
        c.setStatus(SaleContractStatus.ACTIVE);
        c.setTotalAmount(so.getTotalAmount()!=null? so.getTotalAmount() : java.math.BigDecimal.ZERO);
        boolean ok = daoContract.createSaleContract(c);
        ra.addFlashAttribute(ok?"message":"error", ok?"Contract created successfully":"Failed to create contract");
        DTOSaleContract created = daoContract.getSaleContractBySaleOrderId(saleOrderID);
        return created!=null? "redirect:/contract/detail/"+created.getContractID() : "redirect:/saleorder/detail/"+saleOrderID;
    }

    @GetMapping("/detail/{id}")
    public String detail(@PathVariable int id, Model model, RedirectAttributes ra) {
        DTOSaleContract c = daoContract.getSaleContractById(id);
        if (c == null) { ra.addFlashAttribute("error","Contract not found"); return "redirect:/saleorder"; }
        DTOSaleOrder so = daoSaleOrder.getSaleOrderById(c.getSaleOrder().getSaleOrderID());
        c.setSaleOrder(so);
        model.addAttribute("contract", c);
        return "contract/contractDetail";
    }

    @GetMapping("/list")
    public String list(Model model, HttpSession session) {
        addUserContext(model, session, daoAccount);
        model.addAttribute("contracts", daoContract.getAllSaleContracts());
        return "contract/contractList";
    }

    @PostMapping("/updateStatus")
    public String updateStatus(@RequestParam int contractID, @RequestParam String status, RedirectAttributes ra) {
        try {
            SaleContractStatus st = SaleContractStatus.valueOf(status.toUpperCase());
            boolean ok = daoContract.updateSaleContractStatus(contractID, st);
            ra.addFlashAttribute(ok?"message":"error", ok?"Status updated to "+st.name():"Cannot update contract status");
        } catch (IllegalArgumentException e) { ra.addFlashAttribute("error", "Invalid status value"); }
        return "redirect:/contract/detail/" + contractID;
    }
}
