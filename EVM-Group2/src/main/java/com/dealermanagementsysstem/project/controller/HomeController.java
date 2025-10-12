package com.dealermanagementsysstem.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String createAccountPage(Model model) {
        return "mainPage/homePage";
    }
    
}
