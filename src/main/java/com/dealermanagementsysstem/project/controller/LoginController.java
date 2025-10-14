package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DAOAccount;
import com.dealermanagementsysstem.project.Model.DTOAccount;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    // Hiển thị trang login (khi vào /login hoặc khi redirect có lỗi)
    @GetMapping("/login")
    public String showLoginPage(
            @RequestParam(value = "error", required = false) String error,
            Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Email hoặc mật khẩu không đúng!");
        }
        return "mainPage/loginPage";
    }

    // Xử lý khi nhấn nút Login
    @PostMapping("/login")
    public String handleLogin(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            HttpSession session,
            Model model) {

        DAOAccount dao = new DAOAccount();
        DTOAccount account = dao.checkLogin(email, password);

        if (account != null) {
            // Nếu đăng nhập đúng → lưu thông tin user
            session.setAttribute("user", account);
//                        String role = account.getRole();
//            return switch (role) {
//                case "Admin" -> "redirect:/admin/dashboard";
//                case "EVM" -> "redirect:/evm/dashboard";
//                case "DealerStaff" -> "redirect:/dealerstaff/dashboard";
//                case "Dealer" -> "redirect:/dealer/dashboard";
//                case "EVMStaff" -> "redirect:/evmstaff/dashboard";
//                default -> "redirect:/login?error=role";
//            };
            return "evmPage/homePage"; // chuyển đến HomeController
        } else {
            // Nếu sai → quay lại login và thêm lỗi
            return "redirect:/login?error=1";
        }
    }
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // xóa session user
        return "mainPage/loginPage"; // quay lại trang login
    }
}
