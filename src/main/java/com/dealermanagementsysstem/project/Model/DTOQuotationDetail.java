package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Data
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
        return sub.multiply(java.math.BigDecimal.valueOf(1 - pct / 100.0));
    }

    public java.math.BigDecimal getFinalNetAfterAll() {
        return finalNetAfterAll;
    }

    public void setFinalNetAfterAll(java.math.BigDecimal v) {
        this.finalNetAfterAll = v;
    }
}
