package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "Dealer")
public class DTODealer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DealerID")
    private int dealerID;

    @Column(name = "DealerName", nullable = false)
    private String dealerName;

    @Column(name = "Address")
    private String address;

    @Column(name = "Phone", unique = true)
    private String phone;

    @Column(name = "Email", unique = true)
    private String email;

    @Column(name = "EvmID")
    private int evmID;

    @Column(name = "LevelID")
    private int levelID;

    @Column(name = "PolicyID")
    private int policyID;

    @Column(name = "CreatedAt")
    private java.sql.Timestamp createdAt;

    @Column(name = "UpdatedAt")
    private java.sql.Timestamp updatedAt;


}
