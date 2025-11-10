package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
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
