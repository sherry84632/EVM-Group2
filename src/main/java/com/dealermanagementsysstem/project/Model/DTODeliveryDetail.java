package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "DeliveryDetail")
public class DTODeliveryDetail {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DeliveryDetailID")
    private int deliveryDetailID;
    
    @ManyToOne
    @JoinColumn(name = "DeliveryID", referencedColumnName = "DeliveryID")
    private DTODelivery delivery;
    
    @ManyToOne
    @JoinColumn(name = "VIN", referencedColumnName = "VIN")
    private DTOVehicle vehicle;
    
    public DTODeliveryDetail() {
    }
    
    public DTODeliveryDetail(int deliveryDetailID, DTODelivery delivery, DTOVehicle vehicle) {
        this.deliveryDetailID = deliveryDetailID;
        this.delivery = delivery;
        this.vehicle = vehicle;
    }
    
    public int getDeliveryDetailID() {
        return deliveryDetailID;
    }
    
    public void setDeliveryDetailID(int deliveryDetailID) {
        this.deliveryDetailID = deliveryDetailID;
    }
    
    public DTODelivery getDelivery() {
        return delivery;
    }
    
    public void setDelivery(DTODelivery delivery) {
        this.delivery = delivery;
    }
    
    public DTOVehicle getVehicle() {
        return vehicle;
    }
    
    public void setVehicle(DTOVehicle vehicle) {
        this.vehicle = vehicle;
    }
}

