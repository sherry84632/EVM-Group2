package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "DealerLevel")
public class DTODealerLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LevelID")
    private int levelID;

    @Column(name = "LevelName")
    private String levelName;

    @Column(name = "MinOrderValue")
    private java.math.BigDecimal minOrderValue;

    @Column(name = "MaxOrderValue")
    private java.math.BigDecimal maxOrderValue;

    @Transient
    private Double discountSharePercent; // computed, not stored

    @Column(name = "VehiclesRequired")
    private int vehiclesRequired; // number of vehicles sold needed to reach level
    @Column(name = "SharePercent")
    private java.math.BigDecimal sharePercent; // configurable percent share

    public DTODealerLevel() {
    }

    public int getLevelID() {
        return levelID;
    }

    public void setLevelID(int levelID) {
        this.levelID = levelID;
    }

    public String getLevelName() {
        return levelName;
    }

    public void setLevelName(String levelName) {
        this.levelName = levelName;
    }

    public java.math.BigDecimal getMinOrderValue() {
        return minOrderValue;
    }

    public void setMinOrderValue(java.math.BigDecimal minOrderValue) {
        this.minOrderValue = minOrderValue;
    }

    public java.math.BigDecimal getMaxOrderValue() {
        return maxOrderValue;
    }

    public void setMaxOrderValue(java.math.BigDecimal maxOrderValue) {
        this.maxOrderValue = maxOrderValue;
    }

    public int getVehiclesRequired() {
        return vehiclesRequired;
    }

    public void setVehiclesRequired(int vehiclesRequired) {
        this.vehiclesRequired = vehiclesRequired;
    }

    public java.math.BigDecimal getSharePercent() {
        return sharePercent;
    }

    public void setSharePercent(java.math.BigDecimal sharePercent) {
        this.sharePercent = sharePercent;
    }

    public Double getDiscountSharePercent() {
        // Prefer explicit sharePercent if provided
        if (sharePercent != null) return sharePercent.doubleValue();
        if (discountSharePercent != null) return discountSharePercent;
        if (levelName == null) return 0.0;
        String n = levelName.toLowerCase();
        double pct = 0.0;
        if (n.contains("platinum")) pct = 12.0;
        else if (n.contains("gold")) pct = 9.0;
        else if (n.contains("silver")) pct = 7.0;
        else if (n.contains("bronze")) pct = 5.0;
        discountSharePercent = pct;
        return discountSharePercent;
    }

    @Transient
    public Double getRewardPercent() { // alias for share percent renamed in UI
        return getDiscountSharePercent();
    }
}
