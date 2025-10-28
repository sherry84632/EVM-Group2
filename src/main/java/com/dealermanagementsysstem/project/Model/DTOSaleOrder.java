package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;

import java.sql.Timestamp;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "SaleOrder")
public class DTOSaleOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int saleOrderID;
    @ManyToOne
    @JoinColumn(name = "customer_customer_id", referencedColumnName = "CustomerID")
    private DTOCustomer customer;
    @ManyToOne
    @JoinColumn(name = "dealer_dealer_id", referencedColumnName = "DealerID")
    private DTODealer dealer;
    @ManyToOne
    @JoinColumn(name = "staff_staff_id", referencedColumnName = "StaffID")
    private DTODealerStaff staff;
    // Keep quotation mapping (if column exists) else it will stay null
    @ManyToOne
    @JoinColumn(name = "QuotationID", referencedColumnName = "QuotationID", nullable = true)
    private DTOQuotation quotation;
    
    @Column(name = "CreatedAt")
    private Timestamp createdAt;
    
    @Column(name = "Status")
    @Enumerated(EnumType.STRING)
    private SaleOrderStatus status;
    
    @OneToMany(mappedBy = "saleOrder", cascade = CascadeType.ALL)
    private List<DTOSaleOrderDetail> detail; // 🔹 Danh sách chi tiết đơn hàng
    
    // Aggregated fields
    @Column(name = "Quantity")
    private int totalQuantity;            // Tổng số lượng (sum of details)
    
    @Column(name = "TotalAmount")
    private BigDecimal totalAmount;       // Tổng tiền (sum of price * quantity)


    public DTOSaleOrder() {
    }

    public DTOSaleOrder(int saleOrderID, DTOCustomer customer, DTODealer dealer, DTODealerStaff staff, Timestamp createdAt, SaleOrderStatus status, List<DTOSaleOrderDetail> detail, int totalQuantity, BigDecimal totalAmount, DTOQuotation quotation) {
        this.saleOrderID = saleOrderID;
        this.customer = customer;
        this.dealer = dealer;
        this.staff = staff;
        this.createdAt = createdAt;
        this.status = status;
        this.detail = detail;
        this.totalQuantity = totalQuantity;
        this.totalAmount = totalAmount;
        this.quotation = quotation;
    }

    public int getSaleOrderID() {
        return saleOrderID;
    }

    public void setSaleOrderID(int saleOrderID) {
        this.saleOrderID = saleOrderID;
    }

    // Alias methods for legacy Thymeleaf templates referencing orderID instead of saleOrderID
    public int getOrderID() {
        return getSaleOrderID();
    }

    public void setOrderID(int id) {
        setSaleOrderID(id);
    }

    public DTOCustomer getCustomer() {
        return customer;
    }

    public void setCustomer(DTOCustomer customer) {
        this.customer = customer;
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

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    // === Thymeleaf legacy alias for orderDate ===
    public Timestamp getOrderDate() {
        return getCreatedAt();
    }

    public void setOrderDate(Timestamp ts) {
        setCreatedAt(ts);
    }

    public SaleOrderStatus getStatus() {
        return status;
    }

    public void setStatus(SaleOrderStatus status) {
        this.status = status;
    }

    public List<DTOSaleOrderDetail> getDetail() {
        return detail;
    }

    public void setDetail(List<DTOSaleOrderDetail> detail) {
        this.detail = detail;
    }

    // === Aggregated total quantity ===
    public int getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(int totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    // === Aggregated total amount ===
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public DTOQuotation getQuotation() {
        return quotation;
    }

    public void setQuotation(DTOQuotation quotation) {
        this.quotation = quotation;
    }
}
