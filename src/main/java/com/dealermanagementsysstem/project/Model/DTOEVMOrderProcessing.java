package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;
import java.util.Date;

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

    public DTOEVMOrderProcessing() {
    }

    public DTOEVMOrderProcessing(int processId, DTOPurchaseOrder purchaseOrder, int evmStaffId, 
                                 String actionType, Date actionDate, String remarks) {
        this.processId = processId;
        this.purchaseOrder = purchaseOrder;
        this.evmStaffId = evmStaffId;
        this.actionType = actionType;
        this.actionDate = actionDate;
        this.remarks = remarks;
    }

    public int getProcessId() {
        return processId;
    }

    public void setProcessId(int processId) {
        this.processId = processId;
    }

    public DTOPurchaseOrder getPurchaseOrder() {
        return purchaseOrder;
    }

    public void setPurchaseOrder(DTOPurchaseOrder purchaseOrder) {
        this.purchaseOrder = purchaseOrder;
    }

    public int getEvmStaffId() {
        return evmStaffId;
    }

    public void setEvmStaffId(int evmStaffId) {
        this.evmStaffId = evmStaffId;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public Date getActionDate() {
        return actionDate;
    }

    public void setActionDate(Date actionDate) {
        this.actionDate = actionDate;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
