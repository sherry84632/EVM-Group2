package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "DiscountPolicy")
public class DTODiscountPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PolicyID")
    private int policyID;

    @ManyToOne
    @JoinColumn(name = "DealerID", referencedColumnName = "DealerID")
    private DTODealer dealer;

    @Column(name = "PolicyName")
    private String policyName;

    @Column(name = "Description")
    private String description;

    @Column(name = "HangPercent")
    private BigDecimal hangPercent;

    @Column(name = "DailyPercent")
    private BigDecimal dailyPercent;

    @Column(name = "StartDate")
    private LocalDate startDate;

    @Column(name = "EndDate")
    private LocalDate endDate;

    @Column(name = "Status")
    @Enumerated(EnumType.STRING)
    private DiscountPolicyStatus status;

    @Column(name = "CreatedAt")
    private Date creationDate;

    @Column(name = "LevelID")
    private int levelID;


}
