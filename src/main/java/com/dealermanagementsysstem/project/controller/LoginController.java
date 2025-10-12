package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DAOAccount;
import com.dealermanagementsysstem.project.Model.DTOAccount;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    // Hiển thị trang login
    @GetMapping("/login")
    public String showLoginPage() {
        return "mainPage/loginPage"; // Gọi tới templates/mainPage/loginPage.html
    }

    // Xử lý khi nhấn nút Login
    @PostMapping("/login")
    public String handleLogin(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            HttpSession session) {

        DAOAccount dao = new DAOAccount();
        DTOAccount account = dao.checkLogin(email, password);

        if (account != null) {
            // Nếu đăng nhập đúng → lưu thông tin user
            session.setAttribute("user", account);
            return "redirect:/home"; // chuyển đến HomeController
        } else {
            // Nếu sai → quay lại login và thêm lỗi
            return "redirect:/login?error=1";
        }
    }
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // xóa session user
        return "redirect:/login"; // quay lại trang login
    }
}
