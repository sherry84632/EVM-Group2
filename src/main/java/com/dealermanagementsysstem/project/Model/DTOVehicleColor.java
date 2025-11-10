package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "VehicleColor")
public class DTOVehicleColor {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ColorID")
    private int colorID;
    
    @ManyToOne
    @JoinColumn(name = "ModelID", referencedColumnName = "ModelID")
    private DTOVehicleModel model;
    
    @Column(name = "ColorName")
    private String colorName;
    
    @OneToMany(mappedBy = "color")
    private List<DTOVehicle> vehicles;

    @OneToMany(mappedBy = "color")
    private List<DTOQuotationDetail> quotationDetails;
    
    @OneToMany(mappedBy = "color")
    private List<DTOPurchaseOrderDetail> purchaseOrderDetails;
    
    public DTOVehicleColor() {
    }
    
    public DTOVehicleColor(int colorID, DTOVehicleModel model, String colorName) {
        this.colorID = colorID;
        this.model = model;
        this.colorName = colorName;
    }
    
    public int getColorID() {
        return colorID;
    }
    
    public void setColorID(int colorID) {
        this.colorID = colorID;
    }
    
    public DTOVehicleModel getModel() {
        return model;
    }
    
    public void setModel(DTOVehicleModel model) {
        this.model = model;
    }
    
    public String getColorName() {
        return colorName;
    }
    
    public void setColorName(String colorName) {
        this.colorName = colorName;
    }
    
    public List<DTOVehicle> getVehicles() {
        return vehicles;
    }
    
    public void setVehicles(List<DTOVehicle> vehicles) {
        this.vehicles = vehicles;
    }

    
    public List<DTOQuotationDetail> getQuotationDetails() {
        return quotationDetails;
    }
    
    public void setQuotationDetails(List<DTOQuotationDetail> quotationDetails) {
        this.quotationDetails = quotationDetails;
    }
    
    public List<DTOPurchaseOrderDetail> getPurchaseOrderDetails() {
        return purchaseOrderDetails;
    }
    
    public void setPurchaseOrderDetails(List<DTOPurchaseOrderDetail> purchaseOrderDetails) {
        this.purchaseOrderDetails = purchaseOrderDetails;
    }
}
