package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Data
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


}
