package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.sql.SQLException;
import java.util.List;

/**
 * Controller for Dealer Company Management (ADMIN only)
 * Manages dealer companies separately from accounts
 */
@Controller
@RequestMapping("/dealer/company")
public class DealerCompanyController {

    @Autowired
    private DAODealer daoDealer;

    @Autowired
    private DAODealerLevel daoDealerLevel;

    @Autowired
    private DAODealerStaff daoDealerStaff;

    @Autowired
    private DAOAccount daoAccount;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * List all dealer companies
     */
    @GetMapping({"/list", "/"})
    public String listDealers(Model model) throws SQLException {
        List<DTODealer> dealers = daoDealer.getAllDealers();
        model.addAttribute("dealers", dealers);

        // Load dealer levels for dropdown
        List<DTODealerLevel> levels = daoDealerLevel.getAllDealerLevels();
        model.addAttribute("dealerLevels", levels);

        return "evmPage/dealerCompanyList";
    }

    /**
     * Create new dealer company (no account)
     */
    @PostMapping("/create")
    public String createDealer(@ModelAttribute DTODealer dealer, RedirectAttributes redirectAttributes) {
        try {
            // Validation
            if (dealer.getDealerName() == null || dealer.getDealerName().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Dealer name is required!");
                return "redirect:/dealer/company/list";
            }

            if (dealer.getPhone() == null || dealer.getPhone().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Phone is required!");
                return "redirect:/dealer/company/list";
            }

            if (dealer.getEmail() == null || dealer.getEmail().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Email is required!");
                return "redirect:/dealer/company/list";
            }

            // Set defaults
            dealer.setDealerID(0); // Auto-generate
            if (dealer.getEvmID() <= 0) {
                dealer.setEvmID(1); // Default EVM
            }
            if (dealer.getPolicyID() <= 0) {
                dealer.setPolicyID(0); // NULL
            }
            if (dealer.getLevelID() <= 0) {
                dealer.setLevelID(1); // Default level
            }

            int dealerId = daoDealer.insertDealer(dealer);

            if (dealerId > 0) {
                redirectAttributes.addFlashAttribute("successMessage",
                    "✅ Dealer company created successfully! (ID: " + dealerId + ")");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Failed to create dealer!");
            }

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Error: " + e.getMessage());
        }

        return "redirect:/dealer/company/list";
    }

    /**
     * Update dealer company
     */
    @PostMapping("/{id}/update")
    public String updateDealer(
            @PathVariable int id,
            @ModelAttribute DTODealer dealer,
            RedirectAttributes redirectAttributes) {
        try {
            dealer.setDealerID(id);
            daoDealer.updateDealer(dealer);
            redirectAttributes.addFlashAttribute("successMessage", "✅ Dealer updated successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Error: " + e.getMessage());
        }

        return "redirect:/dealer/company/list";
    }

    /**
     * Delete dealer company
     */
    @PostMapping("/{id}/delete")
    public String deleteDealer(@PathVariable int id, RedirectAttributes redirectAttributes) {
        try {
            daoDealer.deleteDealer(id);
            redirectAttributes.addFlashAttribute("successMessage", "✅ Dealer deleted successfully!");
        } catch (Exception e) {
            if (e.getMessage() != null && (
                e.getMessage().contains("REFERENCE constraint") ||
                e.getMessage().contains("foreign key"))) {
                redirectAttributes.addFlashAttribute("errorMessage",
                    "⚠️ Cannot delete dealer! This dealer has related data (customers, orders, or staff).");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Error: " + e.getMessage());
            }
        }

        return "redirect:/dealer/company/list";
    }

    /**
     * View dealer company detail with accounts
     */
    @GetMapping("/{id}/detail")
    public String viewDealerDetail(@PathVariable int id, Model model, RedirectAttributes redirectAttributes) {
        try {
            DTODealer dealer = daoDealer.getDealerById(id);
            if (dealer == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Dealer company not found!");
                return "redirect:/dealer/company/list";
            }

            model.addAttribute("dealer", dealer);

            // Load dealer level name
            try {
                DTODealerLevel level = daoDealerLevel.getDealerLevelById(dealer.getLevelID());
                model.addAttribute("levelName", level != null ? level.getLevelName() : "Unknown");
            } catch (Exception e) {
                model.addAttribute("levelName", "Level " + dealer.getLevelID());
            }

            // Load accounts for this dealer
            List<DTODealerStaff> accountList = daoDealerStaff.getStaffsByDealerId(id);
            model.addAttribute("accountList", accountList);

            return "evmPage/dealerCompanyDetail";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Error loading dealer: " + e.getMessage());
            return "redirect:/dealer/company/list";
        }
    }

    /**
     * Create account for dealer company
     */
    @PostMapping("/{dealerId}/account/create")
    public String createAccountForDealer(
            @PathVariable int dealerId,
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("role") String roleStr,
            @RequestParam("staffFullName") String staffFullName,
            @RequestParam(value = "staffPosition", required = false) String staffPosition,
            @RequestParam(value = "staffPhone", required = false) String staffPhone,
            @RequestParam(value = "staffEmail", required = false) String staffEmail,
            RedirectAttributes redirectAttributes) {

        // Parse role
        Role accountRole;
        try {
            accountRole = Role.valueOf(roleStr);
            if (accountRole != Role.DEALER && accountRole != Role.DEALERSTAFF) {
                redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Can only create DEALER or DEALERSTAFF accounts!");
                return "redirect:/dealer/company/" + dealerId + "/detail";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Invalid role!");
            return "redirect:/dealer/company/" + dealerId + "/detail";
        }

        try {
            // Validation
            if (username == null || username.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Username is required!");
                return "redirect:/dealer/company/" + dealerId + "/detail";
            }

            if (email == null || email.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Email is required!");
                return "redirect:/dealer/company/" + dealerId + "/detail";
            }

            if (password == null || password.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Password is required!");
                return "redirect:/dealer/company/" + dealerId + "/detail";
            }

            if (staffFullName == null || staffFullName.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Full Name is required!");
                return "redirect:/dealer/company/" + dealerId + "/detail";
            }

            if (staffPhone == null || staffPhone.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Phone is required!");
                return "redirect:/dealer/company/" + dealerId + "/detail";
            }

            // Check if email already exists
            if (daoAccount.emailExists(email, null)) {
                redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Email already exists!");
                return "redirect:/dealer/company/" + dealerId + "/detail";
            }

            // 1. Create Account
            DTOAccount account = new DTOAccount();
            account.setUsername(username.trim());
            account.setEmail(email.trim());
            account.setPassword(passwordEncoder.encode(password));
            account.setRole(accountRole);
            account.setActive(true);

            int accountId = daoAccount.insertAccount(account);

            if (accountId <= 0) {
                redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Failed to create account!");
                return "redirect:/dealer/company/" + dealerId + "/detail";
            }

            // 2. Create DealerStaff record
            DTODealerStaff staff = new DTODealerStaff();
            staff.setFullName(staffFullName.trim());

            // Position based on role
            if (accountRole == Role.DEALER) {
                staff.setPosition(staffPosition != null && !staffPosition.trim().isEmpty() ? staffPosition.trim() : "Manager");
            } else {
                staff.setPosition(staffPosition != null && !staffPosition.trim().isEmpty() ? staffPosition.trim() : "Sales");
            }

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
                // Rollback: delete account
                daoAccount.deleteAccount(accountId);
                redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Failed to create staff record!");
                return "redirect:/dealer/company/" + dealerId + "/detail";
            }

            String roleLabel = accountRole == Role.DEALER ? "Manager" : "Staff";
            redirectAttributes.addFlashAttribute("successMessage", "✅ " + roleLabel + " account created successfully!");

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Error: " + e.getMessage());
        }

        return "redirect:/dealer/company/" + dealerId + "/detail";
    }
}

