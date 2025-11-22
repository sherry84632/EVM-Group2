package com.dealermanagementsysstem.project.service;

import com.dealermanagementsysstem.project.Model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.DBUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class VehicleComparisonService {
    
    private static final Logger log = LoggerFactory.getLogger(VehicleComparisonService.class);
    
    @Autowired
    private DAOVehicle daoVehicle;
    
    @Autowired
    private DAOVehicleModel daoVehicleModel;
    
    @Autowired
    private DAOVehicleVersion daoVehicleVersion;
    
    public List<DTOVehicleComparison> getVehiclesForComparison(List<Integer> vehicleIDs) {
        if (vehicleIDs == null || vehicleIDs.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<DTOVehicleComparison> comparisons = new ArrayList<>();
        
        for (Integer vehicleID : vehicleIDs) {
            try {
                DTOVehicle vehicle = daoVehicle.getVehicleById(vehicleID);
                if (vehicle != null) {
                    DTOVehicleComparison comparison = new DTOVehicleComparison(vehicle);
                    comparison.setImageUrl("/vehicle/image/" + vehicleID);
                    comparison.setDetailUrl("/vehicle/detail/" + vehicleID);
                    comparisons.add(comparison);
                }
            } catch (Exception e) {
                log.error("Error fetching vehicle for comparison vehicleID={}", vehicleID, e);
            }
        }
        
        return comparisons;
    }
    
    public List<DTOVehicleComparison> getVersionsForComparison(List<Integer> versionIDs) {
        if (versionIDs == null || versionIDs.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<DTOVehicleComparison> comparisons = new ArrayList<>();
        
        for (Integer versionID : versionIDs) {
            try {
                DTOVehicleVersion version = daoVehicleVersion.getVersionById(versionID);
                if (version != null) {
                    DTOVehicleComparison comparison = createComparisonFromVersion(version);
                    comparisons.add(comparison);
                }
            } catch (Exception e) {
                log.error("Error fetching version for comparison versionID={}", versionID, e);
            }
        }
        
        return comparisons;
    }
    
    public Map<String, List<Object>> getComparisonMatrix(List<DTOVehicleComparison> vehicles) {
        Map<String, List<Object>> matrix = new LinkedHashMap<>();
        
        if (vehicles == null || vehicles.isEmpty()) {
            return matrix;
        }
        
        matrix.put("Model", vehicles.stream()
            .map(v -> v.getDisplayName() != null ? v.getDisplayName() : "N/A")
            .collect(Collectors.toList()));
        
        matrix.put("Brand", vehicles.stream()
            .map(v -> v.getBrand() != null ? v.getBrand() : "N/A")
            .collect(Collectors.toList()));
        
        matrix.put("Year", vehicles.stream()
            .map(v -> v.getYear() != null ? v.getYear().toString() : "N/A")
            .collect(Collectors.toList()));
        
        matrix.put("Body Type", vehicles.stream()
            .map(v -> v.getBodyType() != null ? v.getBodyType() : "N/A")
            .collect(Collectors.toList()));
        
        matrix.put("Version", vehicles.stream()
            .map(v -> v.getVersionName() != null ? v.getVersionName() : "N/A")
            .collect(Collectors.toList()));
        
        matrix.put("Engine", vehicles.stream()
            .map(v -> v.getEngine() != null ? v.getEngine() : "N/A")
            .collect(Collectors.toList()));
        
        matrix.put("Transmission", vehicles.stream()
            .map(v -> v.getTransmission() != null ? v.getTransmission() : "N/A")
            .collect(Collectors.toList()));
        
        matrix.put("Color", vehicles.stream()
            .map(v -> v.getColorName() != null ? v.getColorName() : "N/A")
            .collect(Collectors.toList()));
        
        matrix.put("Base Price", vehicles.stream()
            .map(v -> v.getBasePrice() != null ? formatPrice(v.getBasePrice()) : "N/A")
            .collect(Collectors.toList()));
        
        matrix.put("Dealer Price", vehicles.stream()
            .map(v -> v.getDealerSellingPrice() != null ? formatPrice(v.getDealerSellingPrice()) : "N/A")
            .collect(Collectors.toList()));
        
        matrix.put("Final Price", vehicles.stream()
            .map(v -> v.getFinalPrice() != null ? formatPrice(v.getFinalPrice()) : "N/A")
            .collect(Collectors.toList()));
        
        matrix.put("Available", vehicles.stream()
            .map(v -> v.isAvailable() ? "Yes" : "No")
            .collect(Collectors.toList()));
        
        return matrix;
    }
    
    public List<String> getComparisonSpecs() {
        return Arrays.asList(
            "Model", "Brand", "Year", "Body Type", "Version", 
            "Engine", "Transmission", "Color", 
            "Base Price", "Dealer Price", "Final Price", "Available"
        );
    }
    
    private DTOVehicleComparison createComparisonFromVersion(DTOVehicleVersion version) {
        DTOVehicleComparison comparison = new DTOVehicleComparison();
        comparison.setVersionID(version.getVersionID());
        comparison.setVersionName(version.getVersionName());
        comparison.setEngine(version.getEngine());
        comparison.setTransmission(version.getTransmission());
        
        if (version.getModel() != null) {
            DTOVehicleModel model = version.getModel();
            comparison.setModelID(model.getModelID());
            comparison.setModelName(model.getModelName());
            comparison.setBrand(model.getBrand());
            comparison.setYear(model.getYear());
            comparison.setBodyType(model.getBodyType());
            comparison.setModelDescription(model.getDescription());
            comparison.setModelImage(model.getModelImage());
            comparison.setBasePrice(model.getBasePrice());
            comparison.setDealerSellingPrice(model.getDealerSellingPrice());
            comparison.setFinalPrice(model.getDealerSellingPrice() != null 
                ? model.getDealerSellingPrice() 
                : model.getBasePrice());
            comparison.setImageUrl("/vehicle/model/image/" + model.getModelID());
        }
        
        String sql = "SELECT Engine, Transmission FROM VehicleVersion WHERE VersionID = ?";
        try (java.sql.Connection conn = DBUtils.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, version.getVersionID());
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    if (comparison.getEngine() == null) comparison.setEngine(rs.getString("Engine"));
                    if (comparison.getTransmission() == null) comparison.setTransmission(rs.getString("Transmission"));
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch engine/transmission for versionID={}", version.getVersionID(), e);
        }
        
        return comparison;
    }
    
    private String formatPrice(java.math.BigDecimal price) {
        if (price == null) return "N/A";
        return String.format("%,.0f VND", price.doubleValue());
    }
}

