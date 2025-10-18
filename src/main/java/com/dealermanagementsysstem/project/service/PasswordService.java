package com.dealermanagementsysstem.project.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Hash a plain text password using BCrypt
     */
    public String hashPassword(String plainPassword) {
        return passwordEncoder.encode(plainPassword);
    }

    /**
     * Verify a plain text password against a hashed password
     */
    public boolean verifyPassword(String plainPassword, String hashedPassword) {
        return passwordEncoder.matches(plainPassword, hashedPassword);
    }

    /**
     * Check if a password needs to be updated (for migration from plain text to hashed)
     */
    public boolean needsPasswordUpdate(String storedPassword) {
        // If password doesn't start with $2a$ or $2b$, it's likely plain text
        return !storedPassword.startsWith("$2a$") && !storedPassword.startsWith("$2b$");
    }
}
