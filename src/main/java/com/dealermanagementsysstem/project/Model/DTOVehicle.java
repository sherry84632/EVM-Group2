package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "Vehicle")
public class DTOVehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "VehicleID")
    private Integer vehicleID;

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
    
    @Column(name = "Status", columnDefinition = "VARCHAR(50)")
    @Enumerated(EnumType.STRING)
    private VehicleStatus status;
    
    @Column(name = "Description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "CreatedAt")
    private java.sql.Timestamp createdAt;
    
    @Column(name = "UpdatedAt")
    private java.sql.Timestamp updatedAt;

    public DTOVehicle() {
    }

    public DTOVehicle(Integer vehicleID, DTOVehicleColor color, DTOVehicleVersion version,
                      int manufactureYear, String engineNumber, VehicleStatus status) {
        this.vehicleID = vehicleID;
        this.color = color;
        this.version = version;
        this.manufactureYear = manufactureYear;
        this.engineNumber = engineNumber;
        this.status = status;
    }

    public Integer getVehicleID() {
        return vehicleID;
    }

    public void setVehicleID(Integer vehicleID) {
        this.vehicleID = vehicleID;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    /**
     * Convenience method to access model ID
     * Usage in Thymeleaf: ${vehicle.modelID}
     */
    public Integer getModelID() {
        return version != null && version.getModel() != null ? version.getModel().getModelID() : null;
    }

    /**
     * Convenience method to access color ID
     * Usage in Thymeleaf: ${vehicle.colorID}
     */
    public Integer getColorID() {
        return color != null ? color.getColorID() : null;
    }

    /**
     * Convenience method to access VIN (Vehicle Identification Number)
     * Usage in Thymeleaf: ${vehicle.VIN}
     * Assuming engineNumber represents VIN in current data model
     */
    public String getVIN() {
        return getEngineNumber();
    }

    /**
     * Convenience method to access base price of the vehicle
     * Usage in Thymeleaf: ${vehicle.basePrice}
     * Delegates to model's base price if available
     */
    public java.math.BigDecimal getBasePrice() {
        if (version != null && version.getModel() != null) {
            return version.getModel().getBasePrice();
        }
        return java.math.BigDecimal.ZERO; // fallback to 0 to avoid null in templates
    }
}
