package com.dealermanagementsysstem.project.model;

public class DTOVehicle {
    private String VIN;
    private int ManufactureYear;
    private int ColorID;
    private String EngineNumber;
    private String CurrentOwner;
    private String status;

    public DTOVehicle() {
    }

    public DTOVehicle(String VIN, String status, int manufactureYear, int colorID, String engineNumber, String currentOwner) {
        this.VIN = VIN;
        this.status = status;
        ManufactureYear = manufactureYear;
        ColorID = colorID;
        EngineNumber = engineNumber;
        CurrentOwner = currentOwner;
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
}
