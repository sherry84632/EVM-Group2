package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "DealerInventory")
public class DTODealerInventory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DealerInventoryID")
    private int dealerInventoryID;
    
    @ManyToOne
    @JoinColumn(name = "DealerID", referencedColumnName = "DealerID")
    private DTODealer dealer;
    
    @Column(name = "VIN")
    private String vin;

    @ManyToOne
    @JoinColumn(name = "VehicleID", referencedColumnName = "VehicleID")
    private DTOVehicle vehicle;
    
    @Column(name = "ReceivedDate")
    private Date receivedDate;
    
    @Column(name = "Status")
    @Enumerated(EnumType.STRING)
    private DealerInventoryStatus status;

    @Column(name = "CostPrice")
    private java.math.BigDecimal costPrice; // Giá cost sau chiết khấu từ EVM

    @ManyToOne
    @JoinColumn(name = "PurchaseOrderID", referencedColumnName = "PurchaseOrderID")
    private DTOPurchaseOrder purchaseOrder;

    // Transient fields for UI display
    @Transient
    private String modelName;

    @Transient
    private String versionName;

    @Transient
    private String colorName;

    public DTODealerInventory() {}

    public DTODealerInventory(int dealerInventoryID, DTODealer dealer, DTOVehicle vehicle, 
                              Date receivedDate, DealerInventoryStatus status) {
        this.dealerInventoryID = dealerInventoryID;
        this.dealer = dealer;
        this.vehicle = vehicle;
        this.receivedDate = receivedDate;
        this.status = status;
    }

    public int getDealerInventoryID() {
        return dealerInventoryID;
    }

    public void setDealerInventoryID(int dealerInventoryID) {
        this.dealerInventoryID = dealerInventoryID;
    }

    public DTODealer getDealer() {
        return dealer;
    }

    public void setDealer(DTODealer dealer) {
        this.dealer = dealer;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public DTOVehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(DTOVehicle vehicle) {
        this.vehicle = vehicle;
    }

    public Date getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(Date receivedDate) {
        this.receivedDate = receivedDate;
    }

    public DealerInventoryStatus getStatus() {
        return status;
    }

    public void setStatus(DealerInventoryStatus status) {
        this.status = status;
    }

    public java.math.BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(java.math.BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public DTOPurchaseOrder getPurchaseOrder() {
        return purchaseOrder;
    }

    public void setPurchaseOrder(DTOPurchaseOrder purchaseOrder) {
        this.purchaseOrder = purchaseOrder;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getColorName() {
        return colorName;
    }

    public void setColorName(String colorName) {
        this.colorName = colorName;
    }
}
