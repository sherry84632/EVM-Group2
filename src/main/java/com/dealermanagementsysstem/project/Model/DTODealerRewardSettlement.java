package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Table(name="DealerRewardSettlement")
public class DTODealerRewardSettlement {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="RewardSettlementID")
    private Integer rewardSettlementID;
    @Column(name="DealerID")
    private Integer dealerID;
    @Column(name="PeriodYear")
    private Integer periodYear;
    @Column(name="PeriodMonth")
    private Integer periodMonth;
    @Column(name="ImportedQuantity")
    private Integer importedQuantity;
    @Column(name="RewardPercent")
    private BigDecimal rewardPercent; // stored percent
    @Column(name="RewardAmount")
    private BigDecimal rewardAmount; // computed = importedQuantity * avgImportValue? simplified: quantity * rewardPercent * base? we store direct calculation externally
    @Column(name="Status")
    private String status; // PENDING, APPROVED, PAID
    @Column(name="Notes")
    private String notes;
    @Column(name="CreatedAt")
    private Timestamp createdAt;
    @Column(name="UpdatedAt")
    private Timestamp updatedAt;
    @Column(name="PaidDate")
    private Timestamp paidDate;
    @Column(name="ReimbursedAmount")
    private java.math.BigDecimal reimbursedAmount; // mới thêm để lưu số tiền đã quyết toán

    public Integer getRewardSettlementID() { return rewardSettlementID; }
    public void setRewardSettlementID(Integer rewardSettlementID) { this.rewardSettlementID = rewardSettlementID; }
    public Integer getDealerID() { return dealerID; }
    public void setDealerID(Integer dealerID) { this.dealerID = dealerID; }
    public Integer getPeriodYear() { return periodYear; }
    public void setPeriodYear(Integer periodYear) { this.periodYear = periodYear; }
    public Integer getPeriodMonth() { return periodMonth; }
    public void setPeriodMonth(Integer periodMonth) { this.periodMonth = periodMonth; }
    public Integer getImportedQuantity() { return importedQuantity; }
    public void setImportedQuantity(Integer importedQuantity) { this.importedQuantity = importedQuantity; }
    public BigDecimal getRewardPercent() { return rewardPercent; }
    public void setRewardPercent(BigDecimal rewardPercent) { this.rewardPercent = rewardPercent; }
    public BigDecimal getRewardAmount() { return rewardAmount; }
    public void setRewardAmount(BigDecimal rewardAmount) { this.rewardAmount = rewardAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
    public Timestamp getPaidDate() { return paidDate; }
    public void setPaidDate(Timestamp paidDate) { this.paidDate = paidDate; }
    public java.math.BigDecimal getReimbursedAmount(){ return reimbursedAmount; }
    public void setReimbursedAmount(java.math.BigDecimal reimbursedAmount){ this.reimbursedAmount = reimbursedAmount; }

    @Transient
    public String getPeriodLabel(){ return String.format("%02d/%d", periodMonth, periodYear); }

    @Transient
    public java.math.BigDecimal getImportedValue(){
        if(rewardAmount==null || rewardPercent==null) return java.math.BigDecimal.ZERO;
        if(rewardPercent.compareTo(java.math.BigDecimal.ZERO)==0) return java.math.BigDecimal.ZERO;
        return rewardAmount.multiply(java.math.BigDecimal.valueOf(100))
                .divide(rewardPercent, 2, java.math.RoundingMode.HALF_UP);
    }

    @Transient
    public java.math.BigDecimal getOutstanding(){
        java.math.BigDecimal total = rewardAmount!=null? rewardAmount: java.math.BigDecimal.ZERO;
        java.math.BigDecimal paid = reimbursedAmount!=null? reimbursedAmount: java.math.BigDecimal.ZERO;
        if(paid.compareTo(total)>=0) return java.math.BigDecimal.ZERO; // fully settled
        java.math.BigDecimal out = total.subtract(paid);
        return out.compareTo(java.math.BigDecimal.ZERO) < 0 ? java.math.BigDecimal.ZERO : out;
    }
    @Transient
    public double getPercentPaid(){
        java.math.BigDecimal total = rewardAmount!=null? rewardAmount: java.math.BigDecimal.ZERO;
        if(total.compareTo(java.math.BigDecimal.ZERO)<=0) return 0.0;
        java.math.BigDecimal paid = reimbursedAmount!=null? reimbursedAmount: java.math.BigDecimal.ZERO;
        return paid.divide(total,4, java.math.RoundingMode.HALF_UP).multiply(java.math.BigDecimal.valueOf(100)).doubleValue();
    }
    @Transient
    public boolean isLocked(){
        java.math.BigDecimal total = rewardAmount!=null? rewardAmount: java.math.BigDecimal.ZERO;
        java.math.BigDecimal paid = reimbursedAmount!=null? reimbursedAmount: java.math.BigDecimal.ZERO;
        boolean fullyPaid = paid.compareTo(total)>=0 && total.compareTo(java.math.BigDecimal.ZERO)>0;
        return fullyPaid || (status!=null && status.equalsIgnoreCase("PAID"));
    }

    @Transient
    public int getBatchNumber(){ return 1; }
}
