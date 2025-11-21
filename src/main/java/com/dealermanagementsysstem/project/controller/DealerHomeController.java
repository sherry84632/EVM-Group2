package com.dealermanagementsysstem.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Controller
public class DealerHomeController {

    @GetMapping("/showDealerHomePage")
    public String dealerHome(Model model){
        model.addAttribute("activeMenu","home");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth!=null){ model.addAttribute("currentUserEmail", auth.getName()); }
        return "dealerPage/DealerHomePage";
    }
}

