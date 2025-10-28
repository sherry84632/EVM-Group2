package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

@Entity
@Table(name = "DiscountPolicy")
public class DTODiscountPolicy {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PolicyID")
    private int policyID;
    
    @ManyToOne
    @JoinColumn(name = "DealerID", referencedColumnName = "DealerID")
    private DTODealer dealer;
    
    @Column(name = "PolicyName")
    private String policyName;
    
    @Column(name = "Description")
    private String description;
    
    @Column(name = "HangPercent")
    private BigDecimal hangPercent;
    
    @Column(name = "DailyPercent")
    private BigDecimal dailyPercent;
    
    @Column(name = "StartDate")
    private LocalDate startDate;
    
    @Column(name = "EndDate")
    private LocalDate endDate;
    
    @Column(name = "Status")
    @Enumerated(EnumType.STRING)
    private DiscountPolicyStatus status;
    
    @Column(name = "CreatedAt")
    private Date creationDate;
    
    @Column(name = "LevelID")
    private int levelID;

    public DTODiscountPolicy() {}

    public DTODiscountPolicy(int policyID, DTODealer dealer, String policyName, String description, 
                              BigDecimal hangPercent, BigDecimal dailyPercent, LocalDate startDate, 
                              LocalDate endDate, DiscountPolicyStatus status, Date creationDate, int levelID) {
        this.policyID = policyID;
        this.dealer = dealer;
        this.policyName = policyName;
        this.description = description;
        this.hangPercent = hangPercent;
        this.dailyPercent = dailyPercent;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.creationDate = creationDate;
        this.levelID = levelID;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public int getLevelID() {
        return levelID;
    }

    public void setLevelID(int levelID) {
        this.levelID = levelID;
    }

    // Getters / Setters
    public int getPolicyID() { return policyID; }
    public void setPolicyID(int policyID) { this.policyID = policyID; }

    public DTODealer getDealer() { return dealer; }
    public void setDealer(DTODealer dealer) { this.dealer = dealer; }

    public String getPolicyName() { return policyName; }
    public void setPolicyName(String policyName) { this.policyName = policyName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getHangPercent() { return hangPercent; }
    public void setHangPercent(BigDecimal hangPercent) { this.hangPercent = hangPercent; }

    public BigDecimal getDailyPercent() { return dailyPercent; }
    public void setDailyPercent(BigDecimal dailyPercent) { this.dailyPercent = dailyPercent; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public DiscountPolicyStatus getStatus() { return status; }
    public void setStatus(DiscountPolicyStatus status) { this.status = status; }

}
