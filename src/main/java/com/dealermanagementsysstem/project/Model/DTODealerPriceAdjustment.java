package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@Data
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

}
