package com.dealermanagementsysstem.project.Model;

import java.sql.Date;

@Deprecated
public class DTOOrder {
    private int orderID;
    private int dealerID;
    private int quotationID;
    private Date orderDate;
    private double totalAmount;
    private String status;

    public DTOOrder() {}

    public DTOOrder(int orderID, int dealerID, int quotationID, Date orderDate, double totalAmount, String status) {
        this.orderID = orderID;
        this.dealerID = dealerID;
        this.quotationID = quotationID;
        this.orderDate = orderDate;
        this.totalAmount = totalAmount;
        this.status = status;
    }

    public int getOrderID() {
        return orderID;
    }

    public void setOrderID(int orderID) {
        this.orderID = orderID;
    }

    public int getDealerID() {
        return dealerID;
    }

    public void setDealerID(int dealerID) {
        this.dealerID = dealerID;
    }

    public int getQuotationID() {
        return quotationID;
    }

    public void setQuotationID(int quotationID) {
        this.quotationID = quotationID;
    }

    public Date getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(Date orderDate) {
        this.orderDate = orderDate;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
