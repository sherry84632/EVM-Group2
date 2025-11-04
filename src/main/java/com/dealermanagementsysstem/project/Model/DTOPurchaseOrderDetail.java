package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "PurchaseOrderDetail")
public class DTOPurchaseOrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PODetailID")
    private int poDetailId;
    
    @ManyToOne
    @JoinColumn(name = "PurchaseOrderID", referencedColumnName = "PurchaseOrderID")
    private DTOPurchaseOrder purchaseOrder;
    
    @ManyToOne
    @JoinColumn(name = "ColorID", referencedColumnName = "ColorID")
    private DTOVehicleColor color;
    
    @ManyToOne
    @JoinColumn(name = "VersionID", referencedColumnName = "VersionID")
    private DTOVehicleVersion version;
    
    @Column(name = "UnitPrice")
    private BigDecimal unitPrice;
    
    @Column(name = "Quantity")
    private int quantity;
    
    @Column(name = "Subtotal")
    private java.math.BigDecimal subtotal;

    @Column(name = "PaymentStatus")
    private String paymentStatus; // UNPAID, PAID

    // Transient fields for template display
    @Transient
    private String modelName;

    @Transient
    private String versionName;

    @Transient
    private String colorName;

    @Transient
    private java.math.BigDecimal basePrice; // Giá gốc (chưa chiết khấu)

    @Transient
    private Double discountPercent; // % chiết khấu

    @Transient
    private java.math.BigDecimal discountAmount; // Số tiền chiết khấu

    public DTOPurchaseOrderDetail() {}

    public DTOPurchaseOrderDetail(int poDetailId, DTOPurchaseOrder purchaseOrder, DTOVehicleColor color, 
                                  DTOVehicleVersion version, BigDecimal unitPrice, int quantity, java.math.BigDecimal subtotal) {
        this.poDetailId = poDetailId;
        this.purchaseOrder = purchaseOrder;
        this.color = color;
        this.version = version;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.subtotal = subtotal;
    }

    public int getPoDetailId() { return poDetailId; }
    public void setPoDetailId(int poDetailId) { this.poDetailId = poDetailId; }

    public DTOPurchaseOrder getPurchaseOrder() { return purchaseOrder; }
    public void setPurchaseOrder(DTOPurchaseOrder purchaseOrder) { this.purchaseOrder = purchaseOrder; }

    public DTOVehicleColor getColor() { return color; }
    public void setColor(DTOVehicleColor color) { this.color = color; }

    public DTOVehicleVersion getVersion() { return version; }
    public void setVersion(DTOVehicleVersion version) { this.version = version; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    
    public java.math.BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(java.math.BigDecimal subtotal) { this.subtotal = subtotal; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getVersionName() { return versionName; }
    public void setVersionName(String versionName) { this.versionName = versionName; }

    public String getColorName() { return colorName; }
    public void setColorName(String colorName) { this.colorName = colorName; }

    public java.math.BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(java.math.BigDecimal basePrice) { this.basePrice = basePrice; }

    public Double getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(Double discountPercent) { this.discountPercent = discountPercent; }

    public java.math.BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(java.math.BigDecimal discountAmount) { this.discountAmount = discountAmount; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
}
