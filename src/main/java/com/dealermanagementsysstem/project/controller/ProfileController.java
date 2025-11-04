package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DAOAccount;
import com.dealermanagementsysstem.project.Model.DTOAccount;
import com.dealermanagementsysstem.project.util.SecurityUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProfileController {

    private final DAOAccount daoAccount;

    public ProfileController(DAOAccount daoAccount) {
        this.daoAccount = daoAccount;
    }

    @GetMapping("/profile")
    public String showProfile(Model model) {
        String email = SecurityUtil.getCurrentUserEmail();
        if (email == null) {
            model.addAttribute("error", "You must be logged in to view your profile.");
            return "mainPage/profile";
        }

        DTOAccount account = daoAccount.findAccountByEmail(email);
        if (account == null) {
            model.addAttribute("error", "Account not found.");
            return "mainPage/profile";
        }

        model.addAttribute("account", account);
        return "mainPage/profile";
    }
}
