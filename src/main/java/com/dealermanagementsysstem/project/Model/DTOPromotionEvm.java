package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "PromotionEvm")
public class DTOPromotionEvm {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PromotionEvmID")
    private int promotionEvmID;
    
    @ManyToOne
    @JoinColumn(name = "ModelID", referencedColumnName = "ModelID")
    private DTOVehicleModel model;
    
    @ManyToOne
    @JoinColumn(name = "VersionID", referencedColumnName = "VersionID")
    private DTOVehicleVersion version;
    
    @Column(name = "PolicyName")
    private String policyName;
    
    @Column(name = "DiscountRate")
    private BigDecimal discountRate;
    
    @Column(name = "StartDate")
    private LocalDate startDate;
    
    @Column(name = "EndDate")
    private LocalDate endDate;
    
    @Column(name = "Description")
    private String description;
    
    public DTOPromotionEvm() {
    }
    
    public DTOPromotionEvm(int promotionEvmID, DTOVehicleModel model, DTOVehicleVersion version, 
                            String policyName, BigDecimal discountRate, 
                            LocalDate startDate, LocalDate endDate, String description) {
        this.promotionEvmID = promotionEvmID;
        this.model = model;
        this.version = version;
        this.policyName = policyName;
        this.discountRate = discountRate;
        this.startDate = startDate;
        this.endDate = endDate;
        this.description = description;
    }
    
    public int getPromotionEvmID() {
        return promotionEvmID;
    }
    
    public void setPromotionEvmID(int promotionEvmID) {
        this.promotionEvmID = promotionEvmID;
    }
    
    public DTOVehicleModel getModel() {
        return model;
    }
    
    public void setModel(DTOVehicleModel model) {
        this.model = model;
    }
    
    public DTOVehicleVersion getVersion() {
        return version;
    }
    
    public void setVersion(DTOVehicleVersion version) {
        this.version = version;
    }
    
    public String getPolicyName() {
        return policyName;
    }
    
    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }
    
    public BigDecimal getDiscountRate() {
        return discountRate;
    }
    
    public void setDiscountRate(BigDecimal discountRate) {
        this.discountRate = discountRate;
    }
    
    public LocalDate getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
    
    public LocalDate getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
}

