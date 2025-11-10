package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.math.BigDecimal;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "SaleOrder")
public class DTOSaleOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int saleOrderID;
    @ManyToOne
    @JoinColumn(name = "CustomerID", referencedColumnName = "CustomerID")
    private DTOCustomer customer;
    @ManyToOne
    @JoinColumn(name = "DealerID", referencedColumnName = "DealerID")
    private DTODealer dealer;
    @ManyToOne
    @JoinColumn(name = "StaffID", referencedColumnName = "StaffID")
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

    @Column(name = "PlannedDeliveryDate")
    private java.sql.Timestamp plannedDeliveryDate;
    @Column(name = "ActualDeliveryDate")
    private java.sql.Timestamp actualDeliveryDate;
    @Column(name = "EtaDays")
    private Integer etaDays; // calculated days from creation to planned


    @Transient
    public int getDaysRemaining() {
        if (plannedDeliveryDate == null || actualDeliveryDate != null) return 0;
        long now = System.currentTimeMillis();
        long planned = plannedDeliveryDate.getTime();
        if (planned <= now) return 0;
        long diffMs = planned - now;
        return (int) Math.ceil(diffMs / 86400000.0); // round up to whole day
    }
}
