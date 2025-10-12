package com.dealermanagementsysstem.project.Model;

public class DTOAccount {
    private int accountId;
    private String username;
    private String password;
    private String role;
    private boolean status;
    private Integer evmStaffId;
    private Integer dealerId;
    private Integer dealerStaffId;
    private String email;

    // Constructors
    public DTOAccount() {}

    public DTOAccount(int accountId, String username, String password, String role, boolean status, Integer evmStaffId, Integer dealerId, Integer dealerStaffId, String email) {
        this.accountId = accountId;
        this.username = username;
        this.password = password;
        this.role = role;
        this.status = status;
        this.evmStaffId = evmStaffId;
        this.dealerId = dealerId;
        this.dealerStaffId = dealerStaffId;
        this.email = email;
    }

    public int getAccountId() {
        return accountId;
    }

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public Integer getEvmStaffId() {
        return evmStaffId;
    }

    public void setEvmStaffId(Integer evmStaffId) {
        this.evmStaffId = evmStaffId;
    }

    public Integer getDealerId() {
        return dealerId;
    }

    public void setDealerId(Integer dealerId) {
        this.dealerId = dealerId;
    }

    public Integer getDealerStaffId() {
        return dealerStaffId;
    }

    public void setDealerStaffId(Integer dealerStaffId) {
        this.dealerStaffId = dealerStaffId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
