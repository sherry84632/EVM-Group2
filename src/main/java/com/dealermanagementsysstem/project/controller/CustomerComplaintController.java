package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/complaint")
public class CustomerComplaintController {

    private final DAOCustomerComplaint daoComplaint = new DAOCustomerComplaint();
    private final DAOCustomer daoCustomer = new DAOCustomer();
    private final DAOAccount daoAccount = new DAOAccount();

    private Integer currentDealerId(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth==null) return null;
        String email = auth.getName();
        return daoAccount.getDealerIdByEmail(email);
    }

    private boolean ownsComplaint(Integer dealerId, DTOCustomerComplaint c){
        if(c==null) return false;
        Integer cid = c.getDealer()!=null? c.getDealer().getDealerID(): null;
        return dealerId!=null && cid!=null && dealerId.equals(cid);
    }

    @GetMapping({"","/list"})
    public String list(@RequestParam(value="keyword", required=false) String keyword, Model model){
        model.addAttribute("activeMenu","complaint");
        Integer dealerId = currentDealerId();
        if(dealerId==null){ model.addAttribute("error","Không xác định dealer hiện tại"); return "dealerPage/customerComplaintList"; }
        List<DTOCustomerComplaint> complaints = (keyword!=null && !keyword.isBlank())? daoComplaint.searchComplaints(dealerId, keyword) : daoComplaint.getComplaintsByDealer(dealerId);
        model.addAttribute("complaints", complaints);
        model.addAttribute("keyword", keyword);
        model.addAttribute("statuses", List.of("APPROVED","PROCESSED"));
        return "dealerPage/customerComplaintList";
    }

    @GetMapping("/create")
    public String createForm(Model model){
        model.addAttribute("activeMenu","complaint");
        Integer dealerId = currentDealerId();
        if(dealerId==null){ model.addAttribute("error","Không xác định dealer hiện tại"); return "dealerPage/customerComplaintForm"; }
        List<DTOCustomer> customers = daoCustomer.getCustomersByDealerId(dealerId);
        DTOCustomerComplaint complaint = new DTOCustomerComplaint();
        complaint.setComplaintDate(LocalDate.now());
        model.addAttribute("complaint", complaint);
        model.addAttribute("customers", customers);
        model.addAttribute("statuses", List.of("APPROVED","PROCESSED"));
        return "dealerPage/customerComplaintForm";
    }

    @PostMapping("/save")
    public String save(@RequestParam("customerID") int customerID,
                       @RequestParam("complaintDate") String complaintDateStr,
                       @RequestParam("status") String status,
                       @RequestParam(value="note", required=false) String note,
                       RedirectAttributes ra){
        Integer dealerId = currentDealerId();
        if(dealerId==null){ ra.addFlashAttribute("error","Không xác định dealer"); return "redirect:/complaint/list"; }
        DTOCustomerComplaint c = new DTOCustomerComplaint();
        DTODealer d = new DTODealer(); d.setDealerID(dealerId); c.setDealer(d);
        DTOCustomer cust = new DTOCustomer(); cust.setCustomerID(customerID); c.setCustomer(cust);
        try{ c.setComplaintDate(LocalDate.parse(complaintDateStr)); } catch(Exception ex){ c.setComplaintDate(LocalDate.now()); }
        c.setStatus(status);
        c.setNote(note);
        int id = daoComplaint.insertComplaint(c);
        if(id>0){ ra.addFlashAttribute("message","Tạo khiếu nại thành công (#"+id+")"); }
        else { ra.addFlashAttribute("error","Không thể tạo khiếu nại"); }
        return "redirect:/complaint/list";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable int id, Model model){
        model.addAttribute("activeMenu","complaint");
        Integer dealerId = currentDealerId();
        DTOCustomerComplaint c = daoComplaint.getById(id);
        if(c==null || !ownsComplaint(dealerId,c)){ model.addAttribute("error","Không tìm thấy hoặc không thuộc dealer hiện tại"); return "dealerPage/customerComplaintList"; }
        List<DTOCustomer> customers = dealerId!=null? daoCustomer.getCustomersByDealerId(dealerId): List.of();
        model.addAttribute("complaint", c);
        model.addAttribute("customers", customers);
        model.addAttribute("statuses", List.of("APPROVED","PROCESSED"));
        return "dealerPage/customerComplaintForm";
    }

    @PostMapping("/update")
    public String update(@RequestParam("complaintID") int complaintID,
                         @RequestParam("customerID") int customerID,
                         @RequestParam("complaintDate") String complaintDateStr,
                         @RequestParam("status") String status,
                         @RequestParam(value="note", required=false) String note,
                         RedirectAttributes ra){
        Integer dealerId = currentDealerId();
        DTOCustomerComplaint existing = daoComplaint.getById(complaintID);
        if(existing==null || !ownsComplaint(dealerId, existing)){ ra.addFlashAttribute("error","Không tìm thấy khiếu nại hoặc không thuộc dealer"); return "redirect:/complaint/list"; }
        DTOCustomerComplaint c = new DTOCustomerComplaint();
        c.setComplaintID(complaintID);
        DTODealer d = new DTODealer(); d.setDealerID(dealerId); c.setDealer(d);
        DTOCustomer cust = new DTOCustomer(); cust.setCustomerID(customerID); c.setCustomer(cust);
        try{ c.setComplaintDate(LocalDate.parse(complaintDateStr)); } catch(Exception ex){ c.setComplaintDate(LocalDate.now()); }
        c.setStatus(status);
        c.setNote(note);
        boolean ok = daoComplaint.updateComplaint(c);
        if(ok) ra.addFlashAttribute("message","Cập nhật thành công"); else ra.addFlashAttribute("error","Cập nhật thất bại");
        return "redirect:/complaint/list";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable int id, RedirectAttributes ra){
        Integer dealerId = currentDealerId();
        DTOCustomerComplaint c = daoComplaint.getById(id);
        if(c==null || !ownsComplaint(dealerId,c)){ ra.addFlashAttribute("error","Không tìm thấy khiếu nại hoặc không thuộc dealer"); return "redirect:/complaint/list"; }
        boolean ok = daoComplaint.deleteComplaint(id);
        if(ok) ra.addFlashAttribute("message","Xóa thành công"); else ra.addFlashAttribute("error","Xóa thất bại");
        return "redirect:/complaint/list";
    }
}
