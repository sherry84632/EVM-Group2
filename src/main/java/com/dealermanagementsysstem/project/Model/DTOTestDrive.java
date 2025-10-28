package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "TestDrive")
public class DTOTestDrive {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TestDriveID")
    private int testDriveID;
    
    @ManyToOne
    @JoinColumn(name = "CustomerID", referencedColumnName = "CustomerID")
    private DTOCustomer customer;
    
    @ManyToOne
    @JoinColumn(name = "VehicleID", referencedColumnName = "VehicleID")
    private DTOVehicle vehicle;
    
    @ManyToOne
    @JoinColumn(name = "DealerID", referencedColumnName = "DealerID")
    private DTODealer dealer;
    
    @ManyToOne
    @JoinColumn(name = "StaffID", referencedColumnName = "StaffID")
    private DTODealerStaff staff;
    
    @Column(name = "TestDate")
    private Date testDate;
    
    @Column(name = "Feedback")
    private String feedback;
    
    public DTOTestDrive() {
    }
    
    public DTOTestDrive(int testDriveID, DTOCustomer customer, DTOVehicle vehicle, 
                         DTODealer dealer, DTODealerStaff staff, Date testDate, String feedback) {
        this.testDriveID = testDriveID;
        this.customer = customer;
        this.vehicle = vehicle;
        this.dealer = dealer;
        this.staff = staff;
        this.testDate = testDate;
        this.feedback = feedback;
    }
    
    public int getTestDriveID() {
        return testDriveID;
    }
    
    public void setTestDriveID(int testDriveID) {
        this.testDriveID = testDriveID;
    }
    
    public DTOCustomer getCustomer() {
        return customer;
    }
    
    public void setCustomer(DTOCustomer customer) {
        this.customer = customer;
    }
    
    public DTOVehicle getVehicle() {
        return vehicle;
    }
    
    public void setVehicle(DTOVehicle vehicle) {
        this.vehicle = vehicle;
    }
    
    public DTODealer getDealer() {
        return dealer;
    }
    
    public void setDealer(DTODealer dealer) {
        this.dealer = dealer;
    }
    
    public DTODealerStaff getStaff() {
        return staff;
    }
    
    public void setStaff(DTODealerStaff staff) {
        this.staff = staff;
    }
    
    public Date getTestDate() {
        return testDate;
    }
    
    public void setTestDate(Date testDate) {
        this.testDate = testDate;
    }
    
    public String getFeedback() {
        return feedback;
    }
    
    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
}

