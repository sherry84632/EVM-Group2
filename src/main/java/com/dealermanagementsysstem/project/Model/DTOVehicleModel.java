package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "VehicleModel")
public class DTOVehicleModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ModelID")
    private int modelID;

    @Column(name = "ModelName")
    private String modelName;

    @Column(name = "Brand")
    private String brand;

    @Column(name = "Year")
    private int year;

    @Column(name = "BasePrice")
    private BigDecimal basePrice;

    @Column(name = "BodyType")
    private String bodyType;

    @Column(name = "Description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Lob
    @Column(name = "ModelImage", columnDefinition = "VARBINARY(MAX)")
    private byte[] modelImage;

    @Column(name = "EvmID")
    private int evmID;

    @OneToMany(mappedBy = "model")
    private List<DTOVehicleVersion> versions;


}
