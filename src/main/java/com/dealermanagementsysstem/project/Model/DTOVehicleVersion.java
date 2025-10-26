package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "VehicleVersion")
public class DTOVehicleVersion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "VersionID")
    private int versionID;
    
    @ManyToOne
    @JoinColumn(name = "ModelID", referencedColumnName = "ModelID")
    private DTOVehicleModel model;
    
    @Column(name = "VersionName")
    private String versionName;
    
    @OneToMany(mappedBy = "version")
    private List<DTOVehicle> vehicles;
    
    @OneToMany(mappedBy = "version")
    private List<DTOPurchaseOrderDetail> purchaseOrderDetails;
    
    public DTOVehicleVersion() {
    }
    
    public DTOVehicleVersion(int versionID, DTOVehicleModel model, String versionName) {
        this.versionID = versionID;
        this.model = model;
        this.versionName = versionName;
    }
    
    public int getVersionID() {
        return versionID;
    }
    
    public void setVersionID(int versionID) {
        this.versionID = versionID;
    }
    
    public DTOVehicleModel getModel() {
        return model;
    }
    
    public void setModel(DTOVehicleModel model) {
        this.model = model;
    }
    
    public String getVersionName() {
        return versionName;
    }
    
    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }
    
    public List<DTOVehicle> getVehicles() {
        return vehicles;
    }
    
    public void setVehicles(List<DTOVehicle> vehicles) {
        this.vehicles = vehicles;
    }
    
    public List<DTOPurchaseOrderDetail> getPurchaseOrderDetails() {
        return purchaseOrderDetails;
    }
    
    public void setPurchaseOrderDetails(List<DTOPurchaseOrderDetail> purchaseOrderDetails) {
        this.purchaseOrderDetails = purchaseOrderDetails;
    }
}
