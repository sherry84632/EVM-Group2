package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "DealerInventory")
public class DTODealerInventory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DealerInventoryID")
    private int dealerInventoryID;
    
    @ManyToOne
    @JoinColumn(name = "DealerID", referencedColumnName = "DealerID")
    private DTODealer dealer;
    
    @ManyToOne
    @JoinColumn(name = "VehicleID", referencedColumnName = "VehicleID")
    private DTOVehicle vehicle;
    
    @Column(name = "ReceivedDate")
    private Date receivedDate;
    
    @Column(name = "Status")
    @Enumerated(EnumType.STRING)
    private DealerInventoryStatus status;

    public DTODealerInventory() {}

    public DTODealerInventory(int dealerInventoryID, DTODealer dealer, DTOVehicle vehicle, 
                              Date receivedDate, DealerInventoryStatus status) {
        this.dealerInventoryID = dealerInventoryID;
        this.dealer = dealer;
        this.vehicle = vehicle;
        this.receivedDate = receivedDate;
        this.status = status;
    }

    public int getDealerInventoryID() {
        return dealerInventoryID;
    }

    public void setDealerInventoryID(int dealerInventoryID) {
        this.dealerInventoryID = dealerInventoryID;
    }

    public DTODealer getDealer() {
        return dealer;
    }

    public void setDealer(DTODealer dealer) {
        this.dealer = dealer;
    }

    public DTOVehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(DTOVehicle vehicle) {
        this.vehicle = vehicle;
    }

    public Date getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(Date receivedDate) {
        this.receivedDate = receivedDate;
    }

    public DealerInventoryStatus getStatus() {
        return status;
    }

    public void setStatus(DealerInventoryStatus status) {
        this.status = status;
    }
}
