package com.dealermanagementsysstem.project.Model;

import java.math.BigDecimal;

public class DTOSaleOrderDetail {
    private int saleOrderDetailID; // ✅ FIX: Renamed from soDetailID to match database
    private int saleOrderID;
    private int quotationID; // ✅ Reference to source quotation

    // Vehicle information (can be embedded or separate)
    private String VIN;
    private int colorID;
    private String colorName;
    private String modelName;
    private int manufactureYear;

    private DTOVehicle vehicle; // Optional: full vehicle object
    private BigDecimal price;
    private int quantity;
    public DTOSaleOrderDetail() {
    }

    public DTOSaleOrderDetail(int saleOrderDetailID, int saleOrderID, String VIN,
                              BigDecimal price, int quantity, int quotationID, int colorID) {
        this.saleOrderDetailID = saleOrderDetailID;
        this.saleOrderID = saleOrderID;
        this.VIN = VIN;
        this.price = price;
        this.quantity = quantity;
        this.quotationID = quotationID;
        this.colorID = colorID;
    }

    // ✅ Primary getters/setters
    public int getSaleOrderDetailID() {
        return saleOrderDetailID;
    }

    public void setSaleOrderDetailID(int saleOrderDetailID) {
        this.saleOrderDetailID = saleOrderDetailID;
    }

    // Legacy support
    @Deprecated
    public int getSoDetailID() {
        return saleOrderDetailID;
    }

    @Deprecated
    public void setSoDetailID(int soDetailID) {
        this.saleOrderDetailID = soDetailID;
    }

    public int getSaleOrderID() {
        return saleOrderID;
    }

    public void setSaleOrderID(int saleOrderID) {
        this.saleOrderID = saleOrderID;
    }

    public int getQuotationID() {
        return quotationID;
    }

    public void setQuotationID(int quotationID) {
        this.quotationID = quotationID;
    }

    public String getVIN() {
        return VIN;
    }

    public void setVIN(String VIN) {
        this.VIN = VIN;
    }

    public int getColorID() {
        return colorID;
    }

    public void setColorID(int colorID) {
        this.colorID = colorID;
    }

    public String getColorName() {
        return colorName;
    }

    public void setColorName(String colorName) {
        this.colorName = colorName;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public int getManufactureYear() {
        return manufactureYear;
    }

    public void setManufactureYear(int manufactureYear) {
        this.manufactureYear = manufactureYear;
    }

    // ✅ FIX: Add missing getters/setters for quantity and price
    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public DTOVehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(DTOVehicle vehicle) {
        this.vehicle = vehicle;
    }
}
