package com.dealermanagementsysstem.project.Model;

import java.sql.Timestamp;
import java.util.List;

public class DTOSaleOrder {
    private int saleOrderID;
    private DTOCustomer customer;
    private DTODealer dealer;
    private DTODealerStaff staff;
    private Timestamp createdAt;
    private String status;
    private List<DTOSaleOrderDetail> detail; // 🔹 Danh sách chi tiết đơn hàng

    public DTOSaleOrder() {
    }

    public DTOSaleOrder(int saleOrderID, DTOCustomer customer, DTODealer dealer,
                        DTODealerStaff staff, Timestamp createdAt, String status,
                        List<DTOSaleOrderDetail> detail) {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<DTOSaleOrderDetail> getDetail() {
        return detail;
    }

    public void setDetail(List<DTOSaleOrderDetail> detail) {
        this.detail = detail;
    }
}
