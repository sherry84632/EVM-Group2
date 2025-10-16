package com.dealermanagementsysstem.project.Model;

import java.util.Date;
import java.util.List;

public class DTOPurchaseOrder {
    private int purchaseOrderId;   // Tự sinh từ DB (IDENTITY)
    private int dealerId;          // Lấy tự động từ Account / Dealer
    private int staffId;           // Nhân viên EVM tạo / duyệt
    private String status;         // Pending / Approved / Rejected
    private Date createdAt;        // Ngày tạo (DB default GETDATE())

    // Danh sách chi tiết xe (nếu có)
    private List<DTOPurchaseOrderDetail> orderDetails;

    public DTOPurchaseOrder() {}

    // Constructor đầy đủ
    public DTOPurchaseOrder(int purchaseOrderId, int dealerId, int staffId, String status, Date createdAt) {
        this.purchaseOrderId = purchaseOrderId;
        this.dealerId = dealerId;
        this.staffId = staffId;
        this.status = status;
        this.createdAt = createdAt;
    }

    // --- GETTER & SETTER ---
    public int getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public void setPurchaseOrderId(int purchaseOrderId) {
        this.purchaseOrderId = purchaseOrderId;
    }

    public int getDealerId() {
        return dealerId;
    }

    public void setDealerId(int dealerId) {
        this.dealerId = dealerId;
    }

    public int getStaffId() {
        return staffId;
    }

    public void setStaffId(int staffId) {
        this.staffId = staffId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public List<DTOPurchaseOrderDetail> getOrderDetails() {
        return orderDetails;
    }

    public void setOrderDetails(List<DTOPurchaseOrderDetail> orderDetails) {
        this.orderDetails = orderDetails;
    }

    @Override
    public String toString() {
        return "DTOPurchaseOrder{" +
                "purchaseOrderId=" + purchaseOrderId +
                ", dealerId=" + dealerId +
                ", staffId=" + staffId +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", orderDetails=" + orderDetails +
                '}';
    }
}
