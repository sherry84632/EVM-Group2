package com.dealermanagementsysstem.project.Model;

import java.time.LocalDate;

public class DTODiscountPolicy {
    private int policyID;
    private int dealerID;
    private String policyName;
    private String description;
    private double hangPercent;
    private double dailyPercent;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status; // Active / Expired

    // Thêm thuộc tính tùy chọn dùng khi liên kết đơn hàng
    private int appliedOrderDetailID; // nếu có áp dụng vào SaleOrderDetail

    public DTODiscountPolicy() {}

    public DTODiscountPolicy(int policyID, int dealerID, String policyName, String description,
                             double hangPercent, double dailyPercent,
                             LocalDate startDate, LocalDate endDate, String status) {
        this.policyID = policyID;
        this.dealerID = dealerID;
        this.policyName = policyName;
        this.description = description;
        this.hangPercent = hangPercent;
        this.dailyPercent = dailyPercent;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    // Getters / Setters
    public int getPolicyID() { return policyID; }
    public void setPolicyID(int policyID) { this.policyID = policyID; }

    public int getDealerID() { return dealerID; }
    public void setDealerID(int dealerID) { this.dealerID = dealerID; }

    public String getPolicyName() { return policyName; }
    public void setPolicyName(String policyName) { this.policyName = policyName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getHangPercent() { return hangPercent; }
    public void setHangPercent(double hangPercent) { this.hangPercent = hangPercent; }

    public double getDailyPercent() { return dailyPercent; }
    public void setDailyPercent(double dailyPercent) { this.dailyPercent = dailyPercent; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getAppliedOrderDetailID() { return appliedOrderDetailID; }
    public void setAppliedOrderDetailID(int appliedOrderDetailID) { this.appliedOrderDetailID = appliedOrderDetailID; }
}
