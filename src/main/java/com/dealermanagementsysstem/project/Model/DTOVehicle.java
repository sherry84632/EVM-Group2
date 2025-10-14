package com.dealermanagementsysstem.project.Model;

import java.math.BigDecimal;

public class DTOVehicle {

    private String VIN;
    private int manufactureYear;
    private int colorID;
    private String engineNumber;
    private String currentOwner;
    private String status;
    private int modelID;
    private String modelName;
    private BigDecimal basePrice;
    private String colorName;

    public DTOVehicle() {
    }

    public DTOVehicle(String VIN, int manufactureYear, int colorID, String engineNumber,
                      String currentOwner, String status, int modelID, String modelName,
                      BigDecimal basePrice, String colorName) {
        this.VIN = VIN;
        this.manufactureYear = manufactureYear;
        this.colorID = colorID;
        this.engineNumber = engineNumber;
        this.currentOwner = currentOwner;
        this.status = status;
        this.modelID = modelID;
        this.modelName = modelName;
        this.basePrice = basePrice;
        this.colorName = colorName;
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

    public int getColorID() {
        return colorID;
    }

    public void setColorID(int colorID) {
        this.colorID = colorID;
    }

    public String getEngineNumber() {
        return engineNumber;
    }

    public void setEngineNumber(String engineNumber) {
        this.engineNumber = engineNumber;
    }

    public String getCurrentOwner() {
        return currentOwner;
    }

    public void setCurrentOwner(String currentOwner) {
        this.currentOwner = currentOwner;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getColorName() {
        return colorName;
    }

    public void setColorName(String colorName) {
        this.colorName = colorName;
    }
}
