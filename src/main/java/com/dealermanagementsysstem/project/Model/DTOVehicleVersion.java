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
@Table(name = "VehicleVersion")
public class DTOVehicleVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "VersionID")
    private int versionID;

    @ManyToOne
    @JoinColumn(name = "ModelID", referencedColumnName = "ModelID")
    private DTOVehicleModel model;

    @Column(name = "VersionName")
    private String versionName;

    @Column(name = "Engine")
    private String engine;

    @Column(name = "Transmission")
    private String transmission;

    @OneToMany(mappedBy = "version")
    private List<DTOVehicle> vehicles;

    @OneToMany(mappedBy = "version")
    private List<DTOPurchaseOrderDetail> purchaseOrderDetails;


}
