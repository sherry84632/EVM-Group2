package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller for Dealer Staff Management
 * Allows dealers to manage their own staff accounts
 */
@Controller
@RequestMapping("/dealer/staff")
public class DealerStaffManagementController {

    @Autowired
    private DAODealerStaff daoDealerStaff;

    @Autowired
    private DAOAccount daoAccount;

    @Autowired
    private DAODealer daoDealer;

    @Autowired
    private DAODealerLevel daoDealerLevel;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * List all staff for the logged-in dealer
     */
    @GetMapping({"/list", "/"})
    public String listStaff(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        DTOAccount loggedInAccount = (DTOAccount) session.getAttribute("loggedInAccount");

        if (loggedInAccount == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Please login first!");
            return "redirect:/login";
        }

        // Get dealer ID from logged-in account
        Integer dealerId = null;
        if (loggedInAccount.getDealerStaff() != null &&
            loggedInAccount.getDealerStaff().getDealer() != null) {
            dealerId = loggedInAccount.getDealerStaff().getDealer().getDealerID();
        }

        if (dealerId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "⚠️ No dealer associated with your account!");
            return "redirect:/showDealerHomePage";
        }

        try {
            // Get dealer info
            DTODealer dealer = daoDealer.getDealerById(dealerId);
            model.addAttribute("dealer", dealer);

            // Load dealer level name
            if (dealer != null) {
                try {
                    DTODealerLevel level = daoDealerLevel.getDealerLevelById(dealer.getLevelID());
                    model.addAttribute("levelName", level != null ? level.getLevelName() : "Unknown");
                } catch (Exception e) {
                    model.addAttribute("levelName", "Level " + dealer.getLevelID());
                }
            }


            // Get staff list for this dealer
            List<DTODealerStaff> staffList = daoDealerStaff.getStaffsByDealerId(dealerId);
            model.addAttribute("staffList", staffList);

            return "dealerPage/dealerStaffManagement";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Error loading staff: " + e.getMessage());
            return "redirect:/showDealerHomePage";
        }
    }

    /**
     * Create new account for the dealer (DEALER or DEALERSTAFF role)
     */
    @PostMapping("/create")
    public String createStaff(
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("role") String roleStr,
            @RequestParam("staffFullName") String staffFullName,
            @RequestParam(value = "staffPosition", required = false) String staffPosition,
            @RequestParam(value = "staffPhone", required = false) String staffPhone,
            @RequestParam(value = "staffEmail", required = false) String staffEmail,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        DTOAccount loggedInAccount = (DTOAccount) session.getAttribute("loggedInAccount");

        if (loggedInAccount == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Please login first!");
            return "redirect:/login";
        }

        // Get dealer ID from logged-in account
        Integer dealerId = null;
        if (loggedInAccount.getDealerStaff() != null &&
            loggedInAccount.getDealerStaff().getDealer() != null) {
            dealerId = loggedInAccount.getDealerStaff().getDealer().getDealerID();
        }

        if (dealerId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "⚠️ No dealer associated with your account!");
            return "redirect:/showDealerHomePage";
        }

        // Only DEALER role can create accounts, not DEALERSTAFF
        if (loggedInAccount.getRole() != Role.DEALER) {
            redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Only dealer owners can create accounts!");
            return "redirect:/dealer/staff/list";
        }

        // Parse role
        Role accountRole;
        try {
            accountRole = Role.valueOf(roleStr);
            // Only allow DEALER and DEALERSTAFF roles
            if (accountRole != Role.DEALER && accountRole != Role.DEALERSTAFF) {
                redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Can only create DEALER or DEALERSTAFF accounts!");
                return "redirect:/dealer/staff/list";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Invalid role!");
            return "redirect:/dealer/staff/list";
        }

        try {
            // Validation
            if (username == null || username.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Username is required!");
                return "redirect:/dealer/staff/list";
            }

            if (email == null || email.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Email is required!");
                return "redirect:/dealer/staff/list";
            }

            if (password == null || password.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Password is required!");
                return "redirect:/dealer/staff/list";
            }

            if (staffFullName == null || staffFullName.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Full Name is required!");
                return "redirect:/dealer/staff/list";
            }

            if (staffPhone == null || staffPhone.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Phone is required!");
                return "redirect:/dealer/staff/list";
            }

            // Check if email already exists
            if (daoAccount.emailExists(email, null)) {
                redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Email already exists!");
                return "redirect:/dealer/staff/list";
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
                return "redirect:/dealer/staff/list";
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
                return "redirect:/dealer/staff/list";
            }

            String roleLabel = accountRole == Role.DEALER ? "Manager" : "Staff";
            redirectAttributes.addFlashAttribute("successMessage", "✅ " + roleLabel + " account created successfully!");

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Error: " + e.getMessage());
        }

        return "redirect:/dealer/staff/list";
    }

    /**
     * Delete staff account (only for DEALER role)
     */
    @PostMapping("/delete/{staffId}")
    public String deleteStaff(
            @PathVariable int staffId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        DTOAccount loggedInAccount = (DTOAccount) session.getAttribute("loggedInAccount");

        if (loggedInAccount == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Please login first!");
            return "redirect:/login";
        }

        // Only DEALER role can delete staff
        if (loggedInAccount.getRole() != Role.DEALER) {
            redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Only dealer owners can delete staff accounts!");
            return "redirect:/dealer/staff/list";
        }

        try {
            // Get dealer ID from logged-in account
            Integer dealerId = null;
            if (loggedInAccount.getDealerStaff() != null &&
                loggedInAccount.getDealerStaff().getDealer() != null) {
                dealerId = loggedInAccount.getDealerStaff().getDealer().getDealerID();
            }

            if (dealerId == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "⚠️ No dealer associated with your account!");
                return "redirect:/dealer/staff/list";
            }

            // Get all staff for this dealer to verify ownership
            List<DTODealerStaff> staffList = daoDealerStaff.getStaffsByDealerId(dealerId);
            DTODealerStaff staffToDelete = null;

            for (DTODealerStaff staff : staffList) {
                if (staff.getStaffID() == staffId) {
                    staffToDelete = staff;
                    break;
                }
            }

            if (staffToDelete == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Staff not found or does not belong to your dealer!");
                return "redirect:/dealer/staff/list";
            }

            // Delete using deleteDealerStaff method
            boolean deleted = daoDealerStaff.deleteDealerStaff(staffId);

            if (deleted) {
                redirectAttributes.addFlashAttribute("successMessage", "✅ Staff deleted successfully!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Failed to delete staff!");
            }

        } catch (Exception e) {
            e.printStackTrace();
            // Check if it's a foreign key constraint error
            if (e.getMessage() != null && (
                e.getMessage().contains("REFERENCE constraint") ||
                e.getMessage().contains("foreign key") ||
                e.getMessage().contains("SaleOrder") ||
                e.getMessage().contains("StaffID"))) {
                redirectAttributes.addFlashAttribute("errorMessage",
                    "⚠️ Cannot delete staff! This staff has related data (sale orders, customers, etc.). " +
                    "Please reassign or delete related data first.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Error: " + e.getMessage());
            }
        }

        return "redirect:/dealer/staff/list";
    }
}

