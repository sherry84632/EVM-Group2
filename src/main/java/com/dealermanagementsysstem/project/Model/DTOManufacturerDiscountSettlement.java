package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Table(name = "ManufacturerDiscountSettlement")
public class DTOManufacturerDiscountSettlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SettlementID")
    private Integer settlementID;

    @Column(name = "SaleOrderID")
    private Integer saleOrderID;

    @Column(name = "DealerID")
    private Integer dealerID;

    @Column(name = "TotalManufacturerDiscount")
    private BigDecimal totalManufacturerDiscount;

    @Column(name = "ReimbursedAmount")
    private BigDecimal reimbursedAmount;

    @Column(name = "Status")
    private String status; // PENDING / PARTIAL / PAID

    @Column(name = "CreatedAt")
    private Timestamp createdAt;

    @Column(name = "UpdatedAt")
    private Timestamp updatedAt;

    @Column(name = "PaidDate")
    private Timestamp paidDate;

    @Column(name = "Notes")
    private String notes;

    public DTOManufacturerDiscountSettlement() {}

    public Integer getSettlementID() { return settlementID; }
    public void setSettlementID(Integer settlementID) { this.settlementID = settlementID; }
    public Integer getSaleOrderID() { return saleOrderID; }
    public void setSaleOrderID(Integer saleOrderID) { this.saleOrderID = saleOrderID; }
    public Integer getDealerID() { return dealerID; }
    public void setDealerID(Integer dealerID) { this.dealerID = dealerID; }
    public BigDecimal getTotalManufacturerDiscount() { return totalManufacturerDiscount; }
    public void setTotalManufacturerDiscount(BigDecimal totalManufacturerDiscount) { this.totalManufacturerDiscount = totalManufacturerDiscount; }
    public BigDecimal getReimbursedAmount() { return reimbursedAmount; }
    public void setReimbursedAmount(BigDecimal reimbursedAmount) { this.reimbursedAmount = reimbursedAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
    public Timestamp getPaidDate() { return paidDate; }
    public void setPaidDate(Timestamp paidDate) { this.paidDate = paidDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @Transient
    public BigDecimal getOutstanding() {
        BigDecimal total = totalManufacturerDiscount != null ? totalManufacturerDiscount : BigDecimal.ZERO;
        BigDecimal reimb = reimbursedAmount != null ? reimbursedAmount : BigDecimal.ZERO;
        return total.subtract(reimb);
    }
}
