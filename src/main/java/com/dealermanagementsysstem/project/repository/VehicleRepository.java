package com.dealermanagementsysstem.project.repository;

import com.dealermanagementsysstem.project.Model.DTOVehicle;
import com.dealermanagementsysstem.project.Model.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VehicleRepository extends JpaRepository<DTOVehicle, Integer> {
    @Query(value = """
                 SELECT TOP 1 v.VehicleID
                 FROM Vehicle v\s
                 INNER JOIN VehicleVersion vv ON v.VersionID = vv.VersionID
                 INNER JOIN VehicleModel vm ON vv.ModelID = vm.ModelID
                 WHERE vm.ModelName LIKE :modelName
                     AND v.Status  = :#{#status.name()}
                 ORDER BY v.CreatedAt DESC
            \s""", nativeQuery = true)
    Integer findLatestInStockVehicleByModelName(@Param("modelName") String modelName, @Param("status") VehicleStatus status);
}
