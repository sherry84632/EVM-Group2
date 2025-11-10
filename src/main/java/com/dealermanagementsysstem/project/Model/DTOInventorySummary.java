package com.dealermanagementsysstem.project.Model;

import java.math.BigDecimal;

public class DTOInventorySummary {
    private int total;
    private int inStock;
    private int reserved;
    private int sold;
    private int transferred;
    private BigDecimal inventoryValue;

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public int getInStock() { return inStock; }
    public void setInStock(int inStock) { this.inStock = inStock; }
    public int getReserved() { return reserved; }
    public void setReserved(int reserved) { this.reserved = reserved; }
    public int getSold() { return sold; }
    public void setSold(int sold) { this.sold = sold; }
    public int getTransferred() { return transferred; }
    public void setTransferred(int transferred) { this.transferred = transferred; }
    public BigDecimal getInventoryValue() { return inventoryValue; }
    public void setInventoryValue(BigDecimal inventoryValue) { this.inventoryValue = inventoryValue; }
}


