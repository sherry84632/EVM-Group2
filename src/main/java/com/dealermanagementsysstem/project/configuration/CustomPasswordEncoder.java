package com.dealermanagementsysstem.project.configuration;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Custom password encoder that handles both plain text and BCrypt passwords.
 * Allows gradual migration from legacy plain text passwords to BCrypt.
 */
public class CustomPasswordEncoder implements PasswordEncoder {

    private final BCryptPasswordEncoder bcryptEncoder = new BCryptPasswordEncoder();
    private static final String PREFIX_2A = "$2a$";
    private static final String PREFIX_2B = "$2b$";
    private static final String PREFIX_2Y = "$2y$";

    @Override
    public String encode(CharSequence rawPassword) {
        // Always encode new passwords with BCrypt
        return bcryptEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (isBCryptEncoded(encodedPassword)) {
            return bcryptEncoder.matches(rawPassword, encodedPassword);
        }
        // Fallback plain text comparison (legacy). Consider removing once all hashes migrated.
        return rawPassword != null && encodedPassword != null && rawPassword.toString().equals(encodedPassword);
    }

    /**
     * Check if the stored password looks like a BCrypt hash.
     */
    private boolean isBCryptEncoded(String password) {
        return password != null && (
                password.startsWith(PREFIX_2A) ||
                password.startsWith(PREFIX_2B) ||
                password.startsWith(PREFIX_2Y)
        );
    }
}
