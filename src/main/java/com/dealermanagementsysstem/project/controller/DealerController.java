package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DAODealer;
import com.dealermanagementsysstem.project.Model.DAODealerStaff;
import com.dealermanagementsysstem.project.Model.DAOAccount;
import com.dealermanagementsysstem.project.Model.DAODealerLevel;
import com.dealermanagementsysstem.project.Model.DTODealer;
import com.dealermanagementsysstem.project.Model.DTODealerStaff;
import com.dealermanagementsysstem.project.Model.DTOAccount;
import com.dealermanagementsysstem.project.Model.DTODealerLevel;
import com.dealermanagementsysstem.project.Model.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.sql.SQLException;
import java.util.List;

@Controller
@RequestMapping("/dealer")
public class DealerController {

    @Autowired
    private DAODealer daoDealer;

    @Autowired
    private DAODealerStaff daoDealerStaff;

    @Autowired
    private DAOAccount daoAccount;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DAODealerLevel daoDealerLevel;

    // View dealer detail with account management
    @GetMapping("/{id}/detail")
    public String viewDealerDetail(@PathVariable int id, Model model, RedirectAttributes redirectAttributes) {
        try {
            DTODealer dealer = daoDealer.getDealerById(id);
            if (dealer == null) {
                redirectAttributes.addFlashAttribute("message", "❌ Dealer not found!");
                return "redirect:/dealer/management";
            }

            model.addAttribute("dealer", dealer);

            // Load staff accounts for this dealer
            List<DTODealerStaff> staffList = daoDealerStaff.getStaffsByDealerId(id);
            model.addAttribute("staffList", staffList);

            return "evmPage/dealerDetail";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("message", "❌ Error loading dealer: " + e.getMessage());
            return "redirect:/dealer/management";
        }
    }

    @GetMapping("/management")
    public String listDealers(Model model, @ModelAttribute("message") String message) throws SQLException {
        List<DTODealer> dealers = daoDealer.getAllDealers();
        model.addAttribute("dealers", dealers);

        // Load dealer levels for dropdown
        List<DTODealerLevel> levels = daoDealerLevel.getAllDealerLevels();
        model.addAttribute("dealerLevels", levels);

        // Nếu có message thì hiển thị
        if (message != null && !message.isEmpty()) {
            model.addAttribute("message", message);
        }

        return "evmPage/dealer-management";
    }

    @PostMapping("/create")
    public String createDealer(@ModelAttribute DTODealer d, RedirectAttributes redirectAttributes) throws SQLException {
        d.setDealerID(0); // 🔹 thêm dòng này để tránh lỗi binding int rỗng
        daoDealer.insertDealer(d);
        redirectAttributes.addFlashAttribute("message", "✅ Dealer created successfully!");
        return "redirect:/dealer/management";
    }

    // 🟠 UPDATE
    @PostMapping("/{id}/update")
    public String updateDealer(@PathVariable int id, @ModelAttribute DTODealer d, RedirectAttributes redirectAttributes) throws SQLException {
        d.setDealerID(id);
        daoDealer.updateDealer(d);
        redirectAttributes.addFlashAttribute("message", "🟡 Dealer updated successfully!");
        return "redirect:/dealer/management";
    }

    // 🔴 DELETE - with error handling for foreign key constraints
    @PostMapping("/{id}/delete")
    public String deleteDealer(@PathVariable int id, RedirectAttributes redirectAttributes) {
        try {
            // Check if dealer has any customers
            // Note: This check happens at database level via foreign key
            daoDealer.deleteDealer(id);
            redirectAttributes.addFlashAttribute("message", "✅ Dealer deleted successfully!");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            // Handle foreign key constraint violation
            if (e.getMessage() != null && (
                e.getMessage().contains("REFERENCE constraint") ||
                e.getMessage().contains("foreign key") ||
                e.getMessage().contains("FKjxm2geivgydugseqtjf842mg"))) {

                redirectAttributes.addFlashAttribute("message",
                    "❌ Cannot delete dealer! This dealer still has customers or orders associated. " +
                    "Please reassign or delete related data first.");
                redirectAttributes.addFlashAttribute("messageType", "error");

                System.out.println("⚠️ Cannot delete dealer ID " + id + ": Has related customers/orders");
            } else {
                // Other database errors
                redirectAttributes.addFlashAttribute("message",
                    "❌ Error deleting dealer: " + e.getMessage());
                redirectAttributes.addFlashAttribute("messageType", "error");

                System.out.println("❌ Error deleting dealer ID " + id + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        return "redirect:/dealer/management";
    }

    // Create staff account for specific dealer
    @PostMapping("/{dealerId}/staff/create")
    public String createStaffAccount(
            @PathVariable int dealerId,
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("staffFullName") String staffFullName,
            @RequestParam(value = "staffPosition", required = false) String staffPosition,
            @RequestParam(value = "staffPhone", required = false) String staffPhone,
            @RequestParam(value = "staffEmail", required = false) String staffEmail,
            RedirectAttributes redirectAttributes) {

        try {
            // 1. Create Account
            DTOAccount account = new DTOAccount();
            account.setUsername(username.trim());
            account.setEmail(email.trim());
            account.setPassword(passwordEncoder.encode(password));
            account.setRole(Role.DEALERSTAFF);
            account.setActive(true);

            int accountId = daoAccount.insertAccount(account);

            if (accountId <= 0) {
                redirectAttributes.addFlashAttribute("message", "❌ Failed to create account!");
                return "redirect:/dealer/" + dealerId + "/detail";
            }

            // 2. Create DealerStaff
            DTODealerStaff staff = new DTODealerStaff();
            staff.setFullName(staffFullName.trim());
            staff.setPosition(staffPosition != null && !staffPosition.trim().isEmpty() ? staffPosition.trim() : "Sales");
            staff.setPhone(staffPhone != null ? staffPhone.trim() : "");
            staff.setEmail(staffEmail != null && !staffEmail.trim().isEmpty() ? staffEmail.trim() : email.trim());

            // Link to account
            DTOAccount accountRef = new DTOAccount();
            accountRef.setAccountId(accountId);
            staff.setAccount(accountRef);

            // Link to dealer
            DTODealer dealerRef = new DTODealer();
            dealerRef.setDealerID(dealerId);
            staff.setDealer(dealerRef);

            int staffId = daoDealerStaff.insertDealerStaff(staff);

            if (staffId <= 0) {
                // Rollback
                daoAccount.deleteAccount(accountId);
                redirectAttributes.addFlashAttribute("message", "❌ Failed to create staff!");
                return "redirect:/dealer/" + dealerId + "/detail";
            }

            redirectAttributes.addFlashAttribute("message", "✅ Staff account created successfully!");

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("message", "❌ Error: " + e.getMessage());
        }

        return "redirect:/dealer/" + dealerId + "/detail";
    }
}
