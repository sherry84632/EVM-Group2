package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DAOAccount;
import com.dealermanagementsysstem.project.Model.DTOAccount;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    private final DAOAccount daoAccount = new DAOAccount();

    // Hiển thị trang login
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

        return "mainPage/loginPageV2";
    }

    // ✅ Default success handler - redirect based on user role
    @GetMapping("/success")
    public String defaultSuccessHandler(HttpSession session) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            String email = auth.getName(); // Email người đăng nhập
            DTOAccount account = daoAccount.findAccountByEmail(email);

            if (account != null) {
                // ✅ Lưu account vào session
                session.setAttribute("loggedInAccount", account);
                
                // ✅ Safe logging with null checks
                if (account.getDealerStaff() != null) {
                    System.out.println("✅ [LOGIN SUCCESS] " + email + " (StaffID=" + account.getDealerStaff().getStaffID() + ")");
                } else {
                    System.out.println("✅ [LOGIN SUCCESS] " + email + " (Role=" + account.getRole() + ")");
                }
            } else {
                System.out.println("⚠️ Không tìm thấy account cho email: " + email);
            }

            String role = auth.getAuthorities().iterator().next().getAuthority();
            return switch (role) {
                case "ROLE_ADMIN", "ROLE_EVM", "ROLE_EVMSTAFF" -> "redirect:/showEVMHomePage";
                case "ROLE_DEALER", "ROLE_DEALERSTAFF" -> "redirect:/showDealerHomePage";
                default -> "redirect:/login?error=role";
            };
        }

        return "redirect:/loginPageV2";
    }
}
