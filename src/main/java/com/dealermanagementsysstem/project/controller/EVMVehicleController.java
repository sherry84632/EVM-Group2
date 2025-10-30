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
    // 1️⃣ Danh sách xe MẪU (Catalog) - Ưu tiên hiển thị xe TEMPLATE
    // ===========================
    @GetMapping("/list")
    public String listVehicles(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        List<DTOVehicle> vehicles;
        try {
            if (keyword != null && !keyword.trim().isEmpty()) {
                // Search theo keyword
                vehicles = dao.searchVehiclesByModelName(keyword);

                // Ưu tiên filter lấy TEMPLATE, nếu không có thì lấy tất cả
                List<DTOVehicle> templateVehicles = vehicles.stream()
                        .filter(v -> v.getStatus() == VehicleStatus.TEMPLATE)
                        .toList();

                if (!templateVehicles.isEmpty()) {
                    vehicles = templateVehicles;
                }
                // Nếu không có TEMPLATE, giữ nguyên kết quả search (tất cả xe)

            } else {
                // Lấy xe TEMPLATE trước
                vehicles = dao.getVehiclesByStatus(VehicleStatus.TEMPLATE);

                // Nếu không có xe TEMPLATE nào, lấy tất cả xe (backward compatibility)
                if (vehicles.isEmpty()) {
                    log.warn("No TEMPLATE vehicles found, showing all vehicles for backward compatibility");
                    vehicles = dao.getVehicles();
                }
            }

            if (vehicles == null) vehicles = new ArrayList<>();
            model.addAttribute("vehicles", vehicles);
            model.addAttribute("keyword", keyword);

            // Thêm warning nếu đang hiển thị tất cả xe (chưa có TEMPLATE)
            if (vehicles.stream().anyMatch(v -> v.getStatus() != VehicleStatus.TEMPLATE)) {
                model.addAttribute("warning", "⚠️ Hiển thị tất cả xe. Vui lòng cập nhật Status = TEMPLATE cho xe mẫu catalog.");
            }

            addActionRole(model);
        } catch (Exception e) {
            log.error("Error loading vehicle list", e);
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
    // 📷 Trả ảnh model từ database theo ModelID
    // ===========================
    @GetMapping("/modelImage/{modelId}")
    @ResponseBody
    public ResponseEntity<byte[]> getModelImageFromDatabase(@PathVariable Integer modelId) {
        try {
            log.info("Fetching model image from database for ModelID={}", modelId);

            byte[] imageBytes = dao.getModelImage(modelId);

            if (imageBytes != null && imageBytes.length > 0) {
                log.info("✅ Returning model image, size={} bytes", imageBytes.length);
                return ResponseEntity
                        .ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(imageBytes);
            }

            log.warn("⚠️ No image found in database for ModelID={}", modelId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);

        } catch (Exception e) {
            log.error("❌ Error fetching model image for ModelID={}: {}", modelId, e.getMessage());
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
            @RequestParam(value = "status", required = false, defaultValue = "TEMPLATE") String status,
            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail,
            Model model
    ) {
        log.info("Creating vehicle: model={}, version={}, color={}", modelName, versionName, colorName);

        // ========== VALIDATION ==========
        if (manufactureYear <= 0) {
            model.addAttribute("error", "❌ Manufacture year is required");
            return "evmPage/createANewVehicleToList";
        }
        if (modelName == null || modelName.isBlank()) {
            model.addAttribute("error", "❌ Model name is required");
            return "evmPage/createANewVehicleToList";
        }
        if (versionName == null || versionName.isBlank()) {
            model.addAttribute("error", "❌ Version name is required");
            return "evmPage/createANewVehicleToList";
        }
        if (colorName == null || colorName.isBlank()) {
            model.addAttribute("error", "❌ Color name is required");
            return "evmPage/createANewVehicleToList";
        }

        try {
            // ========== PARSE BASE PRICE ==========
            java.math.BigDecimal basePrice = java.math.BigDecimal.ZERO;
            if (basePriceStr != null && !basePriceStr.isBlank()) {
                try {
                    basePrice = new java.math.BigDecimal(basePriceStr);
                } catch (NumberFormatException e) {
                    model.addAttribute("error", "❌ Invalid base price format");
                    return "evmPage/createANewVehicleToList";
                }
            }

            // ========== STEP 1: GET OR CREATE MODEL ==========
            Integer modelID = dao.getOrCreateModel(
                modelName,
                brand != null ? brand : "Unknown",
                bodyType != null ? bodyType : "Unknown",
                modelYear > 0 ? modelYear : manufactureYear,
                basePrice,
                modelDescription != null ? modelDescription : ""
            );

            if (modelID == null) {
                model.addAttribute("error", "❌ Failed to create/get model: " + modelName);
                return "evmPage/createANewVehicleToList";
            }
            log.info("✅ Model ready: {} (ID={})", modelName, modelID);

            // ========== STEP 2: GET OR CREATE VERSION ==========
            Integer versionID = dao.getOrCreateVersion(
                modelID,
                versionName,
                engine != null ? engine : "Unknown",
                transmission != null ? transmission : "Unknown"
            );

            if (versionID == null) {
                model.addAttribute("error", "❌ Failed to create/get version: " + versionName);
                return "evmPage/createANewVehicleToList";
            }
            log.info("✅ Version ready: {} (ID={})", versionName, versionID);

            // ========== STEP 3: GET OR CREATE COLOR ==========
            Integer colorID = dao.getOrCreateColor(colorName);

            if (colorID == null) {
                model.addAttribute("error", "❌ Failed to create/get color: " + colorName);
                return "evmPage/createANewVehicleToList";
            }
            log.info("✅ Color ready: {} (ID={})", colorName, colorID);

            // ========== STEP 4: BUILD VEHICLE OBJECT ==========
            DTOVehicle vehicle = new DTOVehicle();
            vehicle.setManufactureYear(manufactureYear);
            vehicle.setEngineNumber(engineNumber != null ? engineNumber : "ENG" + System.currentTimeMillis());
            vehicle.setStatus(VehicleStatus.valueOf(status));
            vehicle.setDescription(modelDescription); // Save model description to vehicle
            vehicle.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
            vehicle.setUpdatedAt(new java.sql.Timestamp(System.currentTimeMillis()));

            // Set Color
            DTOVehicleColor color = new DTOVehicleColor();
            color.setColorID(colorID);
            color.setColorName(colorName);
            vehicle.setColor(color);

            // Set Version
            DTOVehicleVersion version = new DTOVehicleVersion();
            version.setVersionID(versionID);
            version.setVersionName(versionName);
            vehicle.setVersion(version);

            // ========== STEP 5: INSERT VEHICLE ==========
            dao.insertVehicle(vehicle);

            if (vehicle.getVehicleID() == null || vehicle.getVehicleID() <= 0) {
                model.addAttribute("error", "❌ Failed to insert vehicle into database");
                return "evmPage/createANewVehicleToList";
            }
            log.info("✅ Vehicle inserted with ID={}", vehicle.getVehicleID());

            // ========== STEP 6: UPLOAD THUMBNAIL & SAVE TO DATABASE ==========
            if (thumbnail != null && !thumbnail.isEmpty()) {
                try {
                    // 1. Upload file to disk
                    Path uploadPath = Paths.get(UPLOAD_DIR);
                    if (!Files.exists(uploadPath)) {
                        Files.createDirectories(uploadPath);
                    }

                    String fileName = System.currentTimeMillis() + "_"
                                    + vehicle.getVehicleID() + "_"
                                    + thumbnail.getOriginalFilename();

                    Files.copy(
                        thumbnail.getInputStream(),
                        uploadPath.resolve(fileName),
                        StandardCopyOption.REPLACE_EXISTING
                    );
                    log.info("✅ Thumbnail uploaded to disk: {}", fileName);

                    // 2. Save image to database (VehicleModel.ModelImage)
                    try {
                        byte[] imageBytes = thumbnail.getBytes();
                        boolean imageSaved = dao.updateModelImage(modelID, imageBytes);
                        if (imageSaved) {
                            log.info("✅ Model image saved to database for ModelID={}", modelID);
                        } else {
                            log.warn("⚠️ Failed to save model image to database for ModelID={}", modelID);
                        }
                    } catch (IOException e) {
                        log.error("⚠️ Error reading image bytes: {}", e.getMessage());
                    }

                } catch (IOException e) {
                    log.error("⚠️ Error uploading thumbnail: {}", e.getMessage());
                    // Don't fail the whole operation, just log the error
                }
            } else {
                log.info("ℹ️ No thumbnail uploaded for vehicle ID={}", vehicle.getVehicleID());
            }

            // ========== SUCCESS ==========
            log.info("🎉 Vehicle created successfully! ID={}, Model={}, Version={}, Color={}",
                    vehicle.getVehicleID(), modelName, versionName, colorName);

            model.addAttribute("message", "✅ Vehicle created successfully! ID: " + vehicle.getVehicleID());
            return "redirect:/evm/vehicle/list";

        } catch (IllegalArgumentException e) {
            log.error("Invalid status value: {}", status, e);
            model.addAttribute("error", "❌ Invalid status: " + status);
            return "evmPage/createANewVehicleToList";
        } catch (Exception e) {
            log.error("❌ Unexpected error creating vehicle", e);
            model.addAttribute("error", "❌ Unexpected error: " + e.getMessage());
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
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail,
            Model model
    ) {
        log.info("Updating vehicle ID={}: model={}, version={}, color={}", id, modelName, versionName, colorName);

        try {
            // Get existing vehicle
            DTOVehicle existing = dao.getVehicleById(id);
            if (existing == null) {
                model.addAttribute("error", "❌ Vehicle not found.");
                return "evmPage/editVehicle";
            }

            // Parse base price - use existing if not provided
            java.math.BigDecimal basePrice = null;
            if (basePriceStr != null && !basePriceStr.isBlank()) {
                try {
                    basePrice = new java.math.BigDecimal(basePriceStr);
                } catch (NumberFormatException e) {
                    model.addAttribute("error", "❌ Invalid base price format");
                    model.addAttribute("vehicle", existing);
                    return "evmPage/editVehicle";
                }
            } else if (existing.getVersion() != null && existing.getVersion().getModel() != null) {
                basePrice = existing.getVersion().getModel().getBasePrice();
            }
            if (basePrice == null) {
                basePrice = java.math.BigDecimal.ZERO;
            }

            // Get existing values for fallback
            String existingModelName = existing.getVersion() != null && existing.getVersion().getModel() != null
                ? existing.getVersion().getModel().getModelName() : null;
            String existingBrand = existing.getVersion() != null && existing.getVersion().getModel() != null
                ? existing.getVersion().getModel().getBrand() : "Unknown";
            String existingBodyType = existing.getVersion() != null && existing.getVersion().getModel() != null
                ? existing.getVersion().getModel().getBodyType() : "Unknown";
            String existingVersionName = existing.getVersion() != null
                ? existing.getVersion().getVersionName() : null;
            String existingEngine = existing.getVersion() != null
                ? existing.getVersion().getEngine() : "Unknown";
            String existingTransmission = existing.getVersion() != null
                ? existing.getVersion().getTransmission() : "Unknown";
            String existingColorName = existing.getColor() != null
                ? existing.getColor().getColorName() : null;

            // Get or Create Model - use existing values as fallback
            Integer modelID = null;
            String finalModelName = (modelName != null && !modelName.isBlank()) ? modelName : existingModelName;
            String finalBrand = (brand != null && !brand.isBlank()) ? brand : existingBrand;
            String finalBodyType = (bodyType != null && !bodyType.isBlank()) ? bodyType : existingBodyType;
            int finalYear = modelYear > 0 ? modelYear : (manufactureYear > 0 ? manufactureYear : existing.getManufactureYear());
            String finalDescription = (modelDescription != null && !modelDescription.isBlank()) ? modelDescription : "";

            if (finalModelName != null && !finalModelName.isBlank()) {
                modelID = dao.getOrCreateModel(
                    finalModelName,
                    finalBrand,
                    finalBodyType,
                    finalYear,
                    basePrice,
                    finalDescription
                );
                log.debug("Model: {} → ID={}", finalModelName, modelID);
            }

            // Get or Create Version - use existing values as fallback
            Integer versionID = null;
            String finalVersionName = (versionName != null && !versionName.isBlank()) ? versionName : existingVersionName;
            String finalEngine = (engine != null && !engine.isBlank()) ? engine : existingEngine;
            String finalTransmission = (transmission != null && !transmission.isBlank()) ? transmission : existingTransmission;

            if (finalVersionName != null && !finalVersionName.isBlank() && modelID != null) {
                versionID = dao.getOrCreateVersion(
                    modelID,
                    finalVersionName,
                    finalEngine,
                    finalTransmission
                );
                log.debug("Version: {} → ID={}", finalVersionName, versionID);
            }

            // Get or Create Color - use existing value as fallback
            Integer colorID = null;
            String finalColorName = (colorName != null && !colorName.isBlank()) ? colorName : existingColorName;

            if (finalColorName != null && !finalColorName.isBlank()) {
                colorID = dao.getOrCreateColor(finalColorName);
                log.debug("Color: {} → ID={}", finalColorName, colorID);
            }

            // Update vehicle fields
            if (manufactureYear > 0) {
                existing.setManufactureYear(manufactureYear);
                log.debug("Updated manufactureYear: {}", manufactureYear);
            }
            if (engineNumber != null && !engineNumber.isBlank()) {
                existing.setEngineNumber(engineNumber);
                log.debug("Updated engineNumber: {}", engineNumber);
            }
            if (status != null && !status.isBlank()) {
                existing.setStatus(VehicleStatus.valueOf(status));
                log.debug("Updated status: {}", status);
            }
            if (modelDescription != null) {
                existing.setDescription(modelDescription);
                log.debug("Updated description");
            }
            existing.setUpdatedAt(new java.sql.Timestamp(System.currentTimeMillis()));

            // Update color - ALWAYS set if colorID obtained
            if (colorID != null && colorID > 0) {
                DTOVehicleColor color = new DTOVehicleColor();
                color.setColorID(colorID);
                color.setColorName(colorName);
                existing.setColor(color);
                log.debug("Updated color: {} (ID={})", colorName, colorID);
            } else {
                log.warn("ColorID is null or invalid, keeping existing color");
            }

            // Update version - ALWAYS set if versionID obtained
            if (versionID != null && versionID > 0) {
                DTOVehicleVersion version = new DTOVehicleVersion();
                version.setVersionID(versionID);
                existing.setVersion(version);
                log.debug("Updated version: {} (ID={})", versionName, versionID);
            } else {
                log.warn("VersionID is null or invalid, keeping existing version");
            }

            log.info("Prepared vehicle for update: ID={}, ColorID={}, VersionID={}, Year={}, Status={}",
                    id,
                    existing.getColor() != null ? existing.getColor().getColorID() : "null",
                    existing.getVersion() != null ? existing.getVersion().getVersionID() : "null",
                    existing.getManufactureYear(),
                    existing.getStatus());

            // Handle thumbnail upload
            if (thumbnail != null && !thumbnail.isEmpty()) {
                try {
                    Path uploadPath = Paths.get(UPLOAD_DIR);
                    if (!Files.exists(uploadPath)) {
                        Files.createDirectories(uploadPath);
                    }
                    String fileName = System.currentTimeMillis() + "_" + id + "_" + thumbnail.getOriginalFilename();
                    Files.copy(thumbnail.getInputStream(), uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                    log.info("✅ Thumbnail updated: {}", fileName);
                } catch (IOException e) {
                    log.error("⚠️ Error uploading thumbnail: {}", e.getMessage());
                }
            }

            // Save to database - Vehicle table
            boolean updated = dao.updateVehicle(existing);
            if (!updated) {
                model.addAttribute("error", "❌ Failed to update vehicle. Please try again.");
                model.addAttribute("vehicle", existing);
                return "evmPage/editVehicle";
            }

            // Update VehicleModel table (brand, bodyType, year, basePrice, description)
            if (modelID != null && modelID > 0) {
                boolean modelUpdated = dao.updateModel(
                    modelID,
                    finalBrand,
                    finalBodyType,
                    finalYear,
                    basePrice,
                    finalDescription
                );
                if (modelUpdated) {
                    log.info("✅ VehicleModel updated: ID={}, BasePrice={}", modelID, basePrice);
                } else {
                    log.warn("⚠️ VehicleModel update failed for ID={}", modelID);
                }
            }

            // Update VehicleVersion table (engine, transmission)
            if (versionID != null && versionID > 0) {
                boolean versionUpdated = dao.updateVersion(
                    versionID,
                    finalEngine,
                    finalTransmission
                );
                if (versionUpdated) {
                    log.info("✅ VehicleVersion updated: ID={}, Engine={}, Transmission={}", versionID, finalEngine, finalTransmission);
                } else {
                    log.warn("⚠️ VehicleVersion update failed for ID={}", versionID);
                }
            }

            log.info("✅ Vehicle updated successfully! ID={}", id);
            model.addAttribute("message", "✅ Vehicle updated successfully!");
            return "redirect:/evm/vehicle/detail/" + id;

        } catch (IllegalArgumentException e) {
            log.error("Invalid status value: {}", status, e);
            model.addAttribute("error", "❌ Invalid status: " + status);
            DTOVehicle existing = dao.getVehicleById(id);
            model.addAttribute("vehicle", existing);
            return "evmPage/editVehicle";
        } catch (Exception e) {
            log.error("❌ Error updating vehicle ID={}", id, e);
            model.addAttribute("error", "❌ Error: " + e.getMessage());
            DTOVehicle existing = dao.getVehicleById(id);
            model.addAttribute("vehicle", existing);
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
