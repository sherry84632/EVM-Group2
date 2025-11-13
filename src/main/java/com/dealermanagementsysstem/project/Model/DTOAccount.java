package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "Account")
public class DTOAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AccountID")
    private int accountId;

    @Column(name = "Username")
    private String username;

    @Column(name = "Password")
    private String password;

    @Column(name = "Role",nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "IsActive")
    private boolean isActive  = true ;

    @Column(name = "Email",unique = true)
    private String email;
    
    @Column(name = "Phone")
    private String phone;

    @Column(name = "CreatedAt")
    private java.sql.Timestamp createdAt;
    
    @Column(name = "UpdatedAt")
    private java.sql.Timestamp updatedAt;


    @OneToOne(mappedBy = "account")
    private DTODealerStaff dealerStaff;

    // Constructors
    public DTOAccount() {}

    public DTOAccount(int accountId, String username, String password, Role role, String email, boolean isActive,  DTODealerStaff dealerStaff) {
        this.accountId = accountId;
        this.username = username;
        this.password = password;
        this.role = role;
        this.email = email;
        this.isActive = isActive;
        this.dealerStaff = dealerStaff;
    }


    public DTODealerStaff getDealerStaff() {
        return dealerStaff;
    }

    public void setDealerStaff(DTODealerStaff dealerStaff) {
        this.dealerStaff = dealerStaff;
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

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public java.sql.Timestamp getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(java.sql.Timestamp createdAt) {
        this.createdAt = createdAt;
    }
    
    public java.sql.Timestamp getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(java.sql.Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
