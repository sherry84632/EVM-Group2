package com.dealermanagementsysstem.project.Model;

import java.sql.Timestamp;
import java.math.BigDecimal;
import java.util.List;

public class DTOSaleOrder {
    private int saleOrderID;
    private DTOCustomer customer;
    private DTODealer dealer;
    private DTODealerStaff staff;
    private Timestamp createdAt;
    private String status;
    private DTOSaleOrderDetail detail; // 🔹 Danh sách chi tiết đơn hàng
    // Aggregated fields
    private int totalQuantity;            // Tổng số lượng (sum of details)
    private BigDecimal totalAmount;       // Tổng tiền (sum of price * quantity)

    public DTOSaleOrder() {
    }

    public DTOSaleOrder(int saleOrderID, DTOCustomer customer, DTODealer dealer,
                        DTODealerStaff staff, Timestamp createdAt, String status,
                        DTOSaleOrderDetail detail) {
        this.saleOrderID = saleOrderID;
        this.customer = customer;
        this.dealer = dealer;
        this.staff = staff;
        this.createdAt = createdAt;
        this.status = status;
        this.detail = detail;
    }

    public int getSaleOrderID() {
        return saleOrderID;
    }

    public void setSaleOrderID(int saleOrderID) {
        this.saleOrderID = saleOrderID;
    }

    // Alias methods for legacy Thymeleaf templates referencing orderID instead of saleOrderID
    public int getOrderID() {
        return getSaleOrderID();
    }

    public void setOrderID(int id) {
        setSaleOrderID(id);
    }

    public DTOCustomer getCustomer() {
        return customer;
    }

    public void setCustomer(DTOCustomer customer) {
        this.customer = customer;
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

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    // === Thymeleaf legacy alias for orderDate ===
    public Timestamp getOrderDate() {
        return getCreatedAt();
    }

    public void setOrderDate(Timestamp ts) {
        setCreatedAt(ts);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public DTOSaleOrderDetail getDetail() {
        return detail;
    }

    public void setDetail(DTOSaleOrderDetail detail) {
        this.detail = detail;
    }

    // === Aggregated total quantity ===
    public int getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(int totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    // === Aggregated total amount ===
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}
