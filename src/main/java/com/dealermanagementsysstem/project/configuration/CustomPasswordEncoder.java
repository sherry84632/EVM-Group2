package com.dealermanagementsysstem.project.configuration;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Custom password encoder that handles both plain text and BCrypt passwords
 * This allows for migration from plain text to BCrypt hashed passwords
 */
public class CustomPasswordEncoder implements PasswordEncoder {

    private final BCryptPasswordEncoder bcryptEncoder = new BCryptPasswordEncoder();

    @Override
    public String encode(CharSequence rawPassword) {
        // Always encode new passwords with BCrypt
        return bcryptEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        // Check if the stored password is BCrypt encoded
        if (isBCryptEncoded(encodedPassword)) {
            // Use BCrypt verification for hashed passwords
            return bcryptEncoder.matches(rawPassword, encodedPassword);
        } else {
            // Use plain text comparison for legacy passwords
            return rawPassword.toString().equals(encodedPassword);
        }
    }

    /**
     * Check if a password is BCrypt encoded
     */
    private boolean isBCryptEncoded(String password) {
        return password != null && 
               password.startsWith("$2a$") || 
               password.startsWith("$2b$") || 
               password.startsWith("$2y$");
    }
}
