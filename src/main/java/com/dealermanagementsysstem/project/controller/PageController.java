package com.dealermanagementsysstem.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

public class PageController {

    @GetMapping("/{folder}/{page}")
    public String showPage(@PathVariable String folder, @PathVariable String page) {
        return folder + "/" + page;
        // e.g. /mainPage/loginPage → templates/mainPage/loginPage.html
    }

}
