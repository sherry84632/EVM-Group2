package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "SaleContract")
public class DTOSaleContract {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ContractID")
    private int contractID;
    
    @ManyToOne
    @JoinColumn(name = "SaleOrderID", referencedColumnName = "SaleOrderID")
    private DTOSaleOrder saleOrder;
    
    @Column(name = "ContractDate")
    private Date contractDate;
    
    @Column(name = "TotalAmount")
    private BigDecimal totalAmount;
    
    @Column(name = "Status")
    @Enumerated(EnumType.STRING)
    private SaleContractStatus status;
    
    public DTOSaleContract() {
    }
    
    public DTOSaleContract(int contractID, DTOSaleOrder saleOrder, Date contractDate, 
                           BigDecimal totalAmount, SaleContractStatus status) {
        this.contractID = contractID;
        this.saleOrder = saleOrder;
        this.contractDate = contractDate;
        this.totalAmount = totalAmount;
        this.status = status;
    }
    
    public int getContractID() {
        return contractID;
    }
    
    public void setContractID(int contractID) {
        this.contractID = contractID;
    }
    
    public DTOSaleOrder getSaleOrder() {
        return saleOrder;
    }
    
    public void setSaleOrder(DTOSaleOrder saleOrder) {
        this.saleOrder = saleOrder;
    }
    
    public Date getContractDate() {
        return contractDate;
    }
    
    public void setContractDate(Date contractDate) {
        this.contractDate = contractDate;
    }
    
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    public SaleContractStatus getStatus() {
        return status;
    }
    
    public void setStatus(SaleContractStatus status) {
        this.status = status;
    }
}

