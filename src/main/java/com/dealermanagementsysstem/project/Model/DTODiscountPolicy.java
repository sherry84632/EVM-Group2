package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

/**
 * DTODiscountPolicy - REFACTORED TO CUSTOMER PROMOTION CODE
 *
 * This represents promotional discount codes/coupons that customers can use
 * when purchasing vehicles (e.g., "SUMMER2024", "NEWCAR15", "VIP10OFF")
 *
 * NOT for manufacturer-dealer commission splits (use ManufacturerDealerPolicy for that)
 */
@Entity
@Table(name = "DiscountPolicy")
@SuppressWarnings({"JpaDataSourceORMInspection", "unused", "deprecation"})
public class DTODiscountPolicy {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PolicyID")
    private int policyID;
    
    @Column(name = "PolicyName")
    private String policyName; // Promo name (e.g., "Summer Sale 2024")

    @Column(name = "PromoCode")
    private String promoCode; // Code customers enter (e.g., "SUMMER2024")

    @Column(name = "Description")
    private String description;
    
    @Column(name = "DiscountPercent")
    private BigDecimal discountPercent; // Discount % for customers (e.g., 15%)

    @Column(name = "DiscountAmount")
    private BigDecimal discountAmount; // Fixed discount (optional, e.g., 5,000,000 VND)

    @Column(name = "MinPurchaseAmount")
    private BigDecimal minPurchaseAmount; // Minimum purchase to apply (optional)

    @Column(name = "MaxDiscountAmount")
    private BigDecimal maxDiscountAmount; // Maximum discount cap (optional)

    @Column(name = "UsageLimit")
    private Integer usageLimit; // Max uses (null = unlimited)

    @Column(name = "UsedCount")
    private Integer usedCount; // Times already used

    @Column(name = "ApplicableToModels")
    private String applicableToModels; // Comma-separated ModelIDs (null = all)

    @Column(name = "StartDate")
    private LocalDate startDate;
    
    @Column(name = "EndDate")
    private LocalDate endDate;
    
    @Column(name = "Status")
    @Enumerated(EnumType.STRING)
    private DiscountPolicyStatus status;
    
    @Column(name = "CreatedAt")
    private Date creationDate;
    
    @Column(name = "CreatedBy")
    private String createdBy; // Who created this promo (ADMIN/EVMSTAFF)

    // DEPRECATED FIELDS - Keep for backward compatibility
    @Deprecated
    @Column(name = "HangPercent")
    private BigDecimal hangPercent;

    @Deprecated
    @Column(name = "DailyPercent")
    private BigDecimal dailyPercent;

    @Deprecated
    @Column(name = "DealerID")
    private Integer dealerID;

    @Deprecated
    @Column(name = "LevelID")
    private Integer levelID;

    public DTODiscountPolicy() {}

    // GETTERS / SETTERS
    public int getPolicyID() { return policyID; }
    public void setPolicyID(int policyID) { this.policyID = policyID; }

    public String getPolicyName() { return policyName; }
    public void setPolicyName(String policyName) { this.policyName = policyName; }

    public String getPromoCode() { return promoCode; }
    public void setPromoCode(String promoCode) { this.promoCode = promoCode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }

    public BigDecimal getMinPurchaseAmount() { return minPurchaseAmount; }
    public void setMinPurchaseAmount(BigDecimal minPurchaseAmount) { this.minPurchaseAmount = minPurchaseAmount; }

    public BigDecimal getMaxDiscountAmount() { return maxDiscountAmount; }
    public void setMaxDiscountAmount(BigDecimal maxDiscountAmount) { this.maxDiscountAmount = maxDiscountAmount; }

    public Integer getUsageLimit() { return usageLimit; }
    public void setUsageLimit(Integer usageLimit) { this.usageLimit = usageLimit; }

    public Integer getUsedCount() { return usedCount; }
    public void setUsedCount(Integer usedCount) { this.usedCount = usedCount; }

    public String getApplicableToModels() { return applicableToModels; }
    public void setApplicableToModels(String applicableToModels) { this.applicableToModels = applicableToModels; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public DiscountPolicyStatus getStatus() { return status; }
    public void setStatus(DiscountPolicyStatus status) { this.status = status; }

    public Date getCreationDate() { return creationDate; }
    public void setCreationDate(Date creationDate) { this.creationDate = creationDate; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    // DEPRECATED GETTERS/SETTERS
    @Deprecated
    public BigDecimal getHangPercent() { return hangPercent; }
    @Deprecated
    public void setHangPercent(BigDecimal hangPercent) { this.hangPercent = hangPercent; }

    @Deprecated
    public BigDecimal getDailyPercent() { return dailyPercent; }
    @Deprecated
    public void setDailyPercent(BigDecimal dailyPercent) { this.dailyPercent = dailyPercent; }

    @Deprecated
    public Integer getDealerID() { return dealerID; }
    @Deprecated
    public void setDealerID(Integer dealerID) { this.dealerID = dealerID; }

    @Deprecated
    public Integer getLevelID() { return levelID; }
    @Deprecated
    public void setLevelID(Integer levelID) { this.levelID = levelID; }

    // HELPER METHODS

    /**
     * Check if promo code is still valid
     */
    public boolean isValid() {
        if (status != DiscountPolicyStatus.ACTIVE) return false;

        LocalDate now = LocalDate.now();
        if (startDate != null && now.isBefore(startDate)) return false;
        if (endDate != null && now.isAfter(endDate)) return false;

        if (usageLimit != null && usedCount != null && usedCount >= usageLimit) return false;

        return true;
    }

    /**
     * Check if promo applies to a specific model
     */
    public boolean appliesToModel(int modelId) {
        if (applicableToModels == null || applicableToModels.trim().isEmpty()) {
            return true; // Applies to all models
        }

        String[] modelIds = applicableToModels.split(",");
        for (String id : modelIds) {
            if (id.trim().equals(String.valueOf(modelId))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Calculate actual discount for a purchase amount
     */
    public BigDecimal calculateDiscount(BigDecimal purchaseAmount) {
        if (purchaseAmount == null || purchaseAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Check minimum purchase
        if (minPurchaseAmount != null && purchaseAmount.compareTo(minPurchaseAmount) < 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = BigDecimal.ZERO;

        // Calculate discount
        if (discountPercent != null && discountPercent.compareTo(BigDecimal.ZERO) > 0) {
            discount = purchaseAmount.multiply(discountPercent).divide(new BigDecimal("100"));
        } else if (discountAmount != null && discountAmount.compareTo(BigDecimal.ZERO) > 0) {
            discount = discountAmount;
        }

        // Apply cap if exists
        if (maxDiscountAmount != null && discount.compareTo(maxDiscountAmount) > 0) {
            discount = maxDiscountAmount;
        }

        return discount;
    }

}
