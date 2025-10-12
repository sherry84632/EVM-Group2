package com.dealermanagementsysstem.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/home")
    public String showHomePage() {
        return "mainPage/homePage"; // Gọi tới templates/mainPage/homePage.html
    }
}
