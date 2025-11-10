package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Date;
import java.util.List;


@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
@Entity
@Table(name = "PurchaseOrder")
public class DTOPurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PurchaseOrderID")
    private int purchaseOrderId;

    @ManyToOne
    @JoinColumn(name = "DealerID", referencedColumnName = "DealerID")
    private DTODealer dealer;

    @ManyToOne
    @JoinColumn(name = "StaffID", referencedColumnName = "StaffID")
    private DTODealerStaff staff;

    @Column(name = "CreatedAt")
    private Date createdAt;

    @Column(name = "Status")
    @Enumerated(EnumType.STRING)
    private PurchaseOrderStatus status;

    @Column(name = "TotalAmount")
    private java.math.BigDecimal totalAmount;

    @Column(name = "EvmID")
    private int evmID;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL)
    private List<DTOPurchaseOrderDetail> orderDetails;

    // Transient fields for additional information (not persisted to DB)
    @Transient
    private String dealerName;

    @Transient
    private String dealerLevelName;

    @Transient
    private String policyName;

    @Transient
    private Double policyDiscountPercent;

    @Transient
    private Double dealerRewardPercent; // DailyPercent from DiscountPolicy

    @Transient
    private Double manufacturerSharePercent; // HangPercent from DiscountPolicy

    @Transient
    private String approvedByStaffName;

    @Transient
    private Integer totalQuantity; // computed sum of detail quantities

    @Transient
    private Date plannedDeliveryDate;
    @Transient
    private Date actualDeliveryDate;
    @Transient
    private String deliveryMethod;
    @Transient
    private String shippingStatus;
    @Transient
    private String logisticsNotes;

    @Transient
    private String primaryModelName;
    @Transient
    private String primaryVersionName;
    @Transient
    private String primaryColorName;
    @Transient
    private java.math.BigDecimal primaryUnitPrice;
    @Transient
    private java.math.BigDecimal primarySubtotal;


}
