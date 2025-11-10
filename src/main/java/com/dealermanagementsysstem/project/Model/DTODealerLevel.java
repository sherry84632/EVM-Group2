package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "DealerLevel")
public class DTODealerLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LevelID")
    private int levelID;

    @Column(name = "LevelName")
    private String levelName;

    @Column(name = "MinOrderValue")
    private java.math.BigDecimal minOrderValue;

    @Column(name = "MaxOrderValue")
    private java.math.BigDecimal maxOrderValue;

}

