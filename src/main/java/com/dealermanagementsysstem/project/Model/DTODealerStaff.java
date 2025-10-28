package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "DealerStaff")
public class DTODealerStaff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "StaffID")
    private int staffID;

    @Column(name = "FullName")
    private String fullName;

    @Column(name = "Position")
    private String position;
    @Column(name = "Phone",unique = true)
    private String phone;
    @Column(name = "Email",unique = true)
    private String email;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AccountID", referencedColumnName = "AccountID", unique = true)
    private DTOAccount account;

    @ManyToOne
    @JoinColumn(name = "DealerID", referencedColumnName = "DealerID")
    private DTODealer dealer;


    public DTODealerStaff() {
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

    public DTOAccount getAccount() {
        return account;
    }

    public void setAccount(DTOAccount account) {
        this.account = account;
    }

    public DTODealer getDealer() {
        return dealer;
    }

    public void setDealer(DTODealer dealer) {
        this.dealer = dealer;
    }
}
