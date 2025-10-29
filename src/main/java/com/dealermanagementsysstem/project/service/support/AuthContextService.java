package com.dealermanagementsysstem.project.service.support;

import com.dealermanagementsysstem.project.Model.DAOAccount;
import com.dealermanagementsysstem.project.Model.DTOAccount;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthContextService {

    @Autowired
    private DAOAccount daoAccount;

    /**
     * Lấy account hiện tại từ session; nếu chưa có sẽ đọc từ SecurityContext rồi hydrate.
     */
    public DTOAccount getCurrentAccount(HttpSession session) {
        if (session == null) return null;
        Object obj = session.getAttribute("user");
        if (obj instanceof DTOAccount acc) {
            return acc;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            DTOAccount db = daoAccount.findAccountByEmail(auth.getName());
            if (db != null) {
                session.setAttribute("user", db);
                return db;
            }
        }
        return null;
    }
}

