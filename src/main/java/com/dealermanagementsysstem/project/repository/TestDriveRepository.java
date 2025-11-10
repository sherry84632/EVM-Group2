package com.dealermanagementsysstem.project.repository;

import com.dealermanagementsysstem.project.Model.DTOTestDrive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TestDriveRepository extends JpaRepository<DTOTestDrive, Integer> {
    @Query("SELECT t FROM DTOTestDrive t WHERE t.customer.customerID = :customerId")
    DTOTestDrive findByCustomerId(@Param("customerId") Integer customerId);
}
