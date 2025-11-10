package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "Delivery")
public class DTODelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DeliveryID")
    private int deliveryID;

    @ManyToOne
    @JoinColumn(name = "PurchaseOrderID", referencedColumnName = "PurchaseOrderID")
    private DTOPurchaseOrder purchaseOrder;

    @Column(name = "DeliveryDate")
    private Date deliveryDate;

    @Column(name = "DeliveryStatus")
    @Enumerated(EnumType.STRING)
    private DeliveryStatus deliveryStatus;

    @OneToMany(mappedBy = "delivery", cascade = CascadeType.ALL)
    private List<DTODeliveryDetail> deliveryDetails;

}

