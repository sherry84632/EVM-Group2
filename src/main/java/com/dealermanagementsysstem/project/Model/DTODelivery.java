package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "Delivery")
public class DTODelivery {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DeliveryID")
    private int deliveryID;
    
    @ManyToOne
    @JoinColumn(name = "PurchaseOrderID", referencedColumnName = "PurchaseOrderID")
    private DTOPurchaseOrder purchaseOrder;
    
    @Column(name = "DeliveryDate")
    private Date deliveryDate;
    
    @Column(name = "DeliveryStatus")
    @Enumerated(EnumType.STRING)
    private DeliveryStatus deliveryStatus;
    
    @OneToMany(mappedBy = "delivery", cascade = CascadeType.ALL)
    private List<DTODeliveryDetail> deliveryDetails;
    
    public DTODelivery() {
    }
    
    public DTODelivery(int deliveryID, DTOPurchaseOrder purchaseOrder, 
                       Date deliveryDate, DeliveryStatus deliveryStatus) {
        this.deliveryID = deliveryID;
        this.purchaseOrder = purchaseOrder;
        this.deliveryDate = deliveryDate;
        this.deliveryStatus = deliveryStatus;
    }
    
    public int getDeliveryID() {
        return deliveryID;
    }
    
    public void setDeliveryID(int deliveryID) {
        this.deliveryID = deliveryID;
    }
    
    public DTOPurchaseOrder getPurchaseOrder() {
        return purchaseOrder;
    }
    
    public void setPurchaseOrder(DTOPurchaseOrder purchaseOrder) {
        this.purchaseOrder = purchaseOrder;
    }
    
    
    public Date getDeliveryDate() {
        return deliveryDate;
    }
    
    public void setDeliveryDate(Date deliveryDate) {
        this.deliveryDate = deliveryDate;
    }
    
    public DeliveryStatus getDeliveryStatus() {
        return deliveryStatus;
    }
    
    public void setDeliveryStatus(DeliveryStatus deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }
    
    public List<DTODeliveryDetail> getDeliveryDetails() {
        return deliveryDetails;
    }
    
    public void setDeliveryDetails(List<DTODeliveryDetail> deliveryDetails) {
        this.deliveryDetails = deliveryDetails;
    }
}

