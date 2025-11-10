package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "Account")
public class DTOAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AccountID")
    private int accountId;

    @Column(name = "Username")
    private String username;

    @Column(name = "Password")
    private String password;

    @Column(name = "Role",nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "IsActive")
    private boolean isActive  = true ;

    @Column(name = "Email",unique = true)
    private String email;

    @Column(name = "CreatedAt")
    private java.sql.Timestamp createdAt;

    @Column(name = "UpdatedAt")
    private java.sql.Timestamp updatedAt;


    @OneToOne(mappedBy = "account")
    private DTODealerStaff dealerStaff;

}

