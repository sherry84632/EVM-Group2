package com.dealermanagementsysstem.project.Model;



import java.util.Date;

public class DTOPurchaseOrder {
    private int purchaseOrderId;
    private int dealerId;
    private int staffId;
    private Date createdAt;
    private String status;

    public DTOPurchaseOrder() {}

    public DTOPurchaseOrder(int purchaseOrderId, int dealerId, int staffId, Date createdAt, String status) {
        this.purchaseOrderId = purchaseOrderId;
        this.dealerId = dealerId;
        this.staffId = staffId;
        this.createdAt = createdAt;
        this.status = status;
    }

    // Getters & Setters
    public int getPurchaseOrderId() { return purchaseOrderId; }
    public void setPurchaseOrderId(int purchaseOrderId) { this.purchaseOrderId = purchaseOrderId; }

    public int getDealerId() { return dealerId; }
    public void setDealerId(int dealerId) { this.dealerId = dealerId; }

    public int getStaffId() { return staffId; }
    public void setStaffId(int staffId) { this.staffId = staffId; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

