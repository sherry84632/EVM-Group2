package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "VehicleColor")
public class DTOVehicleColor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ColorID")
    private int colorID;

    @ManyToOne
    @JoinColumn(name = "ModelID", referencedColumnName = "ModelID")
    private DTOVehicleModel model;

    @Column(name = "ColorName")
    private String colorName;

    @OneToMany(mappedBy = "color")
    private List<DTOVehicle> vehicles;

    @OneToMany(mappedBy = "color")
    private List<DTOQuotationDetail> quotationDetails;

    @OneToMany(mappedBy = "color")
    private List<DTOPurchaseOrderDetail> purchaseOrderDetails;

}
