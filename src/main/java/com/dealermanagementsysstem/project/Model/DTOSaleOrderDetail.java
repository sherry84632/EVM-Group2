package com.dealermanagementsysstem.project.Model;

import java.math.BigDecimal;

public class DTOSaleOrderDetail {
    private int soDetailID;
    private int saleOrderID;
    private DTOVehicle vehicle;
    private BigDecimal price;

    public int getSoDetailID() { return soDetailID; }
    public void setSoDetailID(int soDetailID) { this.soDetailID = soDetailID; }

    public int getSaleOrderID() { return saleOrderID; }
    public void setSaleOrderID(int saleOrderID) { this.saleOrderID = saleOrderID; }

    public DTOVehicle getVehicle() { return vehicle; }
    public void setVehicle(DTOVehicle vehicle) { this.vehicle = vehicle; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}
