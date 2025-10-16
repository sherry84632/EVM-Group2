package com.dealermanagementsysstem.project.Model;
import java.util.Date;
public class DTOEVMOrderProcessing {
    private int processId;
    private int purchaseOrderId;
    private int evmStaffId;
    private String actionType;
    private Date actionDate;
    private String remarks;

    public DTOEVMOrderProcessing() {
    }

    public DTOEVMOrderProcessing(int processId, int purchaseOrderId, int evmStaffId, String actionType, Date actionDate, String remarks) {
        this.processId = processId;
        this.purchaseOrderId = purchaseOrderId;
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

    public int getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public void setPurchaseOrderId(int purchaseOrderId) {
        this.purchaseOrderId = purchaseOrderId;
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
