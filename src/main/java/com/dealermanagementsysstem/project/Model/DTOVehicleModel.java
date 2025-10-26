package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "VehicleModel")
public class DTOVehicleModel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ModelID")
    private int modelID;
    
    @Column(name = "ModelName")
    private String modelName;
    
    @Column(name = "BasePrice")
    private BigDecimal basePrice;
    
    @Column(name = "EvmID")
    private int evmID;
    
    @OneToMany(mappedBy = "model")
    private List<DTOVehicleVersion> versions;
    
    public DTOVehicleModel() {
    }
    
    public DTOVehicleModel(int modelID, String modelName, BigDecimal basePrice, int evmID) {
        this.modelID = modelID;
        this.modelName = modelName;
        this.basePrice = basePrice;
        this.evmID = evmID;
    }
    
    public int getModelID() {
        return modelID;
    }
    
    public void setModelID(int modelID) {
        this.modelID = modelID;
    }
    
    public String getModelName() {
        return modelName;
    }
    
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
    
    public BigDecimal getBasePrice() {
        return basePrice;
    }
    
    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }
    
    public int getEvmID() {
        return evmID;
    }
    
    public void setEvmID(int evmID) {
        this.evmID = evmID;
    }
    
    public List<DTOVehicleVersion> getVersions() {
        return versions;
    }
    
    public void setVersions(List<DTOVehicleVersion> versions) {
        this.versions = versions;
    }
}
