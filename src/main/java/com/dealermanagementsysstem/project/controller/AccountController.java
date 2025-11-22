package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DAOAccount;
import com.dealermanagementsysstem.project.Model.DAODealerStaff;
import com.dealermanagementsysstem.project.Model.DAODealer;
import com.dealermanagementsysstem.project.Model.DTOAccount;
import com.dealermanagementsysstem.project.Model.DTODealerStaff;
import com.dealermanagementsysstem.project.Model.DTODealer;
import com.dealermanagementsysstem.project.Model.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/account")
public class AccountController {

    @Autowired
    private DAOAccount daoAccount;

    @Autowired
    private DAODealerStaff daoDealerStaff;

    @Autowired
    private DAODealer daoDealer;

    @Autowired
    private PasswordEncoder passwordEncoder;


    //  Redirect /account to /account/list
    @GetMapping({"", "/"})
    public String redirectToList() {
        return "redirect:/account/list";
    }

    //  List all accounts
    @GetMapping("/list")
    public String listAccounts(Model model) {
        List<DTOAccount> accounts = daoAccount.getAllAccounts();
        model.addAttribute("accounts", accounts);
        return "evmPage/accountList";
    }

    //  Show create account form - ADMIN only
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("account", new DTOAccount());

        // ADMIN can only create EVMSTAFF and DEALER roles
        // DEALERSTAFF is created by dealers themselves via /dealer/{id}/staff/create
        List<Role> allowedRoles = List.of(Role.ADMIN, Role.EVMSTAFF, Role.DEALER);
        model.addAttribute("roles", allowedRoles);

        // Load dealers for reference
        try {
            List<DTODealer> dealers = daoDealer.getAllDealers();
            model.addAttribute("dealers", dealers);
        } catch (Exception e) {
            System.out.println(" Could not load dealers: " + e.getMessage());
        }

        return "evmPage/accountCreate";
    }

    //  Save new account - ADMIN can create ADMIN, EVMSTAFF, and DEALER roles only
    @PostMapping("/save")
    public String saveAccount(
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("role") String roleStr,
            @RequestParam(value = "isActive", defaultValue = "false") boolean isActive,
            @RequestParam(value = "accountPhone", required = false) String accountPhone,
            @RequestParam(value = "selectedDealerId", required = false) Integer selectedDealerId,
            RedirectAttributes redirectAttributes,
            Model model) {

        // Validation
        if (username == null || username.trim().isEmpty()) {
            model.addAttribute("errorMessage", "Username is required!");
            model.addAttribute("roles", List.of(Role.ADMIN, Role.EVMSTAFF, Role.DEALER));
            loadDealersForForm(model);
            return "evmPage/accountCreate";
        }

        if (email == null || email.trim().isEmpty()) {
            model.addAttribute("errorMessage", "Email is required!");
            model.addAttribute("roles", List.of(Role.ADMIN, Role.EVMSTAFF, Role.DEALER));
            loadDealersForForm(model);
            return "evmPage/accountCreate";
        }

        if (!isValidEmail(email)) {
            model.addAttribute("errorMessage", "Email format is invalid!");
            model.addAttribute("roles", List.of(Role.ADMIN, Role.EVMSTAFF, Role.DEALER));
            loadDealersForForm(model);
            return "evmPage/accountCreate";
        }

        if (password == null || password.trim().isEmpty()) {
            model.addAttribute("errorMessage", "Password is required!");
            model.addAttribute("roles", List.of(Role.ADMIN, Role.EVMSTAFF, Role.DEALER));
            loadDealersForForm(model);
            return "evmPage/accountCreate";
        }

        if (daoAccount.emailExists(email, null)) {
            model.addAttribute("errorMessage", "Email already exists!");
            model.addAttribute("roles", List.of(Role.ADMIN, Role.EVMSTAFF, Role.DEALER));
            loadDealersForForm(model);
            return "evmPage/accountCreate";
        }

        Role role = Role.valueOf(roleStr);

        if (role == Role.DEALERSTAFF) {
            model.addAttribute("errorMessage", "DEALERSTAFF accounts must be created through Dealer Staff Management!");
            model.addAttribute("roles", List.of(Role.ADMIN, Role.EVMSTAFF, Role.DEALER));
            loadDealersForForm(model);
            return "evmPage/accountCreate";
        }

        if (role == Role.DEALER) {
            if (selectedDealerId == null || selectedDealerId <= 0) {
                model.addAttribute("errorMessage", "Please select a dealer company!");
                model.addAttribute("roles", List.of(Role.ADMIN, Role.EVMSTAFF, Role.DEALER));
                loadDealersForForm(model);
                return "evmPage/accountCreate";
            }

            try {
                DTODealer existingDealer = daoDealer.getDealerById(selectedDealerId);
                if (existingDealer == null) {
                    model.addAttribute("errorMessage", "Selected dealer not found!");
                    model.addAttribute("roles", List.of(Role.ADMIN, Role.EVMSTAFF, Role.DEALER));
                    loadDealersForForm(model);
                    return "evmPage/accountCreate";
                }
            } catch (Exception e) {
                model.addAttribute("errorMessage", "Error validating dealer: " + e.getMessage());
                model.addAttribute("roles", List.of(Role.ADMIN, Role.EVMSTAFF, Role.DEALER));
                loadDealersForForm(model);
                return "evmPage/accountCreate";
            }
        }

        try {
            DTOAccount account = new DTOAccount();
            account.setUsername(username.trim());
            account.setEmail(email.trim());
            account.setPassword(passwordEncoder.encode(password));
            account.setRole(role);
            account.setPhone(accountPhone != null ? accountPhone.trim() : null);
            account.setActive(isActive);

            int accountId = daoAccount.insertAccount(account);

            if (accountId <= 0) {
                model.addAttribute("errorMessage", "Failed to create account!");
                model.addAttribute("roles", List.of(Role.ADMIN, Role.EVMSTAFF, Role.DEALER));
                loadDealersForForm(model);
                return "evmPage/accountCreate";
            }

            if (role == Role.DEALER) {
                DTODealerStaff ownerStaff = new DTODealerStaff();
                ownerStaff.setFullName(username.trim() + " (Owner)");
                ownerStaff.setPosition("Owner");
                ownerStaff.setPhone("");
                ownerStaff.setEmail(email.trim());

                DTOAccount accountRef = new DTOAccount();
                accountRef.setAccountId(accountId);
                ownerStaff.setAccount(accountRef);

                DTODealer dealerRef = new DTODealer();
                dealerRef.setDealerID(selectedDealerId);
                ownerStaff.setDealer(dealerRef);

                int staffId = daoDealerStaff.insertDealerStaff(ownerStaff);

                if (staffId <= 0) {
                    daoAccount.deleteAccount(accountId);
                    model.addAttribute("errorMessage", "Failed to create dealer staff record!");
                    model.addAttribute("roles", List.of(Role.ADMIN, Role.EVMSTAFF, Role.DEALER));
                    loadDealersForForm(model);
                    return "evmPage/accountCreate";
                }

                redirectAttributes.addFlashAttribute("successMessage",
                    "Dealer Account created successfully! (Account ID: " + accountId + ", Dealer ID: " + selectedDealerId + ")");
            } else {
                redirectAttributes.addFlashAttribute("successMessage",
                    "Account created successfully! (ID: " + accountId + ")");
            }

            return "redirect:/account/list";

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error: " + e.getMessage());
            model.addAttribute("roles", List.of(Role.ADMIN, Role.EVMSTAFF, Role.DEALER));
            loadDealersForForm(model);
            return "evmPage/accountCreate";
        }
    }

    //  Show edit account form
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") int id, Model model, RedirectAttributes redirectAttributes) {
        DTOAccount account = daoAccount.getAccountById(id);

        if (account == null) {
            redirectAttributes.addFlashAttribute("errorMessage", " Account not found!");
            return "redirect:/account/list";
        }

        model.addAttribute("account", account);
        model.addAttribute("roles", Role.values());

        // Load dealers for DEALERSTAFF role
        loadDealersForForm(model);

        return "evmPage/accountEdit";
    }

    //  Update account
    @PostMapping("/update")
    public String updateAccount(
            @RequestParam("accountId") int accountId,
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("role") String roleStr,
            @RequestParam(value = "isActive", defaultValue = "false") boolean isActive,
            @RequestParam(value = "newPassword", required = false) String newPassword,
            @RequestParam(value = "accountPhone", required = false) String accountPhone,
            // DealerStaff fields (only for DEALERSTAFF role)
            @RequestParam(value = "staffFullName", required = false) String staffFullName,
            @RequestParam(value = "staffPosition", required = false) String staffPosition,
            @RequestParam(value = "staffPhone", required = false) String staffPhone,
            @RequestParam(value = "staffEmail", required = false) String staffEmail,
            @RequestParam(value = "dealerId", required = false) Integer dealerId,
            RedirectAttributes redirectAttributes,
            Model model) {

        DTOAccount account = daoAccount.getAccountById(accountId);
        if (account == null) {
            redirectAttributes.addFlashAttribute("errorMessage", " Account not found!");
            return "redirect:/account/list";
        }

        // Validation
        if (username == null || username.trim().isEmpty()) {
            model.addAttribute("errorMessage", "Username is required!");
            model.addAttribute("account", account);
            model.addAttribute("roles", Role.values());
            loadDealersForForm(model);
            return "evmPage/accountEdit";
        }

        if (email == null || email.trim().isEmpty()) {
            model.addAttribute("errorMessage", " Email is required!");
            model.addAttribute("account", account);
            model.addAttribute("roles", Role.values());
            loadDealersForForm(model);
            return "evmPage/accountEdit";
        }

        // Validate email format
        if (!isValidEmail(email)) {
            model.addAttribute("errorMessage", " Email format is invalid! Please use format: example@domain.com");
            model.addAttribute("account", account);
            model.addAttribute("roles", Role.values());
            loadDealersForForm(model);
            return "evmPage/accountEdit";
        }

        // Check if email already exists (excluding current account)
        if (daoAccount.emailExists(email, accountId)) {
            model.addAttribute("errorMessage", " Email already exists!");
            model.addAttribute("account", account);
            model.addAttribute("roles", Role.values());
            loadDealersForForm(model);
            return "evmPage/accountEdit";
        }

        // Additional validation for DEALERSTAFF role
        Role newRole = Role.valueOf(roleStr);
        if (newRole == Role.DEALERSTAFF) {
            if (staffFullName == null || staffFullName.trim().isEmpty()) {
                model.addAttribute("errorMessage", "Full Name is required for Dealer Staff!");
                model.addAttribute("account", account);
                model.addAttribute("roles", Role.values());
                loadDealersForForm(model);
                return "evmPage/accountEdit";
            }
            if (staffPhone == null || staffPhone.trim().isEmpty()) {
                model.addAttribute("errorMessage", "Phone is required for Dealer Staff!");
                model.addAttribute("account", account);
                model.addAttribute("roles", Role.values());
                loadDealersForForm(model);
                return "evmPage/accountEdit";
            }
            if (dealerId == null || dealerId <= 0) {
                model.addAttribute("errorMessage", "Please select a Dealer for Dealer Staff!");
                model.addAttribute("account", account);
                model.addAttribute("roles", Role.values());
                loadDealersForForm(model);
                return "evmPage/accountEdit";
            }
        }

        try {
            account.setUsername(username.trim());
            account.setPhone(accountPhone != null ? accountPhone.trim() : null);
            account.setEmail(email.trim());

            Role oldRole = account.getRole();
            account.setRole(newRole);
            account.setActive(isActive);

            // Update password if provided
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                String hashedPassword = passwordEncoder.encode(newPassword);
                daoAccount.updatePassword(accountId, hashedPassword);
            }

            boolean success = daoAccount.updateAccount(account);

            if (!success) {
                model.addAttribute("errorMessage", " Failed to update account!");
                model.addAttribute("account", account);
                model.addAttribute("roles", Role.values());
                loadDealersForForm(model);
                return "evmPage/accountEdit";
            }

            // Handle DealerStaff changes
            DTODealerStaff existingStaff = daoDealerStaff.getDealerStaffByAccountId(accountId);

            if (newRole == Role.DEALERSTAFF) {
                // Need DealerStaff record
                if (existingStaff != null) {
                    // Update existing DealerStaff
                    existingStaff.setFullName(staffFullName.trim());
                    existingStaff.setPosition(staffPosition); // DAO will auto-fill "Sales" if null/empty
                    existingStaff.setPhone(staffPhone != null ? staffPhone.trim() : "");
                    existingStaff.setEmail(staffEmail != null ? staffEmail.trim() : email.trim());

                    DTODealer dealerRef = new DTODealer();
                    dealerRef.setDealerID(dealerId);
                    existingStaff.setDealer(dealerRef);

                    daoDealerStaff.updateDealerStaff(existingStaff);
                } else {
                    // Create new DealerStaff
                    DTODealerStaff staff = new DTODealerStaff();
                    staff.setFullName(staffFullName.trim());
                    staff.setPosition(staffPosition); // DAO will auto-fill "Sales" if null/empty
                    staff.setPhone(staffPhone != null ? staffPhone.trim() : "");
                    staff.setEmail(staffEmail != null ? staffEmail.trim() : email.trim());

                    DTOAccount accountRef = new DTOAccount();
                    accountRef.setAccountId(accountId);
                    staff.setAccount(accountRef);

                    DTODealer dealerRef = new DTODealer();
                    dealerRef.setDealerID(dealerId);
                    staff.setDealer(dealerRef);

                    daoDealerStaff.insertDealerStaff(staff);
                }
            } else if (oldRole == Role.DEALERSTAFF && newRole != Role.DEALERSTAFF) {
                // Role changed from DEALERSTAFF to something else - delete DealerStaff record
                if (existingStaff != null) {
                    daoDealerStaff.deleteDealerStaffByAccountId(accountId);
                }
            }

            redirectAttributes.addFlashAttribute("successMessage", " Account updated successfully!");
            return "redirect:/account/list";

        } catch (Exception e) {
            System.out.println(" Error updating account: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMessage", " Error: " + e.getMessage());
            model.addAttribute("account", account);
            model.addAttribute("roles", Role.values());
            loadDealersForForm(model);
            return "evmPage/accountEdit";
        }
    }

    //  Delete account
    @PostMapping("/delete/{id}")
    public String deleteAccount(@PathVariable("id") int id, RedirectAttributes redirectAttributes) {
        try {
            boolean success = daoAccount.deleteAccount(id);

            if (success) {
                redirectAttributes.addFlashAttribute("successMessage", "Account deleted successfully!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete account!");
            }
        } catch (Exception e) {
            // Check if it's a foreign key constraint error
            if (e.getMessage() != null && (
                e.getMessage().contains("REFERENCE constraint") ||
                e.getMessage().contains("foreign key") ||
                e.getMessage().contains("SaleOrder") ||
                e.getMessage().contains("StaffID") ||
                e.getMessage().contains("DealerStaff"))) {
                redirectAttributes.addFlashAttribute("errorMessage",
                    "Cannot delete account! This account has related data (sale orders, staff records, etc.). " +
                    "Please remove or reassign related data first.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Error deleting account: " + e.getMessage());
            }
        }

        return "redirect:/account/list";
    }

    // Search accounts
    @GetMapping("/search")
    public String searchAccounts(
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            Model model) {

        List<DTOAccount> accounts;

        if (keyword == null || keyword.trim().isEmpty()) {
            accounts = daoAccount.getAllAccounts();
            System.out.println("ℹ Search with empty keyword → Returning all accounts (" + accounts.size() + " found)");
        } else {
            accounts = daoAccount.searchAccounts(keyword.trim());
            System.out.println(" Search for: '" + keyword + "' → Found " + accounts.size() + " accounts");
        }

        model.addAttribute("accounts", accounts);
        model.addAttribute("keyword", keyword);
        return "evmPage/accountList";
    }

    //  View account details
    @GetMapping("/detail/{id}")
    public String showAccountDetail(@PathVariable("id") int id, Model model, RedirectAttributes redirectAttributes) {
        DTOAccount account = daoAccount.getAccountById(id);

        if (account == null) {
            redirectAttributes.addFlashAttribute("errorMessage", " Account not found!");
            return "redirect:/account/list";
        }

        model.addAttribute("account", account);
        return "evmPage/accountDetail";
    }

    // ===========================
    // HELPER METHOD - Email Validation
    // ===========================

    /**
     * Validate email format using regex
     * @param email Email to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        // RFC 5322 compliant email regex (simplified)
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email.matches(emailRegex);
    }

    /**
     * Helper method to load dealers into model
     */
    private void loadDealersForForm(Model model) {
        try {
            // Load dealers for DEALERSTAFF role
            List<DTODealer> dealers = daoDealer.getAllDealers();
            model.addAttribute("dealers", dealers);

        } catch (Exception e) {
            System.out.println(" Could not load dealers: " + e.getMessage());
        }
    }
}

