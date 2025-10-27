package com.dealermanagementsysstem.project.controller;

import org.springframework.http.ResponseEntity;
import com.dealermanagementsysstem.project.Model.*;
import com.dealermanagementsysstem.project.util.SecurityUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/evm/vehicle")
public class EVMVehicleController {

    private static final Logger log = LoggerFactory.getLogger(EVMVehicleController.class);
    private final DAOVehicle dao;
    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/vehicle/";

    public EVMVehicleController(DAOVehicle dao) {
        this.dao = dao;
    }

    // ===========================
    // 1️⃣ Danh sách xe
    // ===========================
    @GetMapping("/list")
    public String listVehicles(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        List<DTOVehicle> vehicles;
        try {
            vehicles = (keyword != null && !keyword.trim().isEmpty())
                    ? dao.searchVehiclesByModelName(keyword)
                    : dao.getVehicles();
            if (vehicles == null) vehicles = new ArrayList<>();
            model.addAttribute("vehicles", vehicles);
            model.addAttribute("keyword", keyword);
            addActionRole(model);
        } catch (Exception e) {
            model.addAttribute("vehicles", new ArrayList<>());
            model.addAttribute("error", "Failed to load vehicles: " + e.getMessage());
        }
        return "evmPage/vehicleList";
    }

    // ===========================
    // 2️⃣ Chi tiết xe theo ID
    // ===========================
    @GetMapping("/detail/{id}")
    public String vehicleDetail(@PathVariable Integer id, Model model) {
        DTOVehicle vehicle = dao.getVehicleById(id);
        if (vehicle == null) {
            model.addAttribute("error", "Vehicle not found for ID: " + id);
            return "evmPage/vehicleList";
        }
        model.addAttribute("vehicle", vehicle);
        addActionRole(model);
        return "evmPage/vehicleListDetail";
    }

    // ===========================
    // 3️⃣ Form tạo xe mới
    // ===========================
    @GetMapping("/create")
    public String showCreateForm() {
        return "evmPage/createANewVehicleToList";
    }

    // ===========================
    // 📷 Trả ảnh vehicle theo ID
    // ===========================
    @GetMapping("/showImage/{id}")
    @ResponseBody
    public ResponseEntity<byte[]> showVehicleImage(@PathVariable Integer id) {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (Files.exists(uploadPath)) {
                try (var stream = Files.list(uploadPath)) {
                    Path found = stream.filter(path -> path.getFileName().toString().contains("_" + id + "_")).findFirst().orElse(null);
                    if (found != null) {
                        byte[] bytes = Files.readAllBytes(found);
                        return ResponseEntity
                                .ok()
                                .contentType(MediaType.IMAGE_JPEG)
                                .body(bytes);
                    }
                }
            }
            // Return 404 with explicit byte[] body type
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // ===========================
    // 📷 Trả ảnh vehicle theo filename
    // ===========================
    @GetMapping("/image/{filename}")
    @ResponseBody
    public ResponseEntity<byte[]> getVehicleImage(@PathVariable String filename) {
        try {
            Path imagePath = Paths.get(UPLOAD_DIR, filename);
            if (Files.exists(imagePath)) {
                byte[] imageBytes = Files.readAllBytes(imagePath);
                return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(imageBytes);
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // ===========================
    // 4️⃣ Xử lý tạo xe mới
    // ===========================
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex, Model model) {
        model.addAttribute("error", "📛 File size too large. Please upload an image smaller than 10MB.");
        return "evmPage/createANewVehicleToList";
    }

    @PostMapping("/create")
    public String createVehicle(
            @RequestParam(value = "colorName", required = false) String colorName,
            @RequestParam(value = "modelName", required = false) String modelName,
            @RequestParam(value = "brand", required = false) String brand,
            @RequestParam(value = "bodyType", required = false) String bodyType,
            @RequestParam(value = "year", required = false, defaultValue = "0") int modelYear,
            @RequestParam(value = "description", required = false) String modelDescription,
            @RequestParam(value = "basePrice", required = false) String basePriceStr,
            @RequestParam(value = "versionName", required = false) String versionName,
            @RequestParam(value = "engine", required = false) String engine,
            @RequestParam(value = "transmission", required = false) String transmission,
            @RequestParam(value = "manufactureYear", required = false, defaultValue = "0") int manufactureYear,
            @RequestParam(value = "engineNumber", required = false) String engineNumber,
            @RequestParam(value = "evmID", required = false, defaultValue = "1") int evmID,
            @RequestParam(value = "status", required = false, defaultValue = "IN_STOCK") String status,
            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail,
            Model model
    ) {
        // Validation
        if (manufactureYear <= 0) {
            model.addAttribute("error", "Manufacture year is required");
            return "evmPage/createANewVehicleToList";
        }
        if (modelName == null || modelName.isBlank()) {
            model.addAttribute("error", "Model name is required");
            return "evmPage/createANewVehicleToList";
        }
        if (versionName == null || versionName.isBlank()) {
            model.addAttribute("error", "Version name is required");
            return "evmPage/createANewVehicleToList";
        }

        try {
            // Parse base price
            java.math.BigDecimal basePrice = null;
            if (basePriceStr != null && !basePriceStr.isBlank()) {
                basePrice = new java.math.BigDecimal(basePriceStr);
            }

            // === Step 1: Get or Create VehicleModel ===
            Integer modelID = dao.getModelIdByName(modelName);
            if (modelID == null) {
                // Create new model (you need to add this method to DAO)
                log.info("Model not found, creating new model: {}", modelName);
                // For now, return error - you should implement createModel in DAO
                model.addAttribute("error", "Model '" + modelName + "' not found. Please create it first in EVM system.");
                return "evmPage/createANewVehicleToList";
            }

            // === Step 2: Get or Create VehicleVersion ===
            // You need to add getVersionIdByModelAndName method to DAO
            // For now, we'll assume versionID=1 exists, but this needs proper implementation
            int versionID = 1; // TODO: Implement proper version lookup/creation

            // === Step 3: Get ColorID ===
            Integer colorID = null;
            if (colorName != null && !colorName.isBlank()) {
                colorID = dao.getColorIdByName(colorName);
                if (colorID == null) {
                    model.addAttribute("error", "Color not found: " + colorName);
                    return "evmPage/createANewVehicleToList";
                }
            }

            // === Step 4: Build Vehicle ===
            DTOVehicle vehicle = new DTOVehicle();
            vehicle.setManufactureYear(manufactureYear);
            vehicle.setEngineNumber(engineNumber);
            vehicle.setStatus(VehicleStatus.valueOf(status));
            vehicle.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
            vehicle.setUpdatedAt(new java.sql.Timestamp(System.currentTimeMillis()));

            if (colorID != null) {
                DTOVehicleColor color = new DTOVehicleColor();
                color.setColorID(colorID);
                color.setColorName(colorName);
                vehicle.setColor(color);
            }

            if (versionID > 0) {
                DTOVehicleVersion version = new DTOVehicleVersion();
                version.setVersionID(versionID);
                vehicle.setVersion(version);
            }

            // === Step 5: Insert vehicle first to get ID ===
            dao.insertVehicle(vehicle);

            // === Step 6: Handle image upload with vehicle ID ===
            if (thumbnail != null && !thumbnail.isEmpty() && vehicle.getVehicleID() != null) {
                Path uploadPath = Paths.get(UPLOAD_DIR);
                if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
                String fileName = System.currentTimeMillis() + "_" + vehicle.getVehicleID() + "_" + thumbnail.getOriginalFilename();
                Files.copy(thumbnail.getInputStream(), uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            }

            model.addAttribute("message", "✅ Vehicle created successfully!");
            return "redirect:/evm/vehicle/list";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", "Invalid status: " + status);
            return "evmPage/createANewVehicleToList";
        } catch (IOException e) {
            model.addAttribute("error", "Error uploading thumbnail: " + e.getMessage());
            return "evmPage/createANewVehicleToList";
        } catch (Exception e) {
            log.error("Unexpected error creating vehicle: {}", e.getMessage(), e);
            model.addAttribute("error", "Unexpected error: " + e.getMessage());
            return "evmPage/createANewVehicleToList";
        }
    }

    // ===========================
    // 5️⃣ Form chỉnh sửa xe
    // ===========================
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        DTOVehicle vehicle = dao.getVehicleById(id);
        if (vehicle == null) {
            model.addAttribute("error", "Vehicle not found for ID: " + id);
            return "evmPage/editVehicle";
        }
        model.addAttribute("vehicle", vehicle);
        return "evmPage/editVehicle";
    }

    // ===========================
    // 6️⃣ Xử lý cập nhật xe
    // ===========================
    @PostMapping("/edit/{id}")
    public String updateVehicle(
            @PathVariable Integer id,
            @RequestParam("colorName") String colorName,
            @RequestParam("modelName") String modelName,
            @RequestParam("manufactureYear") int manufactureYear,
            @RequestParam("engineNumber") String engineNumber,
            @RequestParam("versionID") int versionID,
            @RequestParam("status") String status,
            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail,
            Model model
    ) {
        try {
            DTOVehicle existing = dao.getVehicleById(id);
            if (existing == null) {
                model.addAttribute("error", "Vehicle not found.");
                return "evmPage/editVehicle";
            }
            existing.setManufactureYear(manufactureYear);
            existing.setEngineNumber(engineNumber);
            existing.setStatus(VehicleStatus.valueOf(status));
            existing.setUpdatedAt(new java.sql.Timestamp(System.currentTimeMillis()));

            DTOVehicleColor color = new DTOVehicleColor();
            color.setColorName(colorName);
            existing.setColor(color);

            DTOVehicleVersion version = new DTOVehicleVersion();
            version.setVersionID(versionID);
            // Attempt to resolve model by name if provided
            if (modelName != null && !modelName.isBlank()) {
                Integer modelId = dao.getModelIdByName(modelName);
                if (modelId != null) {
                    DTOVehicleModel m = new DTOVehicleModel();
                    m.setModelID(modelId);
                    m.setModelName(modelName);
                    version.setModel(m);
                }
            }
            existing.setVersion(version);

            if (thumbnail != null && !thumbnail.isEmpty()) {
                Path uploadPath = Paths.get(UPLOAD_DIR);
                if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
                String fileName = System.currentTimeMillis() + "_" + id + "_" + thumbnail.getOriginalFilename();
                Files.copy(thumbnail.getInputStream(), uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            }

            boolean updated = dao.updateVehicle(existing);
            if (!updated) {
                model.addAttribute("error", "Failed to update vehicle. Please try again.");
                return "evmPage/editVehicle";
            }
            return "redirect:/evm/vehicle/detail/" + id;
        } catch (IOException e) {
            model.addAttribute("error", "Error uploading thumbnail: " + e.getMessage());
            return "evmPage/editVehicle";
        } catch (Exception e) {
            model.addAttribute("error", "Error: " + e.getMessage());
            return "evmPage/editVehicle";
        }
    }

    // ===========================
    // 7️⃣ Xử lý xóa xe
    // ===========================
    @PostMapping("/delete/{id}")
    public String deleteVehicle(@PathVariable Integer id, Model model) {
        if (id == null) {
            model.addAttribute("error", "Invalid vehicle identifier.");
            return "redirect:/evm/vehicle/list";
        }
        try {
            DTOVehicle existing = dao.getVehicleById(id);
            if (existing == null) {
                model.addAttribute("error", "Vehicle not found.");
                return "redirect:/evm/vehicle/list";
            }
            boolean deleted = dao.deleteVehicle(id);
            if (!deleted) {
                model.addAttribute("error", "Failed to delete vehicle. Please try again.");
            } else {
                model.addAttribute("message", "✅ Vehicle deleted successfully!");
            }
        } catch (Exception e) {
            model.addAttribute("error", "Error deleting vehicle: " + e.getMessage());
        }
        return "redirect:/evm/vehicle/list";
    }

    private void addActionRole(Model model) {
        String role = SecurityUtil.getCurrentUserRole();
        if (role == null) return;
        if (role.equals("ROLE_EVM") || role.equals("ROLE_EVMSTAFF") || role.equals("ROLE_ADMIN")) {
            model.addAttribute("actionRole", "EVM");
        } else if (role.equals("ROLE_DEALER") || role.equals("ROLE_DEALERSTAFF")) {
            model.addAttribute("actionRole", "DEALER");
        }
    }
}
