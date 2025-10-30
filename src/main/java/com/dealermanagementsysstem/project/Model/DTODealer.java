package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "Dealer")
public class DTODealer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DealerID")
    private int dealerID;

    @Column(name = "DealerName", nullable = false)
    private String dealerName;

    @Column(name = "Address")
    private String address;

    @Column(name = "Phone", unique = true)
    private String phone;

    @Column(name = "Email", unique = true)
    private String email;

    @Column(name = "EvmID")
    private int evmID;

    @Column(name = "LevelID")
    private int levelID;

    @Column(name = "PolicyID")
    private int policyID;
    
    @Column(name = "CreatedAt")
    private java.sql.Timestamp createdAt;

    @Column(name = "UpdatedAt")
    private java.sql.Timestamp updatedAt;

    // === CONSTRUCTORS ===

    public DTODealer() {
    }

    public DTODealer(int dealerID, String dealerName, String address, String phone,
                     String email, int evmID, int levelID, int policyID) {
        this.dealerID = dealerID;
        this.dealerName = dealerName;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.evmID = evmID;
        this.levelID = levelID;
        this.policyID = policyID;
    }

    // === GETTERS / SETTERS ===

    public int getDealerID() {
        return dealerID;
    }

    public void setDealerID(int dealerID) {
        this.dealerID = dealerID;
    }

    public String getDealerName() {
        return dealerName;
    }

    public void setDealerName(String dealerName) {
        this.dealerName = dealerName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getEvmID() {
        return evmID;
    }

    public void setEvmID(int evmID) {
        this.evmID = evmID;
    }

    public int getLevelID() {
        return levelID;
    }

    public void setLevelID(int levelID) {
        this.levelID = levelID;
    }

    public int getPolicyID() {
        return policyID;
    }

    public void setPolicyID(int policyID) {
        this.policyID = policyID;
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
