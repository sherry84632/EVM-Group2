package com.dealermanagementsysstem.project.service;

import com.dealermanagementsysstem.project.Model.DAOAccount;
import com.dealermanagementsysstem.project.Model.DTOAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    @Autowired
    private DAOAccount daoAccount;

    @Autowired
    private PasswordService passwordService;

    /**
     * Authenticate user with email and password
     * Handles both plain text (legacy) and hashed passwords
     */
    public DTOAccount authenticateUser(String email, String password) {
        DTOAccount account = daoAccount.findAccountByEmail(email);
        
        if (account == null || !account.isStatus()) {
            return null;
        }

        String storedPassword = account.getPassword();
        
        // Check if password is hashed or plain text (for migration)
        if (passwordService.needsPasswordUpdate(storedPassword)) {
            // Legacy plain text password - verify directly
            if (storedPassword.equals(password)) {
                // Update to hashed password for future logins
                String hashedPassword = passwordService.hashPassword(password);
                daoAccount.updatePassword(account.getAccountId(), hashedPassword);
                return account;
            }
        } else {
            // Modern hashed password - use BCrypt verification
            if (passwordService.verifyPassword(password, storedPassword)) {
                return account;
            }
        }
        
        return null;
    }

    /**
     * Update user password
     */
    public boolean updatePassword(int accountId, String newPassword) {
        String hashedPassword = passwordService.hashPassword(newPassword);
        return daoAccount.updatePassword(accountId, hashedPassword);
    }
}
