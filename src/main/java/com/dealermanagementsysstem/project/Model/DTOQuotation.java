package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;

import java.sql.Timestamp;
import java.util.List;

@Entity
@Table(name = "Quotation")
@SuppressWarnings({"JpaDataSourceORMInspection", "unused"})
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

    // ===== MÃ GIẢM GIÁ CỦA HÃNG (MANUFACTURER PROMO CODE) - Cấp toàn báo giá =====
    @Column(name = "PromoCode")
    private String promoCode; // Mã giảm giá áp dụng cho toàn bộ quotation

    @ManyToOne
    @JoinColumn(name = "PromoPolicyID", referencedColumnName = "PolicyID")
    private DTODiscountPolicy promoPolicy; // Reference đến DiscountPolicy

    public DTOQuotation() {
    }

    public DTOQuotation(int quotationID, DTODealer dealer, DTOCustomer customer,
                        Timestamp createdAt, QuotationStatus status, double totalPrice,
                        int quantity, int levelID, DTODealerStaff staff) {
        this.quotationID = quotationID;
        this.dealer = dealer;
        this.customer = customer;
        this.createdAt = createdAt;
        this.status = status;
        this.totalPrice = totalPrice;
        this.quantity = quantity;
        this.levelID = levelID;
        this.staff = staff;
    }

    public int getQuotationID() {
        return quotationID;
    }

    public void setQuotationID(int quotationID) {
        this.quotationID = quotationID;
    }

    public DTODealer getDealer() {
        return dealer;
    }

    public void setDealer(DTODealer dealer) {
        this.dealer = dealer;
    }

    public DTOCustomer getCustomer() {
        return customer;
    }

    public void setCustomer(DTOCustomer customer) {
        this.customer = customer;
    }


    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public QuotationStatus getStatus() {
        return status;
    }

    public void setStatus(QuotationStatus status) {
        this.status = status;
    }


    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public List<DTOQuotationDetail> getQuotationDetails() {
        return quotationDetails;
    }

    public void setQuotationDetails(List<DTOQuotationDetail> quotationDetails) {
        this.quotationDetails = quotationDetails;
    }

    // Extended fields accessors
    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getLevelID() {
        return levelID;
    }

    public void setLevelID(int levelID) {
        this.levelID = levelID;
    }

    public DTODealerStaff getStaff() {
        return staff;
    }

    public void setStaff(DTODealerStaff staff) {
        this.staff = staff;
    }

    public Double getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(Double discountPercent) {
        this.discountPercent = discountPercent;
    }

    // ===== PROMO CODE GETTERS/SETTERS =====
    public String getPromoCode() {
        return promoCode;
    }

    public void setPromoCode(String promoCode) {
        this.promoCode = promoCode;
    }

    public DTODiscountPolicy getPromoPolicy() {
        return promoPolicy;
    }

    public void setPromoPolicy(DTODiscountPolicy promoPolicy) {
        this.promoPolicy = promoPolicy;
    }

    /**
     * Tính tổng giá trị báo giá sau khi áp dụng promo code
     */
    @Transient
    public java.math.BigDecimal getTotalAfterPromo() {
        if (quotationDetails == null || quotationDetails.isEmpty()) {
            return java.math.BigDecimal.ZERO;
        }

        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        for (DTOQuotationDetail detail : quotationDetails) {
            total = total.add(detail.getNetAfterAllDiscounts());
        }

        return total;
    }

    /**
     * Tính tổng tiền giảm từ promo code
     */
    @Transient
    public java.math.BigDecimal getTotalPromoDiscount() {
        if (quotationDetails == null || quotationDetails.isEmpty()) {
            return java.math.BigDecimal.ZERO;
        }

        java.math.BigDecimal totalDiscount = java.math.BigDecimal.ZERO;
        for (DTOQuotationDetail detail : quotationDetails) {
            totalDiscount = totalDiscount.add(detail.getPromoDiscountTotal());
        }

        return totalDiscount;
    }

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
