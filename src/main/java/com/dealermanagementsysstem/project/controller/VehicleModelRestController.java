package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DAOVehicleModel;
import com.dealermanagementsysstem.project.Model.DTOVehicleModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Base64;

@RestController
@RequestMapping("/api/vehicle-model")
public class VehicleModelRestController {

    private final DAOVehicleModel daoVehicleModel = new DAOVehicleModel();

    @GetMapping("/{id}")
    public ResponseEntity<?> getVehicleModel(@PathVariable("id") int id) {
        DTOVehicleModel model = daoVehicleModel.getModelById(id);
        if (model == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Vehicle model not found");
            err.put("modelID", id);
            return ResponseEntity.status(404).body(err);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("modelID", model.getModelID());
        data.put("modelName", model.getModelName());
        data.put("brand", model.getBrand());
        data.put("year", model.getYear());
        data.put("bodyType", model.getBodyType());
        data.put("basePrice", model.getBasePrice() != null ? model.getBasePrice() : null);
        data.put("description", model.getDescription());
        if (model.getModelImage() != null && model.getModelImage().length > 0) {
            data.put("imageBase64", Base64.getEncoder().encodeToString(model.getModelImage()));
        }
        return ResponseEntity.ok(data);
    }

    @GetMapping("s/all")
    public ResponseEntity<?> getAllVehicleModels() {
        java.util.List<DTOVehicleModel> models = daoVehicleModel.getAllModels();
        java.util.List<Map<String, Object>> data = new java.util.ArrayList<>();

        for (DTOVehicleModel model : models) {
            Map<String, Object> item = new HashMap<>();
            item.put("modelID", model.getModelID());
            item.put("modelId", model.getModelID()); // Both for compatibility
            item.put("modelName", model.getModelName());
            item.put("brand", model.getBrand());
            item.put("year", model.getYear());
            item.put("bodyType", model.getBodyType());
            item.put("basePrice", model.getBasePrice() != null ? model.getBasePrice() : null);
            item.put("description", model.getDescription());
            if (model.getModelImage() != null && model.getModelImage().length > 0) {
                item.put("imageBase64", Base64.getEncoder().encodeToString(model.getModelImage()));
            }
            data.add(item);
        }

        return ResponseEntity.ok(data);
    }
}
