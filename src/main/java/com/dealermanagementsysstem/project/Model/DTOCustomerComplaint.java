package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.sql.Timestamp;

@Entity
@Table(name = "CustomerComplaint")
public class DTOCustomerComplaint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ComplaintID")
    private Integer complaintID;

    @ManyToOne
    @JoinColumn(name = "DealerID", referencedColumnName = "DealerID")
    private DTODealer dealer; // nullable (if not tied yet)

    @ManyToOne
    @JoinColumn(name = "CustomerID", referencedColumnName = "CustomerID")
    private DTOCustomer customer;

    @Column(name = "ComplaintDate")
    private LocalDate complaintDate;

    @Column(name = "Status")
    private String status; // APPROVED | PROCESSED (simplified)

    @Column(name = "Note")
    private String note;

    @Column(name = "CreatedAt")
    private Timestamp createdAt;

    @Column(name = "UpdatedAt")
    private Timestamp updatedAt;

    public Integer getComplaintID() { return complaintID; }
    public void setComplaintID(Integer complaintID) { this.complaintID = complaintID; }
    public DTODealer getDealer() { return dealer; }
    public void setDealer(DTODealer dealer) { this.dealer = dealer; }
    public DTOCustomer getCustomer() { return customer; }
    public void setCustomer(DTOCustomer customer) { this.customer = customer; }
    public LocalDate getComplaintDate() { return complaintDate; }
    public void setComplaintDate(LocalDate complaintDate) { this.complaintDate = complaintDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    @Transient
    public String getCustomerName(){ return customer!=null? customer.getFullName(): null; }
}

