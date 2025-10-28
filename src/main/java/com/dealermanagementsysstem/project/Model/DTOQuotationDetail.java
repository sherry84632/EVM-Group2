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
}
