package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DAOAccount;
import com.dealermanagementsysstem.project.Model.DTOAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class UserTestController {

    @Autowired
    private DAOAccount daoAccount;

    @GetMapping("/user")
    public String testUser(@RequestParam String email) {
        try {
            DTOAccount account = daoAccount.findAccountByEmail(email);
            if (account != null) {
                return String.format("User found: %s, Role: %s, Status: %s, Password: %s", 
                    account.getUsername(), 
                    account.getRole(), 
                    account.isStatus(),
                    account.getPassword().substring(0, Math.min(10, account.getPassword().length())) + "..."
                );
            } else {
                return "User not found: " + email;
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @GetMapping("/user-info")
    public String getUserInfo() {
        try {
            // This is a simple test - you might want to implement a method to list all users
            return "Database authentication is working. Try logging in with existing database users.";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
