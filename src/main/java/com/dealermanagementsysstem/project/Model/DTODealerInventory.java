package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Data
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
    
    @Column(name = "VIN")
    private String vin;

    @ManyToOne
    @JoinColumn(name = "VehicleID", referencedColumnName = "VehicleID")
    private DTOVehicle vehicle;
    
    @Column(name = "ReceivedDate")
    private Date receivedDate;
    
    @Column(name = "Status")
    @Enumerated(EnumType.STRING)
    private DealerInventoryStatus status;

    @Column(name = "CostPrice")
    private java.math.BigDecimal costPrice; // Giá cost sau chiết khấu từ EVM

}
