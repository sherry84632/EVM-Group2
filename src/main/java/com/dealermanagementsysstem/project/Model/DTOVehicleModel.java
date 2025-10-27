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
    
    @Column(name = "Brand")
    private String brand;

    @Column(name = "Year")
    private int year;

    @Column(name = "BasePrice")
    private BigDecimal basePrice;
    
    @Column(name = "BodyType")
    private String bodyType;

    @Column(name = "Description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "ModelImage")
    private byte[] modelImage;

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
    
    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }
    
    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }
    
    public byte[] getModelImage() {
        return modelImage;
    }

    public void setModelImage(byte[] modelImage) {
        this.modelImage = modelImage;
    }

    public String getBodyType() {
        return bodyType;
    }

    public void setBodyType(String bodyType) {
        this.bodyType = bodyType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
