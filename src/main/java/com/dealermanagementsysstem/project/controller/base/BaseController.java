package com.dealermanagementsysstem.project.controller.base;

import com.dealermanagementsysstem.project.Model.DAOAccount;
import com.dealermanagementsysstem.project.Model.DTOAccount;
import com.dealermanagementsysstem.project.Model.DTODealer;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;

/**
 * BaseController cung cấp hàm tiện ích chung để giảm lặp lại giữa các controller.
 */
public abstract class BaseController {
    /** Lấy account hiện tại (từ session hoặc security). */
    protected DTOAccount currentAccount(HttpSession session, DAOAccount daoAccount) {
        if (session != null) {
            Object o = session.getAttribute("user");
            if (o instanceof DTOAccount a) return a;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()) && daoAccount != null) {
            DTOAccount acc = daoAccount.findAccountByEmail(auth.getName());
            if (acc != null && session != null) session.setAttribute("user", acc);
            return acc;
        }
        return null;
    }

    /** Lấy dealer hiện tại nếu account là dealer staff. */
    protected DTODealer currentDealer(HttpSession session, DAOAccount daoAccount) {
        DTOAccount acc = currentAccount(session, daoAccount);
        if (acc != null && acc.getDealerStaff()!=null) return acc.getDealerStaff().getDealer();
        return null;
    }

    /** Đưa thông tin user/email/role vào model nếu có. */
    protected void addUserContext(Model model, HttpSession session, DAOAccount daoAccount) {
        DTOAccount acc = currentAccount(session, daoAccount);
        if (acc != null) {
            model.addAttribute("currentUserEmail", acc.getEmail());
            model.addAttribute("currentUserRole", acc.getRole()!=null? acc.getRole().name(): null);
            if (acc.getDealerStaff()!=null && acc.getDealerStaff().getDealer()!=null) {
                model.addAttribute("currentDealerId", acc.getDealerStaff().getDealer().getDealerID());
            }
        }
    }
}
