package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "SaleOrderDetail")
public class DTOSaleOrderDetail {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SODetailID")
    private int soDetailID;
    
    @ManyToOne
    @JoinColumn(name = "SaleOrderID", referencedColumnName = "SaleOrderID")
    private DTOSaleOrder saleOrder;
    
    @ManyToOne
    @JoinColumn(name = "VIN", referencedColumnName = "VIN")
    private DTOVehicle vehicle;
    
    @Column(name = "Price")
    private BigDecimal price;
    
    @ManyToOne
    @JoinColumn(name = "PolicyID", referencedColumnName = "PolicyID")
    private DTODiscountPolicy discountPolicy;
    public DTOSaleOrderDetail() {
    }

    public DTOSaleOrderDetail(int soDetailID, DTOSaleOrder saleOrder, DTOVehicle vehicle, BigDecimal price, 
                              DTODiscountPolicy discountPolicy) {
        this.soDetailID = soDetailID;
        this.saleOrder = saleOrder;
        this.vehicle = vehicle;
        this.price = price;
        this.discountPolicy = discountPolicy;
    }

    public DTOSaleOrder getSaleOrder() {
        return saleOrder;
    }

    public void setSaleOrder(DTOSaleOrder saleOrder) {
        this.saleOrder = saleOrder;
    }

    public int getSoDetailID() {
        return soDetailID;
    }

    public void setSoDetailID(int soDetailID) {
        this.soDetailID = soDetailID;
    }

    public DTODiscountPolicy getDiscountPolicy() {
        return discountPolicy;
    }

    public void setDiscountPolicy(DTODiscountPolicy discountPolicy) {
        this.discountPolicy = discountPolicy;
    }


    public DTOVehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(DTOVehicle vehicle) {
        this.vehicle = vehicle;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }


}
