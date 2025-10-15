package com.dealermanagementsysstem.project.Model;

import java.sql.Date;
import java.sql.Timestamp;

public class DTOCustomer {
    private int customerID;
    private String fullName;
    private String phone;
    private String email;
    private String address;
    private Timestamp createdAt;
    private Date birthDate;
    private String note;
    private Timestamp testDriveSchedule;
    private String vehicleInterest;

    // ✅ Getters & Setters
    public int getCustomerID() { return customerID; }
    public void setCustomerID(int customerID) { this.customerID = customerID; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public String getPhoneNumber() { return phone; } // alias getter
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Date getBirthDate() { return birthDate; }
    public void setBirthDate(Date birthDate) { this.birthDate = birthDate; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Timestamp getTestDriveSchedule() { return testDriveSchedule; }
    public void setTestDriveSchedule(Timestamp testDriveSchedule) { this.testDriveSchedule = testDriveSchedule; }

    public String getVehicleInterest() { return vehicleInterest; }
    public void setVehicleInterest(String vehicleInterest) { this.vehicleInterest = vehicleInterest; }
}
