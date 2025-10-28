package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;
import java.util.Date;
import java.util.List;

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
    private String approvedByStaffName;

    @Transient
    private Integer totalQuantity; // computed sum of detail quantities

    @Transient private Date plannedDeliveryDate;
    @Transient private Date actualDeliveryDate;
    @Transient private String deliveryMethod;
    @Transient private String shippingStatus;
    @Transient private String logisticsNotes;

    @Transient private String primaryModelName;
    @Transient private String primaryVersionName;
    @Transient private String primaryColorName;
    @Transient private java.math.BigDecimal primaryUnitPrice;
    @Transient private java.math.BigDecimal primarySubtotal;

    public DTOPurchaseOrder() {}

    public DTOPurchaseOrder(int purchaseOrderId, DTODealer dealer, DTODealerStaff staff, 
                            PurchaseOrderStatus status, Date createdAt, java.math.BigDecimal totalAmount, int evmID) {
        this.purchaseOrderId = purchaseOrderId;
        this.dealer = dealer;
        this.staff = staff;
        this.status = status;
        this.createdAt = createdAt;
        this.totalAmount = totalAmount;
        this.evmID = evmID;
    }

    // --- GETTER & SETTER ---
    public int getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public void setPurchaseOrderId(int purchaseOrderId) {
        this.purchaseOrderId = purchaseOrderId;
    }

    public DTODealer getDealer() {
        return dealer;
    }

    public void setDealer(DTODealer dealer) {
        this.dealer = dealer;
    }

    public DTODealerStaff getStaff() {
        return staff;
    }

    public void setStaff(DTODealerStaff staff) {
        this.staff = staff;
    }

    public PurchaseOrderStatus getStatus() {
        return status;
    }

    public void setStatus(PurchaseOrderStatus status) {
        this.status = status;
    }
    
    public java.math.BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(java.math.BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public List<DTOPurchaseOrderDetail> getOrderDetails() {
        return orderDetails;
    }

    public void setOrderDetails(List<DTOPurchaseOrderDetail> orderDetails) {
        this.orderDetails = orderDetails;
    }

    public int getEvmID() {
        return evmID;
    }

    public void setEvmID(int evmID) {
        this.evmID = evmID;
    }

    public String getDealerName() {
        return dealerName;
    }

    public void setDealerName(String dealerName) {
        this.dealerName = dealerName;
    }

    public String getDealerLevelName() {
        return dealerLevelName;
    }

    public void setDealerLevelName(String dealerLevelName) {
        this.dealerLevelName = dealerLevelName;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public Double getPolicyDiscountPercent() {
        return policyDiscountPercent;
    }

    public void setPolicyDiscountPercent(Double policyDiscountPercent) {
        this.policyDiscountPercent = policyDiscountPercent;
    }

    public String getApprovedByStaffName() {
        return approvedByStaffName;
    }

    public void setApprovedByStaffName(String approvedByStaffName) {
        this.approvedByStaffName = approvedByStaffName;
    }

    public Integer getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }

    public Date getPlannedDeliveryDate() { return plannedDeliveryDate; }
    public void setPlannedDeliveryDate(Date plannedDeliveryDate) { this.plannedDeliveryDate = plannedDeliveryDate; }
    public Date getActualDeliveryDate() { return actualDeliveryDate; }
    public void setActualDeliveryDate(Date actualDeliveryDate) { this.actualDeliveryDate = actualDeliveryDate; }
    public String getDeliveryMethod() { return deliveryMethod; }
    public void setDeliveryMethod(String deliveryMethod) { this.deliveryMethod = deliveryMethod; }
    public String getShippingStatus() { return shippingStatus; }
    public void setShippingStatus(String shippingStatus) { this.shippingStatus = shippingStatus; }
    public String getLogisticsNotes() { return logisticsNotes; }
    public void setLogisticsNotes(String logisticsNotes) { this.logisticsNotes = logisticsNotes; }

    public String getPrimaryModelName(){return primaryModelName;}
    public void setPrimaryModelName(String s){this.primaryModelName=s;}
    public String getPrimaryVersionName(){return primaryVersionName;}
    public void setPrimaryVersionName(String s){this.primaryVersionName=s;}
    public String getPrimaryColorName(){return primaryColorName;}
    public void setPrimaryColorName(String s){this.primaryColorName=s;}
    public java.math.BigDecimal getPrimaryUnitPrice(){return primaryUnitPrice;}
    public void setPrimaryUnitPrice(java.math.BigDecimal p){this.primaryUnitPrice=p;}
    public java.math.BigDecimal getPrimarySubtotal(){return primarySubtotal;}
    public void setPrimarySubtotal(java.math.BigDecimal p){this.primarySubtotal=p;}

    @Override
    public String toString() {
        return "DTOPurchaseOrder{" +
                "purchaseOrderId=" + purchaseOrderId +
                ", dealer=" + dealer +
                ", staff=" + staff +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", evmID=" + evmID +
                ", orderDetails=" + orderDetails +
                '}';
    }
}
