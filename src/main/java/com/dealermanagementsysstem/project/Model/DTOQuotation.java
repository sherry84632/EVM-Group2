package com.dealermanagementsysstem.project.Model;

import java.sql.Timestamp;

public class DTOQuotation {
    private int quotationID;
    private DTODealer dealer;
    private DTOCustomer customer;
    private DTOVehicle vehicle;
    private Timestamp createdAt;
    private String status;
    private DTODiscountPolicy DiscountPolicy;
    private double totalPrice;


    public DTOQuotation() {
    }

    public DTOQuotation(int quotationID, DTODealer dealer, DTOCustomer customer, DTOVehicle vehicle, Timestamp createdAt, String status, DTODiscountPolicy discountPolicy, double totalPrice) {
        this.quotationID = quotationID;
        this.dealer = dealer;
        this.customer = customer;
        this.vehicle = vehicle;
        this.createdAt = createdAt;
        this.status = status;
        DiscountPolicy = discountPolicy;
        this.totalPrice = totalPrice;
    }

    public int getQuotationID() {
        return quotationID;
    }

    public void setQuotationID(int quotationID) {
        this.quotationID = quotationID;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public DTODealer getDealer() {
        return dealer;
    }

    public void setDealer(DTODealer dealer) {
        this.dealer = dealer;
    }

    public DTOCustomer getCustomer() {
        return customer;
    }

    public void setCustomer(DTOCustomer customer) {
        this.customer = customer;
    }

    public DTOVehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(DTOVehicle vehicle) {
        this.vehicle = vehicle;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public DTODiscountPolicy getDiscountPolicy() {
        return DiscountPolicy;
    }

    public void setDiscountPolicy(DTODiscountPolicy discountPolicy) {
        DiscountPolicy = discountPolicy;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
