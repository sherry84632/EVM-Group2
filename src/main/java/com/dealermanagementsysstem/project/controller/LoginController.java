package com.dealermanagementsysstem.project.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    // Hiển thị trang login (khi vào /login hoặc khi redirect có lỗi)
    @GetMapping("/login")
    public String showLoginPage(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "access_denied", required = false) String accessDenied,
            Model model) {
        
        if (error != null) {
            if ("true".equals(error)) {
                model.addAttribute("errorMessage", "Email hoặc mật khẩu không đúng!");
            } else if ("access_denied".equals(error)) {
                model.addAttribute("errorMessage", "Bạn không có quyền truy cập trang này!");
            } else {
                model.addAttribute("errorMessage", "Đăng nhập thất bại. Vui lòng thử lại!");
            }
        }
        
        if (logout != null) {
            model.addAttribute("successMessage", "Đăng xuất thành công!");
        }
        
        return "mainPage/loginPage";
    }

    // Default success handler - redirect based on user role
    @GetMapping("/success")
    public String defaultSuccessHandler() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            String role = auth.getAuthorities().iterator().next().getAuthority();
            
            return switch (role) {
                case "ROLE_ADMIN", "ROLE_EVM", "ROLE_EVMSTAFF" -> "redirect:/showEVMHomePage";
                case "ROLE_DEALER", "ROLE_DEALERSTAFF" -> "redirect:/showDealerHomePage";
                default -> "redirect:/login?error=role";
            };
        }
        
        return "redirect:/login";
    }
}
