package com.dealermanagementsysstem.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class LoginController {

    @PostMapping("/login")
    public String handleLogin(@RequestParam String email,
                              @RequestParam String password) {

        // Just for example:
        System.out.println("Email: " + email);
        System.out.println("Password: " + password);

        // You can now validate the credentials
        if (email.equals("admin@example.com") && password.equals("1234")) {
            // Redirect to home page or dashboard
            return "loginSuccess";
        } else {
            // Return to login page with error
            return "loginFailed"; // maps to loginFailed.html
        }
        
    }

}
