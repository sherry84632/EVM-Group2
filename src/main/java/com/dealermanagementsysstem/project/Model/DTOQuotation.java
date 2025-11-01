package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;


@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "Quotation")
public class DTOQuotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int quotationID;

    @ManyToOne
    @JoinColumn(name = "DealerID", referencedColumnName = "DealerID")
    private DTODealer dealer;

    @ManyToOne
    @JoinColumn(name = "CustomerID", referencedColumnName = "CustomerID")
    private DTOCustomer customer;

    @Column(name = "CreatedAt")
    private Timestamp createdAt;

    @Column(name = "Status")
    @Enumerated(EnumType.STRING)
    private QuotationStatus status;

    @Column(name = "TotalAmount")
    private double totalPrice;

    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL)
    private List<DTOQuotationDetail> quotationDetails;

    // New fields for extended pricing logic
    @Column(name = "Quantity")
    private int quantity = 1; // default single unit

    @Column(name = "LevelID")
    private int levelID;

    @ManyToOne
    @JoinColumn(name = "StaffID", referencedColumnName = "StaffID")
    private DTODealerStaff staff; // staff who created the quotation

    @Column(name = "DiscountPercent")
    private Double discountPercent; // nullable discount applied to whole quotation


    @Transient
    public DTOQuotationDetail getFirstDetail() {
        return (quotationDetails != null && !quotationDetails.isEmpty()) ? quotationDetails.get(0) : null;
    }

    @Transient
    public String getFirstModelName() {
        DTOQuotationDetail d = getFirstDetail();
        return d != null ? d.getModelName() : null;
    }

    @Transient
    public String getFirstVersionName() {
        DTOQuotationDetail d = getFirstDetail();
        return d != null ? d.getVersionName() : null;
    }

    @Transient
    public String getFirstColorName() {
        DTOQuotationDetail d = getFirstDetail();
        return d != null ? d.getColorName() : null;
    }

    @Transient
    public java.math.BigDecimal getFirstUnitPrice() {
        DTOQuotationDetail d = getFirstDetail();
        return d != null ? d.getUnitPrice() : java.math.BigDecimal.ZERO;
    }

    @Transient
    public double getCalculatedTotal() {
        java.math.BigDecimal unit = getFirstUnitPrice();
        return unit.doubleValue() * Math.max(1, quantity);
    }

    @Transient
    public double getEffectiveDiscountPercent() {
        return discountPercent != null ? discountPercent : 0.0;
    }

    @Transient
    public double getGrossTotal() {
        if (quotationDetails == null) return 0.0;
        return quotationDetails.stream().mapToDouble(d -> d.getSubtotal().doubleValue()).sum();
    }

    @Transient
    public double getNetTotal() {
        double gross = getGrossTotal();
        double dp = getEffectiveDiscountPercent();
        return gross * (1 - dp / 100.0);
    }
}
