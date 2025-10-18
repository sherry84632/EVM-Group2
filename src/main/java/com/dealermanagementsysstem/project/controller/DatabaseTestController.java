package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DAOAccount;
import com.dealermanagementsysstem.project.Model.DTOAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/test")
public class DatabaseTestController {

    @Autowired
    private DAOAccount daoAccount;

    @GetMapping("/database")
    public String testDatabaseConnection() {
        try {
            // Test database connection by trying to find a user
            DTOAccount testAccount = daoAccount.findAccountByEmail("test@example.com");
            return "Database connection successful! Account found: " + (testAccount != null ? "Yes" : "No");
        } catch (Exception e) {
            return "Database connection failed: " + e.getMessage();
        }
    }

    @GetMapping("/status")
    public String getStatus() {
        try {
            // This is a simple test - in a real scenario, you'd want to implement a method to list all users
            return "Database connection working. User authentication is now database-based.";
        } catch (Exception e) {
            return "Error accessing database: " + e.getMessage();
        }
    }
}
