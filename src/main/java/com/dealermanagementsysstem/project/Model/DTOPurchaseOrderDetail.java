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
}
