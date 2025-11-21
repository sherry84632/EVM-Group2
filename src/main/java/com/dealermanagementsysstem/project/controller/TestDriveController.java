package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/testdrive")
public class TestDriveController {
    private final DAOTestDrive daoTestDrive = new DAOTestDrive();
    private final DAOCustomer daoCustomer = new DAOCustomer();
    private final DAOVehicle daoVehicle = new DAOVehicle();
    private final DAOAccount daoAccount = new DAOAccount();

    private Integer dealerId(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth==null) return null; return daoAccount.getDealerIdByEmail(auth.getName());
    }

    @GetMapping({"","/list"})
    public String list(@RequestParam(value="status",required=false) String status,
                       @RequestParam(value="sort",required=false,defaultValue="desc") String sort,
                       Model model){
        model.addAttribute("activeMenu","testdrive");
        Integer dealerId = dealerId();
        if(dealerId==null){ model.addAttribute("error","Dealer not found"); return "dealerPage/testDriveList"; }
        List<DTOTestDrive> drives = daoTestDrive.getTestDrivesByDealerFiltered(dealerId, status, sort);
        model.addAttribute("drives", drives);
        model.addAttribute("statusFilter", status);
        model.addAttribute("sortDir", sort);
        model.addAttribute("statuses", List.of("NOT_YET","TODAY","ATTENDED","NOT_ATTENDED"));
        model.addAttribute("customers", daoCustomer.getCustomersByDealerId(dealerId));
        model.addAttribute("vehicles", daoVehicle.getAllVehicles());
        return "dealerPage/testDriveList";
    }

    @PostMapping("/create")
    public String create(@RequestParam int customerID,
                         @RequestParam String testDateTime,
                         @RequestParam(required=false) Integer vehicleID,
                         @RequestParam(required=false) String feedback,
                         RedirectAttributes ra){
        Integer dealerId = dealerId();
        if(dealerId==null){ ra.addFlashAttribute("error","Dealer null"); return "redirect:/testdrive/list"; }
        Date when; try{ LocalDateTime ldt = LocalDateTime.parse(testDateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")); when = Date.from(ldt.atZone(java.time.ZoneId.systemDefault()).toInstant()); }catch(Exception ex){ when = new Date(); }
        DTOTestDrive td = new DTOTestDrive();
        DTOCustomer cust = new DTOCustomer(); cust.setCustomerID(customerID); td.setCustomer(cust);
        if(vehicleID!=null && vehicleID>0){ DTOVehicle veh = new DTOVehicle(); veh.setVehicleID(vehicleID); td.setVehicle(veh);} // optional
        DTODealer dealer = new DTODealer(); dealer.setDealerID(dealerId); td.setDealer(dealer);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth!=null){ DTOAccount acc = daoAccount.findAccountByEmail(auth.getName()); if(acc!=null && acc.getDealerStaff()!=null){ DTODealerStaff st = new DTODealerStaff(); st.setStaffID(acc.getDealerStaff().getStaffID()); td.setStaff(st);} }
        td.setTestDate(when); td.setFeedback(feedback); td.setStatus("NOT_YET");
        boolean ok = daoTestDrive.createTestDrive(td);
        if(ok) ra.addFlashAttribute("message","Created test drive"); else ra.addFlashAttribute("error","Create failed");
        return "redirect:/testdrive/list";
    }

    @PostMapping("/status")
    public String updateStatus(@RequestParam int testDriveID, @RequestParam String status, RedirectAttributes ra){
        boolean ok = daoTestDrive.updateStatus(testDriveID, status);
        if(ok) ra.addFlashAttribute("message","Status updated"); else ra.addFlashAttribute("error","Update failed");
        return "redirect:/testdrive/list";
    }

    @PostMapping("/delete")
    public String delete(@RequestParam int testDriveID, RedirectAttributes ra){
        boolean ok = daoTestDrive.deleteTestDrive(testDriveID);
        if(ok) ra.addFlashAttribute("message","Deleted"); else ra.addFlashAttribute("error","Delete failed");
        return "redirect:/testdrive/list";
    }
}
