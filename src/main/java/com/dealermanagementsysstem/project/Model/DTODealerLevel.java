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
}

