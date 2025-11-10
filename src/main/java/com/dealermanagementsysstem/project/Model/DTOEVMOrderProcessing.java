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
@Table(name = "EVMOrderProcessing")
public class DTOEVMOrderProcessing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ProcessID")
    private int processId;

    @ManyToOne
    @JoinColumn(name = "PurchaseOrderID", referencedColumnName = "PurchaseOrderID")
    private DTOPurchaseOrder purchaseOrder;

    @Column(name = "EvmStaffID")
    private int evmStaffId;

    @Column(name = "ActionType")
    private String actionType;

    @Column(name = "ActionDate")
    private Date actionDate;

    @Column(name = "Remarks")
    private String remarks;


}
