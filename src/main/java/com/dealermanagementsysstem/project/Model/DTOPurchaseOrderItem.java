package com.dealermanagementsysstem.project.Model;



import java.math.BigDecimal;

public class DTOPurchaseOrderItem {
    private int purchaseOrderItemId;
    private int purchaseOrderId;
    private String vehicleVin;
    private int quantity;
    private BigDecimal unitPrice;
    private Double discountPercent;

    public int getPurchaseOrderItemId() { return purchaseOrderItemId; }
    public void setPurchaseOrderItemId(int purchaseOrderItemId) { this.purchaseOrderItemId = purchaseOrderItemId; }

    public int getPurchaseOrderId() { return purchaseOrderId; }
    public void setPurchaseOrderId(int purchaseOrderId) { this.purchaseOrderId = purchaseOrderId; }

    public String getVehicleVin() { return vehicleVin; }
    public void setVehicleVin(String vehicleVin) { this.vehicleVin = vehicleVin; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public Double getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(Double discountPercent) { this.discountPercent = discountPercent; }
}


