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

    @Transient
    private Double baseQuotationDiscountPercent; // new transient percent from parent quotation
    @Transient
    private java.math.BigDecimal baseQuotationDiscountAmount; // computed per line after manufacturer discount

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

    public Double getBaseQuotationDiscountPercent() { return baseQuotationDiscountPercent; }
    public void setBaseQuotationDiscountPercent(Double pct) { this.baseQuotationDiscountPercent = pct; }
    public java.math.BigDecimal getBaseQuotationDiscountAmount() { return baseQuotationDiscountAmount; }
    public void setBaseQuotationDiscountAmount(java.math.BigDecimal v) { this.baseQuotationDiscountAmount = v; }

    // Helper: safe percent
    private double safePct(Double v){ return (v!=null && v>0)? v : 0.0; }

    @Transient
    public java.math.BigDecimal getDealerDiscountAmount(){
        double pct = safePct(appliedDealerDiscountPercent);
        java.math.BigDecimal sub = getSubtotal();
        return pct>0? sub.multiply(java.math.BigDecimal.valueOf(pct/100.0)) : java.math.BigDecimal.ZERO;
    }
    @Transient
    public java.math.BigDecimal getAfterDealer(){
        return getSubtotal().subtract(getDealerDiscountAmount());
    }
    @Transient
    public java.math.BigDecimal getManufacturerDiscountAmount(){
        java.math.BigDecimal afterDealer = getAfterDealer();
        if (afterDealer.compareTo(java.math.BigDecimal.ZERO)<=0) return java.math.BigDecimal.ZERO;
        double promoPct = safePct(promoDiscountPercent);
        if (promoPct>0){
            return afterDealer.multiply(java.math.BigDecimal.valueOf(promoPct/100.0));
        }
        if (promoDiscountAmount!=null && promoDiscountAmount.compareTo(java.math.BigDecimal.ZERO)>0){
            // treat promoDiscountAmount as total for the line if originally stored per line; clamp
            java.math.BigDecimal fixed = promoDiscountAmount.multiply(java.math.BigDecimal.valueOf(Math.max(1, quantity)));
            return fixed.min(afterDealer);
        }
        return java.math.BigDecimal.ZERO;
    }
    @Transient
    public java.math.BigDecimal getAfterManufacturer(){
        return getAfterDealer().subtract(getManufacturerDiscountAmount());
    }
    @Transient
    public java.math.BigDecimal getBaseQuotationDiscountAmountComputed(){
        java.math.BigDecimal afterManufacturer = getAfterManufacturer();
        double basePct = safePct(baseQuotationDiscountPercent);
        if (afterManufacturer.compareTo(java.math.BigDecimal.ZERO)<=0 || basePct<=0) return java.math.BigDecimal.ZERO;
        return afterManufacturer.multiply(java.math.BigDecimal.valueOf(basePct/100.0));
    }
    @Transient
    public java.math.BigDecimal getNetAfterFullStack(){
        java.math.BigDecimal net = getAfterManufacturer().subtract(getBaseQuotationDiscountAmountComputed());
        if (net.compareTo(java.math.BigDecimal.ZERO)<0) net = java.math.BigDecimal.ZERO;
        return net;
    }

    /**
     * Tính giá cuối cùng sau khi áp dụng MÃ GIẢM GIÁ CỦA HÃNG
     * Áp dụng theo thứ tự:
     * 1. Dealer discount (appliedDealerDiscountPercent)
     * 2. Manufacturer promo code (promoCode)
     */
    @Transient
    public java.math.BigDecimal getNetAfterAllDiscounts() {
        java.math.BigDecimal afterDealerDiscount = getAfterDealer();
        java.math.BigDecimal promoDiscount = getManufacturerDiscountAmount();
        java.math.BigDecimal finalPrice = afterDealerDiscount.subtract(promoDiscount);
        if (finalPrice.compareTo(java.math.BigDecimal.ZERO) < 0) finalPrice = java.math.BigDecimal.ZERO;
        return finalPrice;
    }

    /**
     * Tính tổng tiền giảm từ promo code
     */
    @Transient
    public java.math.BigDecimal getPromoDiscountTotal() {
        return getManufacturerDiscountAmount();
    }

    public java.math.BigDecimal getFinalNetAfterAll() {
        return finalNetAfterAll;
    }

    public void setFinalNetAfterAll(java.math.BigDecimal v) {
        this.finalNetAfterAll = v;
    }
}
