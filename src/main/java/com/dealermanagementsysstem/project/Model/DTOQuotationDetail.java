package com.dealermanagementsysstem.project.Model;



import java.math.BigDecimal;

public class DTOQuotationDetail {
    private int quotationDetailId;
    private int quotationId;
    private int colorId;
    private int quantity;
    private BigDecimal unitPrice;
    private String vin; // optional per schema

    public int getQuotationDetailId() { return quotationDetailId; }
    public void setQuotationDetailId(int quotationDetailId) { this.quotationDetailId = quotationDetailId; }

    public int getQuotationId() { return quotationId; }
    public void setQuotationId(int quotationId) { this.quotationId = quotationId; }

    public int getColorId() { return colorId; }
    public void setColorId(int colorId) { this.colorId = colorId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }
}


