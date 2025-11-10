package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "QuotationDetail")
public class DTOQuotationDetail {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "QuotationDetailID")
    private int quotationDetailID;
    
    @ManyToOne
    @JoinColumn(name = "QuotationID", referencedColumnName = "QuotationID")
    private DTOQuotation quotation;
    
    @ManyToOne
    @JoinColumn(name = "VersionID", referencedColumnName = "VersionID")
    private DTOVehicleVersion version;
    
    @Column(name = "UnitPrice")
    private BigDecimal unitPrice;
    
    @ManyToOne
    @JoinColumn(name = "ColorID", referencedColumnName = "ColorID")
    private DTOVehicleColor color;

    @Column(name = "Quantity")
    private int quantity = 1;

    @Transient
    private Double appliedDealerDiscountPercent; // line-level dealer promotion percent (if model matches)

    @Transient
    private java.math.BigDecimal finalNetAfterAll; // line net after line-level + base discount stacking

    public DTOQuotationDetail() {
    }

    public DTOQuotationDetail(int quotationDetailID, DTOQuotation quotation, DTOVehicleVersion version, 
                             BigDecimal unitPrice, DTOVehicleColor color) {
        this.quotationDetailID = quotationDetailID;
        this.quotation = quotation;
        this.version = version;
        this.unitPrice = unitPrice;
        this.color = color;
    }

    public int getQuotationDetailID() {
        return quotationDetailID;
    }

    public void setQuotationDetailID(int quotationDetailID) {
        this.quotationDetailID = quotationDetailID;
    }

    public DTOQuotation getQuotation() {
        return quotation;
    }

    public void setQuotation(DTOQuotation quotation) {
        this.quotation = quotation;
    }

    public DTOVehicleVersion getVersion() {
        return version;
    }

    public void setVersion(DTOVehicleVersion version) {
        this.version = version;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public DTOVehicleColor getColor() {
        return color;
    }

    public void setColor(DTOVehicleColor color) {
        this.color = color;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Double getAppliedDealerDiscountPercent() {
        return appliedDealerDiscountPercent;
    }

    public void setAppliedDealerDiscountPercent(Double appliedDealerDiscountPercent) {
        this.appliedDealerDiscountPercent = appliedDealerDiscountPercent;
    }

    public BigDecimal getSubtotal() {
        return unitPrice != null ? unitPrice.multiply(BigDecimal.valueOf(Math.max(1, quantity))) : BigDecimal.ZERO;
    }

    public String getVersionName() {
        return version != null ? version.getVersionName() : null;
    }

    public String getModelName() {
        return (version != null && version.getModel() != null) ? version.getModel().getModelName() : null;
    }

    public String getColorName() {
        return color != null ? color.getColorName() : null;
    }

    @Transient
    public java.math.BigDecimal getNetAfterLineDiscount() {
        java.math.BigDecimal sub = getSubtotal();
        double pct = appliedDealerDiscountPercent != null ? appliedDealerDiscountPercent : 0.0;
        return sub.multiply(java.math.BigDecimal.valueOf(1 - pct/100.0));
    }

    public java.math.BigDecimal getFinalNetAfterAll() {
        return finalNetAfterAll;
    }

    public void setFinalNetAfterAll(java.math.BigDecimal v) {
        this.finalNetAfterAll = v;
    }
}
