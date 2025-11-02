package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DAOAccount;
import com.dealermanagementsysstem.project.Model.DTOAccount;
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
    private PasswordEncoder passwordEncoder;

    // ✅ Redirect /account to /account/list
    @GetMapping({"", "/"})
    public String redirectToList() {
        return "redirect:/account/list";
    }

    // ✅ List all accounts
    @GetMapping("/list")
    public String listAccounts(Model model) {
        List<DTOAccount> accounts = daoAccount.getAllAccounts();
        model.addAttribute("accounts", accounts);
        return "evmPage/accountList";
    }

    // ✅ Show create account form
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("account", new DTOAccount());
        model.addAttribute("roles", Role.values());
        return "evmPage/accountCreate";
    }

    // ✅ Save new account
    @PostMapping("/save")
    public String saveAccount(
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("role") String roleStr,
            @RequestParam(value = "isActive", defaultValue = "false") boolean isActive,
            RedirectAttributes redirectAttributes,
            Model model) {

        // Validation
        if (username == null || username.trim().isEmpty()) {
            model.addAttribute("errorMessage", "❌ Username is required!");
            model.addAttribute("roles", Role.values());
            return "evmPage/accountCreate";
        }

        if (email == null || email.trim().isEmpty()) {
            model.addAttribute("errorMessage", "❌ Email is required!");
            model.addAttribute("roles", Role.values());
            return "evmPage/accountCreate";
        }

        // Validate email format
        if (!isValidEmail(email)) {
            model.addAttribute("errorMessage", "❌ Email format is invalid! Please use format: example@domain.com");
            model.addAttribute("roles", Role.values());
            return "evmPage/accountCreate";
        }

        if (password == null || password.trim().isEmpty()) {
            model.addAttribute("errorMessage", "❌ Password is required!");
            model.addAttribute("roles", Role.values());
            return "evmPage/accountCreate";
        }

        // Check if email already exists
        if (daoAccount.emailExists(email, null)) {
            model.addAttribute("errorMessage", "❌ Email already exists!");
            model.addAttribute("roles", Role.values());
            return "evmPage/accountCreate";
        }

        try {
            DTOAccount account = new DTOAccount();
            account.setUsername(username.trim());
            account.setEmail(email.trim());

            // Hash password using BCrypt
            String hashedPassword = passwordEncoder.encode(password);
            account.setPassword(hashedPassword);

            account.setRole(Role.valueOf(roleStr));
            account.setActive(isActive);

            int accountId = daoAccount.insertAccount(account);

            if (accountId > 0) {
                redirectAttributes.addFlashAttribute("successMessage",
                    "✅ Account created successfully! (ID: " + accountId + ")");
                return "redirect:/account/list";
            } else {
                model.addAttribute("errorMessage", "❌ Failed to create account!");
                model.addAttribute("roles", Role.values());
                return "evmPage/accountCreate";
            }

        } catch (Exception e) {
            System.out.println("❌ Error creating account: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMessage", "❌ Error: " + e.getMessage());
            model.addAttribute("roles", Role.values());
            return "evmPage/accountCreate";
        }
    }

    // ✅ Show edit account form
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") int id, Model model, RedirectAttributes redirectAttributes) {
        DTOAccount account = daoAccount.getAccountById(id);

        if (account == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Account not found!");
            return "redirect:/account/list";
        }

        model.addAttribute("account", account);
        model.addAttribute("roles", Role.values());
        return "evmPage/accountEdit";
    }

    // ✅ Update account
    @PostMapping("/update")
    public String updateAccount(
            @RequestParam("accountId") int accountId,
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("role") String roleStr,
            @RequestParam(value = "isActive", defaultValue = "false") boolean isActive,
            @RequestParam(value = "newPassword", required = false) String newPassword,
            RedirectAttributes redirectAttributes,
            Model model) {

        DTOAccount account = daoAccount.getAccountById(accountId);
        if (account == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Account not found!");
            return "redirect:/account/list";
        }

        // Validation
        if (username == null || username.trim().isEmpty()) {
            model.addAttribute("errorMessage", "❌ Username is required!");
            model.addAttribute("account", account);
            model.addAttribute("roles", Role.values());
            return "evmPage/accountEdit";
        }

        if (email == null || email.trim().isEmpty()) {
            model.addAttribute("errorMessage", "❌ Email is required!");
            model.addAttribute("account", account);
            model.addAttribute("roles", Role.values());
            return "evmPage/accountEdit";
        }

        // Validate email format
        if (!isValidEmail(email)) {
            model.addAttribute("errorMessage", "❌ Email format is invalid! Please use format: example@domain.com");
            model.addAttribute("account", account);
            model.addAttribute("roles", Role.values());
            return "evmPage/accountEdit";
        }

        // Check if email already exists (excluding current account)
        if (daoAccount.emailExists(email, accountId)) {
            model.addAttribute("errorMessage", "❌ Email already exists!");
            model.addAttribute("account", account);
            model.addAttribute("roles", Role.values());
            return "evmPage/accountEdit";
        }

        try {
            account.setUsername(username.trim());
            account.setEmail(email.trim());
            account.setRole(Role.valueOf(roleStr));
            account.setActive(isActive);

            // Update password if provided
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                String hashedPassword = passwordEncoder.encode(newPassword);
                daoAccount.updatePassword(accountId, hashedPassword);
            }

            boolean success = daoAccount.updateAccount(account);

            if (success) {
                redirectAttributes.addFlashAttribute("successMessage", "✅ Account updated successfully!");
                return "redirect:/account/list";
            } else {
                model.addAttribute("errorMessage", "❌ Failed to update account!");
                model.addAttribute("account", account);
                model.addAttribute("roles", Role.values());
                return "evmPage/accountEdit";
            }

        } catch (Exception e) {
            System.out.println("❌ Error updating account: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMessage", "❌ Error: " + e.getMessage());
            model.addAttribute("account", account);
            model.addAttribute("roles", Role.values());
            return "evmPage/accountEdit";
        }
    }

    // ✅ Delete account
    @PostMapping("/delete/{id}")
    public String deleteAccount(@PathVariable("id") int id, RedirectAttributes redirectAttributes) {
        boolean success = daoAccount.deleteAccount(id);

        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "🗑️ Account deleted successfully!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Failed to delete account!");
        }

        return "redirect:/account/list";
    }

    // ✅ Search accounts
    @GetMapping("/search")
    public String searchAccounts(
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            Model model) {

        List<DTOAccount> accounts;

        if (keyword == null || keyword.trim().isEmpty()) {
            accounts = daoAccount.getAllAccounts();
            System.out.println("ℹ️ Search with empty keyword → Returning all accounts (" + accounts.size() + " found)");
        } else {
            accounts = daoAccount.searchAccounts(keyword.trim());
            System.out.println("🔍 Search for: '" + keyword + "' → Found " + accounts.size() + " accounts");
        }

        model.addAttribute("accounts", accounts);
        model.addAttribute("keyword", keyword);
        return "evmPage/accountList";
    }

    // ✅ View account details
    @GetMapping("/detail/{id}")
    public String showAccountDetail(@PathVariable("id") int id, Model model, RedirectAttributes redirectAttributes) {
        DTOAccount account = daoAccount.getAccountById(id);

        if (account == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Account not found!");
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
}

