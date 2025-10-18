package com.dealermanagementsysstem.project.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

    /**
     * Get the current authenticated user's email
     */
    public static String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        return null;
    }

    /**
     * Get the current authenticated user's role
     */
    public static String getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return auth.getAuthorities().iterator().next().getAuthority();
        }
        return null;
    }

    /**
     * Check if current user has a specific role
     */
    public static boolean hasRole(String role) {
        String currentRole = getCurrentUserRole();
        return currentRole != null && currentRole.equals("ROLE_" + role.toUpperCase());
    }

    /**
     * Check if current user has any of the specified roles
     */
    public static boolean hasAnyRole(String... roles) {
        String currentRole = getCurrentUserRole();
        if (currentRole == null) return false;
        
        for (String role : roles) {
            if (currentRole.equals("ROLE_" + role.toUpperCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if user is authenticated
     */
    public static boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName());
    }
}
