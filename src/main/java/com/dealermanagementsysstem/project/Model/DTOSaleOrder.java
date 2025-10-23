package com.dealermanagementsysstem.project.Model;

import java.sql.Timestamp;
import java.math.BigDecimal;
import java.util.List;

public class DTOSaleOrder {
    private int saleOrderID;
    private int quotationID; // ✅ FIX: Add quotationID to track source quotation
    private DTOCustomer customer;
    private DTODealer dealer;
    private DTODealerStaff staff;
    private Timestamp createdAt;
    private String status;
    private List<DTOSaleOrderDetail> details; // ✅ FIX: Changed from singular to List
    // Aggregated fields
    private int totalQuantity;            // Tổng số lượng (sum of details)
    private BigDecimal totalAmount;       // Tổng tiền (sum of price * quantity)

    public DTOSaleOrder() {
        this.customer = new DTOCustomer();
        this.dealer = new DTODealer();
        this.staff = new DTODealerStaff();
        this.details = new java.util.ArrayList<>(); // ✅ Initialize list
    }

    public DTOSaleOrder(int saleOrderID, int quotationID, DTOCustomer customer, DTODealer dealer,
                        DTODealerStaff staff, Timestamp createdAt, String status,
                        List<DTOSaleOrderDetail> details) {
        this.saleOrderID = saleOrderID;
        this.quotationID = quotationID;
        this.customer = customer;
        this.dealer = dealer;
        this.staff = staff;
        this.createdAt = createdAt;
        this.status = status;
        this.details = details;
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

    // ✅ FIX: Add quotationID getter/setter
    public int getQuotationID() {
        return quotationID;
    }

    public void setQuotationID(int quotationID) {
        this.quotationID = quotationID;
    }

    // ✅ FIX: Changed from detail (singular) to details (List)
    public List<DTOSaleOrderDetail> getDetails() {
        return details;
    }

    public void setDetails(List<DTOSaleOrderDetail> details) {
        this.details = details;
    }

    // Legacy support for old code using getDetail()
    @Deprecated
    public DTOSaleOrderDetail getDetail() {
        return details != null && !details.isEmpty() ? details.get(0) : null;
    }

    @Deprecated
    public void setDetail(DTOSaleOrderDetail detail) {
        if (this.details == null) {
            this.details = new java.util.ArrayList<>();
        }
        this.details.clear();
        if (detail != null) {
            this.details.add(detail);
        }
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
