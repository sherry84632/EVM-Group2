package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "QuotationDetail")
@SuppressWarnings({"JpaDataSourceORMInspection", "unused"})
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

    @Column(name = "AppliedDealerDiscountPercent")
    private Double appliedDealerDiscountPercent; // line-level dealer promotion percent (if model matches)

    // ===== MÃ GIẢM GIÁ CỦA HÃNG (MANUFACTURER PROMO CODE) =====
    @Column(name = "PromoCode")
    private String promoCode; // Mã giảm giá của hãng áp dụng cho dòng này (e.g., "SUMMER2024")

    @Column(name = "PromoDiscountPercent")
    private Double promoDiscountPercent; // % giảm giá từ promo code

    @Column(name = "PromoDiscountAmount")
    private BigDecimal promoDiscountAmount; // Số tiền giảm cố định từ promo code

    @ManyToOne
    @JoinColumn(name = "PromoPolicyID", referencedColumnName = "PolicyID")
    private DTODiscountPolicy promoPolicy; // Reference đến bảng DiscountPolicy

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

    // ===== PROMO CODE GETTERS/SETTERS =====
    public String getPromoCode() {
        return promoCode;
    }

    public void setPromoCode(String promoCode) {
        this.promoCode = promoCode;
    }

    public Double getPromoDiscountPercent() {
        return promoDiscountPercent;
    }

    public void setPromoDiscountPercent(Double promoDiscountPercent) {
        this.promoDiscountPercent = promoDiscountPercent;
    }

    public java.math.BigDecimal getPromoDiscountAmount() {
        return promoDiscountAmount;
    }

    public void setPromoDiscountAmount(java.math.BigDecimal promoDiscountAmount) {
        this.promoDiscountAmount = promoDiscountAmount;
    }

    public DTODiscountPolicy getPromoPolicy() {
        return promoPolicy;
    }

    public void setPromoPolicy(DTODiscountPolicy promoPolicy) {
        this.promoPolicy = promoPolicy;
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

    /**
     * Tính giá cuối cùng sau khi áp dụng MÃ GIẢM GIÁ CỦA HÃNG
     * Áp dụng theo thứ tự:
     * 1. Dealer discount (appliedDealerDiscountPercent)
     * 2. Manufacturer promo code (promoCode)
     */
    @Transient
    public java.math.BigDecimal getNetAfterAllDiscounts() {
        // Bước 1: Tính giá sau dealer discount
        java.math.BigDecimal afterDealerDiscount = getNetAfterLineDiscount();
        
        // Bước 2: Áp dụng manufacturer promo code
        java.math.BigDecimal promoDiscount = java.math.BigDecimal.ZERO;
        
        if (promoDiscountPercent != null && promoDiscountPercent > 0) {
            // Discount theo phần trăm
            promoDiscount = afterDealerDiscount.multiply(
                java.math.BigDecimal.valueOf(promoDiscountPercent / 100.0)
            );
        } else if (promoDiscountAmount != null && promoDiscountAmount.compareTo(java.math.BigDecimal.ZERO) > 0) {
            // Discount cố định (fixed amount)
            promoDiscount = promoDiscountAmount.multiply(java.math.BigDecimal.valueOf(quantity));
        }
        
        java.math.BigDecimal finalPrice = afterDealerDiscount.subtract(promoDiscount);
        
        // Đảm bảo giá không âm
        if (finalPrice.compareTo(java.math.BigDecimal.ZERO) < 0) {
            finalPrice = java.math.BigDecimal.ZERO;
        }
        
        return finalPrice;
    }

    /**
     * Tính tổng tiền giảm từ promo code
     */
    @Transient
    public java.math.BigDecimal getPromoDiscountTotal() {
        java.math.BigDecimal afterDealerDiscount = getNetAfterLineDiscount();
        java.math.BigDecimal afterPromo = getNetAfterAllDiscounts();
        return afterDealerDiscount.subtract(afterPromo);
    }

    public java.math.BigDecimal getFinalNetAfterAll() {
        return finalNetAfterAll;
    }

    public void setFinalNetAfterAll(java.math.BigDecimal v) {
        this.finalNetAfterAll = v;
    }
}
