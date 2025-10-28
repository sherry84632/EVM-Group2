package com.dealermanagementsysstem.project.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility class to generate BCrypt password hashes
 * Run this to get the correct hash for "password123"
 */
public class PasswordHashGenerator {
    
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "password123";
        String hash = encoder.encode(password);
        
        System.out.println("Password: " + password);
        System.out.println("BCrypt Hash: " + hash);
        System.out.println("Verification: " + encoder.matches(password, hash));
        
        // Generate multiple hashes for testing
        System.out.println("\nMultiple hashes for the same password:");
        for (int i = 0; i < 3; i++) {
            System.out.println("Hash " + (i+1) + ": " + encoder.encode(password));
        }
    }
}
