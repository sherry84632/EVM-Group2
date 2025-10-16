package com.dealermanagementsysstem.project.Model;

public class DTODealer {
    private int dealerID;
    private String dealerName;
    private String address;
    private String phone;
    private String email;

    public DTODealer() {
    }

    public DTODealer(int dealerID, String dealerName, String address, String phone, String email) {
        this.dealerID = dealerID;
        this.dealerName = dealerName;
        this.address = address;
        this.phone = phone;
        this.email = email;
    }

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
}
