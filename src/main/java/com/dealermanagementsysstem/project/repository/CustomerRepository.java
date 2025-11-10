package com.dealermanagementsysstem.project.repository;

import com.dealermanagementsysstem.project.Model.DTOCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustomerRepository extends JpaRepository<DTOCustomer, Integer> {
    boolean existsByEmail(String email);

    @Query("SELECT c FROM DTOCustomer c WHERE c.fullName LIKE %:keyword% OR c.phone LIKE %:keyword%")
    List<DTOCustomer> searchByKeyword(@Param("keyword") String keyword);
}
