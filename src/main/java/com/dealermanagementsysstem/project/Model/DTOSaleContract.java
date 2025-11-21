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
    
    @Column(name = "RegistrationFee")
    private java.math.BigDecimal registrationFee;
    @Column(name = "DeliveryFee")
    private java.math.BigDecimal deliveryFee;
    @Column(name = "InsuranceFee")
    private java.math.BigDecimal insuranceFee;
    @Column(name = "ServiceFee")
    private java.math.BigDecimal serviceFee;
    @Column(name = "Terms")
    private String terms;
    @Column(name = "CustomerAddressSnapshot")
    private String customerAddressSnapshot;
    @Column(name = "CustomerIdNumber")
    private String customerIdNumber;
    @Column(name = "DealerSignatureName")
    private String dealerSignatureName;
    @Column(name = "CustomerSignatureName")
    private String customerSignatureName;
    @Column(name = "SignedDate")
    private java.util.Date signedDate;
    @Column(name = "SignStatus")
    @Enumerated(EnumType.STRING)
    private ContractSignStatus signStatus;

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
    public java.math.BigDecimal getRegistrationFee(){ return registrationFee; }
    public void setRegistrationFee(java.math.BigDecimal v){ this.registrationFee=v; }
    public java.math.BigDecimal getDeliveryFee(){ return deliveryFee; }
    public void setDeliveryFee(java.math.BigDecimal v){ this.deliveryFee=v; }
    public java.math.BigDecimal getInsuranceFee(){ return insuranceFee; }
    public void setInsuranceFee(java.math.BigDecimal v){ this.insuranceFee=v; }
    public java.math.BigDecimal getServiceFee(){ return serviceFee; }
    public void setServiceFee(java.math.BigDecimal v){ this.serviceFee=v; }
    public String getTerms(){ return terms; }
    public void setTerms(String t){ this.terms=t; }
    public String getCustomerAddressSnapshot(){ return customerAddressSnapshot; }
    public void setCustomerAddressSnapshot(String s){ this.customerAddressSnapshot=s; }
    public String getCustomerIdNumber(){ return customerIdNumber; }
    public void setCustomerIdNumber(String s){ this.customerIdNumber=s; }
    public String getDealerSignatureName(){ return dealerSignatureName; }
    public void setDealerSignatureName(String s){ this.dealerSignatureName=s; }
    public String getCustomerSignatureName(){ return customerSignatureName; }
    public void setCustomerSignatureName(String s){ this.customerSignatureName=s; }
    public java.util.Date getSignedDate(){ return signedDate; }
    public void setSignedDate(java.util.Date d){ this.signedDate=d; }
    public ContractSignStatus getSignStatus(){ return signStatus; }
    public void setSignStatus(ContractSignStatus s){ this.signStatus=s; }
    public java.math.BigDecimal getGrandTotal(){
        java.math.BigDecimal base = totalAmount!=null? totalAmount: java.math.BigDecimal.ZERO;
        java.math.BigDecimal sumFees = java.math.BigDecimal.ZERO;
        if(registrationFee!=null) sumFees=sumFees.add(registrationFee);
        if(deliveryFee!=null) sumFees=sumFees.add(deliveryFee);
        if(insuranceFee!=null) sumFees=sumFees.add(insuranceFee);
        if(serviceFee!=null) sumFees=sumFees.add(serviceFee);
        return base.add(sumFees);
    }
}
