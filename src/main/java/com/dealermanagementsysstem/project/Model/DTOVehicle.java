package com.dealermanagementsysstem.project.Model;

import java.math.BigDecimal;

public class DTOVehicle {
    private String VIN;
    private int ManufactureYear;
    private int ColorID;
    private String EngineNumber;
    private String CurrentOwner;
    private String status;
    private int modelID;
    private String modelName;
    private java.math.BigDecimal basePrice;

    private int colorID;
    private String colorName;

    public DTOVehicle() {
    }

    public DTOVehicle(String VIN, int manufactureYear, int colorID, String engineNumber, String currentOwner, String status, int modelID, String modelName, BigDecimal basePrice, int colorID1, String colorName) {
        this.VIN = VIN;
        ManufactureYear = manufactureYear;
        ColorID = colorID;
        EngineNumber = engineNumber;
        CurrentOwner = currentOwner;
        this.status = status;
        this.modelID = modelID;
        this.modelName = modelName;
        this.basePrice = basePrice;
        this.colorID = colorID1;
        this.colorName = colorName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCurrentOwner() {
        return CurrentOwner;
    }

    public void setCurrentOwner(String currentOwner) {
        CurrentOwner = currentOwner;
    }

    public String getEngineNumber() {
        return EngineNumber;
    }

    public void setEngineNumber(String engineNumber) {
        EngineNumber = engineNumber;
    }

    public int getColorID() {
        return ColorID;
    }

    public void setColorID(int colorID) {
        ColorID = colorID;
    }

    public int getManufactureYear() {
        return ManufactureYear;
    }

    public void setManufactureYear(int manufactureYear) {
        ManufactureYear = manufactureYear;
    }

    public String getVIN() {
        return VIN;
    }

    public void setVIN(String VIN) {
        this.VIN = VIN;
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
