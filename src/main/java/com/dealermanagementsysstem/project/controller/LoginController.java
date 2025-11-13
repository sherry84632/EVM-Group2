package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DAOAccount;
import com.dealermanagementsysstem.project.Model.DTOAccount;
import com.dealermanagementsysstem.project.util.SecurityUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    private final DAOAccount daoAccount;

    public LoginController(DAOAccount daoAccount) {
        this.daoAccount = daoAccount;
    }

    // Hiển thị trang login
    @GetMapping("/login")
    public String showLoginPage(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {

        if (error != null) {
            model.addAttribute("errorMessage", switch (error) {
                case "true" -> "Email hoặc mật khẩu không đúng!";
                case "access_denied" -> "Bạn không có quyền truy cập trang này!";
                default -> "Đăng nhập thất bại. Vui lòng thử lại!";
            });
        }

        if (logout != null) {
            model.addAttribute("successMessage", "Đăng xuất thành công!");
        }

        return "mainPage/loginPageV2";
    }

    // Default success handler - redirect based on user role
    @GetMapping("/success")
    public String defaultSuccessHandler(HttpSession session, Model model) {
        String email = SecurityUtil.getCurrentUserEmail();
        if (email == null) {
            return "redirect:/login";
        }

        DTOAccount account = daoAccount.findAccountByEmail(email);
        if (account == null) {
            model.addAttribute("errorMessage", "Không tìm thấy tài khoản cho email: " + email);
            return "redirect:/login";
        }

        // Lưu account vào session
        session.setAttribute("loggedInAccount", account);

        // Logging đơn giản (có thể thay bằng logger)
        if (account.getDealerStaff() != null) {
            System.out.printf(" [LOGIN SUCCESS] %s (StaffID=%d)%n", email, account.getDealerStaff().getStaffID());
        } else {
            System.out.printf(" [LOGIN SUCCESS] %s (Role=%s)%n", email, account.getRole());
        }

        // Determine redirect based on role
        Authentication auth = SecurityUtil.isAuthenticated() ?
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() : null;

        if (auth == null || auth.getAuthorities().isEmpty()) {
            return "redirect:/login?error=role";
        }
        String role = auth.getAuthorities().iterator().next().getAuthority();
        return switch (role) {
            case "ROLE_ADMIN", "ROLE_EVMSTAFF" -> "redirect:/showEVMHomePage";
            case "ROLE_DEALER", "ROLE_DEALERSTAFF" -> "redirect:/showDealerHomePage";
            default -> "redirect:/login?error=role";
        };
    }
}
