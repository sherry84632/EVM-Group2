package com.dealermanagementsysstem.project.Model;

public class DTODealerStaff {
    private int staffID;
    private String fullName;
    private String position;
    private String phone;
    private String email;

    public DTODealerStaff() {
    }

    public DTODealerStaff(int staffID, String fullName, String position, String phone, String email) {
        this.staffID = staffID;
        this.fullName = fullName;
        this.position = position;
        this.phone = phone;
        this.email = email;
    }

    public int getStaffID() {
        return staffID;
    }

    public void setStaffID(int staffID) {
        this.staffID = staffID;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
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
