package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Date;

@Controller
@RequestMapping("/contract")
public class SaleContractController {

    private final DAOSaleContract daoContract = new DAOSaleContract();
    private final DAOSaleOrder daoSaleOrder = new DAOSaleOrder();

    @PostMapping("/create")
    public String createFromSaleOrder(@RequestParam("saleOrderID") int saleOrderID, RedirectAttributes ra) {
        // Check if user has permission to create contracts (DEALER/DEALERSTAFF only)
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            DAOAccount daoAccount = new DAOAccount();
            DTOAccount acc = daoAccount.findAccountByEmail(auth.getName());
            if (acc != null && (acc.getRole() == Role.ADMIN || acc.getRole() == Role.EVMSTAFF)) {
                ra.addFlashAttribute("error", "You do not have permission to create contracts. This action is restricted to dealers.");
                return "redirect:/saleorder/detail/" + saleOrderID;
            }
        }

        DTOSaleOrder so = daoSaleOrder.getSaleOrderById(saleOrderID);
        if (so == null) { ra.addFlashAttribute("error","Sale order not found"); return "redirect:/saleorder/detail/"+saleOrderID; }
        DTOSaleContract existing = daoContract.getSaleContractBySaleOrderId(saleOrderID);
        if (existing != null) { ra.addFlashAttribute("message","Contract already exists"); return "redirect:/contract/detail/"+ existing.getContractID(); }
        DTOSaleContract c = new DTOSaleContract();
        c.setSaleOrder(so);
        c.setContractDate(new java.util.Date());
        c.setStatus(SaleContractStatus.ACTIVE);
        c.setTotalAmount(so.getTotalAmount()!=null? so.getTotalAmount() : java.math.BigDecimal.ZERO);
        c.setRegistrationFee(java.math.BigDecimal.ZERO);
        c.setDeliveryFee(java.math.BigDecimal.ZERO);
        c.setInsuranceFee(java.math.BigDecimal.ZERO);
        c.setServiceFee(java.math.BigDecimal.ZERO);
        c.setTerms("Standard terms will be updated.");
        if(so.getCustomer()!=null){ c.setCustomerAddressSnapshot(so.getCustomer().getAddress()); c.setCustomerIdNumber(so.getCustomer().getIdNumber()); }
        c.setSignStatus(ContractSignStatus.DRAFT);
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

        // Check if current user is EVM role (read-only mode)
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isReadOnly = false;
        if (auth != null && auth.getName() != null) {
            DAOAccount daoAccount = new DAOAccount();
            DTOAccount acc = daoAccount.findAccountByEmail(auth.getName());
            if (acc != null && (acc.getRole() == Role.ADMIN || acc.getRole() == Role.EVMSTAFF)) {
                isReadOnly = true;
                System.out.println("✓ Contract view - Setting isReadOnly=true for user: " + auth.getName() + " (Role: " + acc.getRole() + ")");
            } else if (acc != null) {
                System.out.println("✓ Contract view - Setting isReadOnly=false for user: " + auth.getName() + " (Role: " + acc.getRole() + ")");
            }
        }

        model.addAttribute("contract", c);
        model.addAttribute("isReadOnly", isReadOnly);
        model.addAttribute("grandTotal", c.getGrandTotal());
        System.out.println("✓ Model attributes - contract ID: " + c.getContractID() + ", isReadOnly: " + isReadOnly);
        return "contract/contractDetail";
    }

    @GetMapping("/list")
    public String list(Model model) {
        model.addAttribute("contracts", daoContract.getAllSaleContracts());
        return "contract/contractList";
    }

    @PostMapping("/updateStatus")
    public String updateStatus(@RequestParam int contractID, @RequestParam String status, RedirectAttributes ra) {
        // Check if user has permission to update contract status (DEALER/DEALERSTAFF only)
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            DAOAccount daoAccount = new DAOAccount();
            DTOAccount acc = daoAccount.findAccountByEmail(auth.getName());
            if (acc != null && (acc.getRole() == Role.ADMIN || acc.getRole() == Role.EVMSTAFF)) {
                ra.addFlashAttribute("error", "You do not have permission to update contract status. This action is restricted to dealers.");
                return "redirect:/contract/detail/" + contractID;
            }
        }

        try {
            SaleContractStatus st = SaleContractStatus.valueOf(status.toUpperCase());
            boolean ok = daoContract.updateSaleContractStatus(contractID, st);
            ra.addFlashAttribute(ok?"message":"error", ok?"Status updated to "+st.name():"Cannot update contract status");
        } catch (IllegalArgumentException e) { ra.addFlashAttribute("error", "Invalid status value"); }
        return "redirect:/contract/detail/" + contractID;
    }

    @PostMapping("/updateFeesTerms")
    public String updateFees(@RequestParam int contractID,
                             @RequestParam(required=false) String terms,
                             RedirectAttributes ra){
        DTOSaleContract c = daoContract.getSaleContractById(contractID);
        if(c==null){ ra.addFlashAttribute("error","Contract not found"); return "redirect:/saleorder"; }
        boolean ok = daoContract.updateFeesAndTerms(contractID, c.getRegistrationFee(), c.getDeliveryFee(), c.getInsuranceFee(), c.getServiceFee(), terms);
        if(ok) ra.addFlashAttribute("message","Terms updated"); else ra.addFlashAttribute("error","Update failed");
        return "redirect:/contract/detail/"+contractID;
    }

    @PostMapping("/sign")
    public String sign(@RequestParam int contractID,
                       @RequestParam(required=false) String dealerSignatureName,
                       @RequestParam(required=false) String customerSignatureName,
                       RedirectAttributes ra){
        DTOSaleContract c = daoContract.getSaleContractById(contractID);
        if(c==null){ ra.addFlashAttribute("error","Contract not found"); return "redirect:/saleorder"; }
        boolean ok = daoContract.updateSignatures(contractID, dealerSignatureName, customerSignatureName);
        if(ok) ra.addFlashAttribute("message","Signatures updated"); else ra.addFlashAttribute("error","Sign failed");
        return "redirect:/contract/detail/"+contractID;
    }
}
