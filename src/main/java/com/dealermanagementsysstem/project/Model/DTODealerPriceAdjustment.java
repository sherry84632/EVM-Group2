package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "DealerPriceAdjustment")
public class DTODealerPriceAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AdjustmentID")
    private int adjustmentID;
    
    @ManyToOne
    @JoinColumn(name = "DealerID", referencedColumnName = "DealerID")
    private DTODealer dealer;
    
    @ManyToOne
    @JoinColumn(name = "ModelID", referencedColumnName = "ModelID")
    private DTOVehicleModel vehicleModel;
    
    @Column(name = "DiscountAmount")
    private Double discountAmount;
    
    @Column(name = "DiscountPercent")
    private Double discountPercent;
    
    @Column(name = "StartDate")
    private LocalDate startDate;
    
    @Column(name = "EndDate")
    private LocalDate endDate;
    
    @Column(name = "Notes")
    private String notes;
    
    @Column(name = "PromotionName")
    private String promotionName;


    public DTODealerPriceAdjustment() {}

    public DTODealerPriceAdjustment(int adjustmentID, DTODealer dealer, DTOVehicleModel vehicleModel,
                                    Double discountAmount, Double discountPercent,
                                    LocalDate startDate, LocalDate endDate,
                                    String notes, String promotionName) {
        this.adjustmentID = adjustmentID;
        this.dealer = dealer;
        this.vehicleModel = vehicleModel;
        this.discountAmount = discountAmount;
        this.discountPercent = discountPercent;
        this.startDate = startDate;
        this.endDate = endDate;
        this.notes = notes;
        this.promotionName = promotionName;
    }

    public int getAdjustmentID() {
        return adjustmentID;
    }

    public void setAdjustmentID(int adjustmentID) {
        this.adjustmentID = adjustmentID;
    }

    public DTODealer getDealer() {
        return dealer;
    }

    public void setDealer(DTODealer dealer) {
        this.dealer = dealer;
    }

    public DTOVehicleModel getVehicleModel() {
        return vehicleModel;
    }

    public void setVehicleModel(DTOVehicleModel vehicleModel) {
        this.vehicleModel = vehicleModel;
    }

    public Double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public Double getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(Double discountPercent) {
        this.discountPercent = discountPercent;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getPromotionName() {
        return promotionName;
    }

    public void setPromotionName(String promotionName) {
        this.promotionName = promotionName;
    }
}
