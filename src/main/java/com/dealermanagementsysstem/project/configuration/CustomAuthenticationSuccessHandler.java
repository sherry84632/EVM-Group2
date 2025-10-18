package com.dealermanagementsysstem.project.configuration;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        
        switch (role) {
            case "ROLE_ADMIN":
            case "ROLE_EVM":
            case "ROLE_EVMSTAFF":
                response.sendRedirect("/showEVMHomePage");
                break;
            case "ROLE_DEALER":
            case "ROLE_DEALERSTAFF":
                response.sendRedirect("/showDealerHomePage");
                break;
            default:
                response.sendRedirect("/login?error=role");
                break;
        }
    }
}
