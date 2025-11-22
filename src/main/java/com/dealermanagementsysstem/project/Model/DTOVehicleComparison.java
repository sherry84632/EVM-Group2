package com.dealermanagementsysstem.project.Model;

import java.math.BigDecimal;
import java.util.Map;

public class DTOVehicleComparison {
    
    private Integer vehicleID;
    private Integer versionID;
    private Integer modelID;
    
    private String modelName;
    private String brand;
    private Integer year;
    private String bodyType;
    private String modelDescription;
    private byte[] modelImage;
    
    private String versionName;
    private String engine;
    private String transmission;
    
    private String colorName;
    private Integer colorID;
    
    private Integer manufactureYear;
    private String vin;
    private String engineNumber;
    private VehicleStatus status;
    
    private BigDecimal basePrice;
    private BigDecimal dealerSellingPrice;
    private BigDecimal finalPrice;
    
    private String imageUrl;
    private String detailUrl;
    
    private Map<String, Object> additionalSpecs;
    
    public DTOVehicleComparison() {
    }
    
    public DTOVehicleComparison(DTOVehicle vehicle) {
        if (vehicle == null) return;
        
        this.vehicleID = vehicle.getVehicleID();
        this.manufactureYear = vehicle.getManufactureYear();
        this.engineNumber = vehicle.getEngineNumber();
        this.vin = vehicle.getVin();
        this.status = vehicle.getStatus();
        
        if (vehicle.getColor() != null) {
            this.colorID = vehicle.getColor().getColorID();
            this.colorName = vehicle.getColor().getColorName();
        }
        
        if (vehicle.getVersion() != null) {
            this.versionID = vehicle.getVersion().getVersionID();
            this.versionName = vehicle.getVersion().getVersionName();
            this.engine = vehicle.getVersion().getEngine();
            this.transmission = vehicle.getVersion().getTransmission();
            
            if (vehicle.getVersion().getModel() != null) {
                DTOVehicleModel model = vehicle.getVersion().getModel();
                this.modelID = model.getModelID();
                this.modelName = model.getModelName();
                this.brand = model.getBrand();
                this.year = model.getYear();
                this.bodyType = model.getBodyType();
                this.modelDescription = model.getDescription();
                this.modelImage = model.getModelImage();
                this.basePrice = model.getBasePrice();
                this.dealerSellingPrice = model.getDealerSellingPrice();
            }
        }
        
        this.dealerSellingPrice = vehicle.getDealerSellingPrice() != null 
            ? vehicle.getDealerSellingPrice() 
            : this.dealerSellingPrice;
        
        this.finalPrice = this.dealerSellingPrice != null 
            ? this.dealerSellingPrice 
            : (this.basePrice != null ? this.basePrice : BigDecimal.ZERO);
    }
    
    public Integer getVehicleID() {
        return vehicleID;
    }
    
    public void setVehicleID(Integer vehicleID) {
        this.vehicleID = vehicleID;
    }
    
    public Integer getVersionID() {
        return versionID;
    }
    
    public void setVersionID(Integer versionID) {
        this.versionID = versionID;
    }
    
    public Integer getModelID() {
        return modelID;
    }
    
    public void setModelID(Integer modelID) {
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
    
    public Integer getYear() {
        return year;
    }
    
    public void setYear(Integer year) {
        this.year = year;
    }
    
    public String getBodyType() {
        return bodyType;
    }
    
    public void setBodyType(String bodyType) {
        this.bodyType = bodyType;
    }
    
    public String getModelDescription() {
        return modelDescription;
    }
    
    public void setModelDescription(String modelDescription) {
        this.modelDescription = modelDescription;
    }
    
    public byte[] getModelImage() {
        return modelImage;
    }
    
    public void setModelImage(byte[] modelImage) {
        this.modelImage = modelImage;
    }
    
    public String getVersionName() {
        return versionName;
    }
    
    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }
    
    public String getEngine() {
        return engine;
    }
    
    public void setEngine(String engine) {
        this.engine = engine;
    }
    
    public String getTransmission() {
        return transmission;
    }
    
    public void setTransmission(String transmission) {
        this.transmission = transmission;
    }
    
    public String getColorName() {
        return colorName;
    }
    
    public void setColorName(String colorName) {
        this.colorName = colorName;
    }
    
    public Integer getColorID() {
        return colorID;
    }
    
    public void setColorID(Integer colorID) {
        this.colorID = colorID;
    }
    
    public Integer getManufactureYear() {
        return manufactureYear;
    }
    
    public void setManufactureYear(Integer manufactureYear) {
        this.manufactureYear = manufactureYear;
    }
    
    public String getVin() {
        return vin;
    }
    
    public void setVin(String vin) {
        this.vin = vin;
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
    
    public BigDecimal getBasePrice() {
        return basePrice;
    }
    
    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }
    
    public BigDecimal getDealerSellingPrice() {
        return dealerSellingPrice;
    }
    
    public void setDealerSellingPrice(BigDecimal dealerSellingPrice) {
        this.dealerSellingPrice = dealerSellingPrice;
    }
    
    public BigDecimal getFinalPrice() {
        return finalPrice;
    }
    
    public void setFinalPrice(BigDecimal finalPrice) {
        this.finalPrice = finalPrice;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public String getDetailUrl() {
        return detailUrl;
    }
    
    public void setDetailUrl(String detailUrl) {
        this.detailUrl = detailUrl;
    }
    
    public Map<String, Object> getAdditionalSpecs() {
        return additionalSpecs;
    }
    
    public void setAdditionalSpecs(Map<String, Object> additionalSpecs) {
        this.additionalSpecs = additionalSpecs;
    }
    
    public String getDisplayName() {
        StringBuilder sb = new StringBuilder();
        if (brand != null) sb.append(brand).append(" ");
        if (modelName != null) sb.append(modelName).append(" ");
        if (versionName != null) sb.append(versionName);
        return sb.toString().trim();
    }
    
    public boolean isAvailable() {
        return status != null && (status == VehicleStatus.IN_STOCK);
    }
}

