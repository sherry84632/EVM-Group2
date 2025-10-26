package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "Vehicle")
public class DTOVehicle {

    @Id
    @Column(name = "VIN")
    private String VIN;
    
    @ManyToOne
    @JoinColumn(name = "ColorID", referencedColumnName = "ColorID")
    private DTOVehicleColor color;
    
    @ManyToOne
    @JoinColumn(name = "VersionID", referencedColumnName = "VersionID")
    private DTOVehicleVersion version;
    
    @Column(name = "ManufactureYear")
    private int manufactureYear;
    
    @Column(name = "EngineNumber")
    private String engineNumber;
    
    @ManyToOne
    @JoinColumn(name = "OwnerID", referencedColumnName = "CustomerID")
    private DTOCustomer owner;
    
    @ManyToOne
    @JoinColumn(name = "CurrentDealerID", referencedColumnName = "DealerID")
    private DTODealer currentDealer;
    
    @Column(name = "Status")
    @Enumerated(EnumType.STRING)
    private VehicleStatus status;
    
    @Column(name = "CreatedAt")
    private java.sql.Timestamp createdAt;
    
    @Column(name = "UpdatedAt")
    private java.sql.Timestamp updatedAt;

    public DTOVehicle() {
    }

    public DTOVehicle(String VIN, DTOVehicleColor color, DTOVehicleVersion version,
                      int manufactureYear, String engineNumber, DTOCustomer owner, 
                      DTODealer currentDealer, VehicleStatus status) {
        this.VIN = VIN;
        this.color = color;
        this.version = version;
        this.manufactureYear = manufactureYear;
        this.engineNumber = engineNumber;
        this.owner = owner;
        this.currentDealer = currentDealer;
        this.status = status;
    }

    public String getVIN() {
        return VIN;
    }

    public void setVIN(String VIN) {
        this.VIN = VIN;
    }

    public int getManufactureYear() {
        return manufactureYear;
    }

    public void setManufactureYear(int manufactureYear) {
        this.manufactureYear = manufactureYear;
    }

    public DTOVehicleColor getColor() {
        return color;
    }

    public void setColor(DTOVehicleColor color) {
        this.color = color;
    }

    public String getEngineNumber() {
        return engineNumber;
    }

    public void setEngineNumber(String engineNumber) {
        this.engineNumber = engineNumber;
    }

    public DTOCustomer getOwner() {
        return owner;
    }

    public void setOwner(DTOCustomer owner) {
        this.owner = owner;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    public void setStatus(VehicleStatus status) {
        this.status = status;
    }

    public DTOVehicleVersion getVersion() {
        return version;
    }

    public void setVersion(DTOVehicleVersion version) {
        this.version = version;
    }

    public DTODealer getCurrentDealer() {
        return currentDealer;
    }

    public void setCurrentDealer(DTODealer currentDealer) {
        this.currentDealer = currentDealer;
    }
    
    public java.sql.Timestamp getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(java.sql.Timestamp createdAt) {
        this.createdAt = createdAt;
    }
    
    public java.sql.Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(java.sql.Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    // === CONVENIENCE METHODS FOR THYMELEAF TEMPLATES ===
    
    /**
     * Convenience method to access model through version relationship
     * Usage in Thymeleaf: ${vehicle.model} instead of ${vehicle.version.model}
     */
    public DTOVehicleModel getModel() {
        return version != null ? version.getModel() : null;
    }
    
    /**
     * Convenience method to access model name
     * Usage in Thymeleaf: ${vehicle.modelName}
     */
    public String getModelName() {
        return version != null && version.getModel() != null ? version.getModel().getModelName() : null;
    }
    
    /**
     * Convenience method to access color name
     * Usage in Thymeleaf: ${vehicle.colorName}
     */
    public String getColorName() {
        return color != null ? color.getColorName() : null;
    }
    
    /**
     * Convenience method to access version name
     * Usage in Thymeleaf: ${vehicle.versionName}
     */
    public String getVersionName() {
        return version != null ? version.getVersionName() : null;
    }
}
