package com.dealermanagementsysstem.project.controller;

import org.springframework.http.ResponseEntity;

import com.dealermanagementsysstem.project.Model.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/evm/vehicle")
public class EVMVehicleController {

    private final DAOVehicle dao = new DAOVehicle();

    // ===========================
    // 1️⃣ Danh sách xe
    // ===========================
    @GetMapping("/list")
    public String listVehicles(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        List<DTOVehicle> vehicles;

        try {
            if (keyword != null && !keyword.trim().isEmpty()) {
                vehicles = dao.searchVehiclesByModelName(keyword);
                model.addAttribute("keyword", keyword);
            } else {
                vehicles = dao.getVehicles();
            }

            // Ensure vehicles is never null
            if (vehicles == null) {
                vehicles = new ArrayList<>();
            }

            model.addAttribute("vehicles", vehicles);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                String role = auth.getAuthorities().iterator().next().getAuthority();

                if (role.equals("ROLE_EVM") || role.equals("ROLE_EVMSTAFF") || role.equals("ROLE_ADMIN")) {
                    model.addAttribute("actionRole", "EVM");
                } else if (role.equals("ROLE_DEALER") || role.equals("ROLE_DEALERSTAFF")) {
                    model.addAttribute("actionRole", "DEALER");
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error loading vehicle list: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("vehicles", new ArrayList<>());
            model.addAttribute("error", "Failed to load vehicles: " + e.getMessage());
        }

        return "evmPage/vehicleList";
    }


    // ===========================
    // 2️⃣ Chi tiết xe theo VIN
    // ===========================
    @GetMapping("/detail/{vin}")
    public String vehicleDetail(@PathVariable("vin") String vin, Model model) {
        DTOVehicle vehicle = dao.getVehicleByVIN(vin);
        if (vehicle == null) {
            model.addAttribute("error", "Vehicle not found for VIN: " + vin);
            return "evmPage/vehicleList";
        }
        model.addAttribute("vehicle", vehicle);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            String role = auth.getAuthorities().iterator().next().getAuthority();

            if (role.equals("ROLE_EVM") || role.equals("ROLE_EVMSTAFF") || role.equals("ROLE_ADMIN")) {
                model.addAttribute("actionRole", "EVM");
            } else if (role.equals("ROLE_DEALER") || role.equals("ROLE_DEALERSTAFF")) {
                model.addAttribute("actionRole", "DEALER");
            }
        }

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
    // ✅ Trả ảnh vehicle theo VIN (từ file system)
    // ===========================
    @GetMapping("/showImage/{vin}")
    @ResponseBody
    public ResponseEntity<byte[]> showVehicleImage(@PathVariable("vin") String vin) {
        try {
            // Look for image file in uploads directory
            String uploadDir = "src/main/resources/static/uploads/vehicle/";
            Path uploadPath = Paths.get(uploadDir);
            
            if (Files.exists(uploadPath)) {
                // Search for files that might contain the VIN in the name
                Files.list(uploadPath)
                    .filter(path -> path.getFileName().toString().contains(vin))
                    .findFirst()
                    .ifPresent(imagePath -> {
                        try {
                            byte[] imageBytes = Files.readAllBytes(imagePath);
                            ResponseEntity.ok()
                                .header("Content-Type", "image/jpeg")
                                .body(imageBytes);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
            }
            
            // If no specific image found, return a default image or 404
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    // ===========================
    // ✅ Trả ảnh vehicle theo filename
    // ===========================
    @GetMapping("/image/{filename}")
    @ResponseBody
    public ResponseEntity<byte[]> getVehicleImage(@PathVariable("filename") String filename) {
        try {
            String uploadDir = "src/main/resources/static/uploads/vehicle/";
            Path imagePath = Paths.get(uploadDir, filename);
            
            if (Files.exists(imagePath)) {
                byte[] imageBytes = Files.readAllBytes(imagePath);
                return ResponseEntity
                    .ok()
                    .header("Content-Type", "image/jpeg")
                    .body(imageBytes);
            }
            
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    // ===========================
    // 4️⃣ Xử lý tạo xe mới
    // ===========================
    
    // Exception handler for file upload size errors
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex, Model model) {
        System.out.println("❌ [ERROR] File upload size exceeded: " + ex.getMessage());
        model.addAttribute("error", "❌ File size too large. Please upload an image smaller than 10MB.");
        return "evmPage/createANewVehicleToList";
    }
    @PostMapping("/create")
    public String createVehicle(
            @RequestParam(value = "vin", required = false, defaultValue = "") String vin,
            @RequestParam(value = "colorName", required = false, defaultValue = "") String colorName,
            @RequestParam(value = "modelName", required = false, defaultValue = "") String modelName,
            @RequestParam(value = "manufactureYear", required = false, defaultValue = "0") int manufactureYear,
            @RequestParam(value = "engineNumber", required = false, defaultValue = "") String engineNumber,
            @RequestParam(value = "ownerID", required = false, defaultValue = "0") int ownerID,
            @RequestParam(value = "currentDealerID", required = false, defaultValue = "0") int currentDealerID,
            @RequestParam(value = "versionID", required = false, defaultValue = "0") int versionID,
            @RequestParam(value = "status", required = false, defaultValue = "AVAILABLE") String status,
            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail,
            Model model
    ) {
        System.out.println("🔍 [DEBUG] EVMVehicleController.createVehicle called");
        System.out.println("🔍 [DEBUG] VIN: " + vin);
        System.out.println("🔍 [DEBUG] Status: " + status);
        System.out.println("🔍 [DEBUG] Request reached controller successfully - CSRF disabled for testing");
        
        // Validate required fields
        if (vin == null || vin.trim().isEmpty()) {
            model.addAttribute("error", "VIN is required");
            return "evmPage/createANewVehicleToList";
        }

        if (manufactureYear == 0) {
            model.addAttribute("error", "Manufacture year is required");
            return "evmPage/createANewVehicleToList";
        }

        try {
            // === Validate and fetch Color ID ===
            Integer colorID = null;
            if (colorName != null && !colorName.trim().isEmpty()) {
                colorID = dao.getColorIdByName(colorName);
                if (colorID == null) {
                    model.addAttribute("error", "❌ Color not found: " + colorName);
                    return "evmPage/createANewVehicleToList";
                }
            }

            // === Validate Version ID ===
            if (versionID <= 0) {
                model.addAttribute("error", "❌ Version ID is required");
                return "evmPage/createANewVehicleToList";
            }

            // === Create DTOVehicle object ===
            DTOVehicle vehicle = new DTOVehicle();
            vehicle.setVIN(vin);
            vehicle.setManufactureYear(manufactureYear);
            vehicle.setEngineNumber(engineNumber);
            vehicle.setStatus(VehicleStatus.valueOf(status));
            vehicle.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
            vehicle.setUpdatedAt(new java.sql.Timestamp(System.currentTimeMillis()));

            // === Set relationships with proper IDs ===

            // Set color relationship with actual ColorID from database
            if (colorID != null) {
                DTOVehicleColor color = new DTOVehicleColor();
                color.setColorID(colorID);
                color.setColorName(colorName);
                vehicle.setColor(color);
            }

            // Set version relationship
            if (versionID > 0) {
                DTOVehicleVersion version = new DTOVehicleVersion();
                version.setVersionID(versionID);
                vehicle.setVersion(version);
            }

            // Set owner relationship (optional)
            if (ownerID > 0) {
                DTOCustomer owner = new DTOCustomer();
                owner.setCustomerID(ownerID);
                vehicle.setOwner(owner);
            }

            // Set current dealer relationship (optional)
            if (currentDealerID > 0) {
                DTODealer currentDealer = new DTODealer();
                currentDealer.setDealerID(currentDealerID);
                vehicle.setCurrentDealer(currentDealer);
            }

            // === Handle image upload ===
            if (thumbnail != null && !thumbnail.isEmpty()) {
                String uploadDir = "src/main/resources/static/uploads/vehicle/";
                String fileName = System.currentTimeMillis() + "_" + thumbnail.getOriginalFilename();

                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                Path filePath = uploadPath.resolve(fileName);
                Files.copy(thumbnail.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                
                System.out.println("✅ [SUCCESS] Image uploaded: " + fileName);
            }

            // === Call DAO to insert vehicle ===
            System.out.println("🔍 [DEBUG] Inserting vehicle with VIN: " + vin);
            dao.insertVehicle(vehicle);
            System.out.println("✅ [SUCCESS] Vehicle created successfully!");

            model.addAttribute("success", true);
            model.addAttribute("message", "✅ Vehicle created successfully!");

        } catch (IOException e) {
            System.err.println("❌ [ERROR] Error uploading thumbnail: " + e.getMessage());
            model.addAttribute("success", false);
            model.addAttribute("message", "⚠️ Error uploading thumbnail: " + e.getMessage());
            e.printStackTrace();
            return "evmPage/createANewVehicleToList";
        } catch (IllegalArgumentException e) {
            System.err.println("❌ [ERROR] Invalid status value: " + e.getMessage());
            model.addAttribute("success", false);
            model.addAttribute("message", "⚠️ Invalid status: " + status);
            e.printStackTrace();
            return "evmPage/createANewVehicleToList";
        } catch (Exception e) {
            System.err.println("❌ [ERROR] Unexpected error: " + e.getMessage());
            model.addAttribute("success", false);
            model.addAttribute("message", "⚠️ Error: " + e.getMessage());
            e.printStackTrace();
            return "evmPage/createANewVehicleToList";
        }

        return "redirect:/evm/vehicle/list";
    }

    // ===========================
    // 5️⃣ Form chỉnh sửa xe
    // ===========================
    @GetMapping("/edit/{vin}")
    public String showEditForm(@PathVariable("vin") String vin, Model model) {
        System.out.println("🔍 [DEBUG] EVMVehicleController.showEditForm called with VIN: " + vin);
        
        DTOVehicle vehicle = dao.getVehicleByVIN(vin);
        if (vehicle == null) {
            System.out.println("❌ [ERROR] Vehicle not found for VIN: " + vin);
            model.addAttribute("error", "Vehicle not found for VIN: " + vin);
            model.addAttribute("vehicle", null); // Explicitly set to null
            return "evmPage/editVehicle";
        }
        
        System.out.println("✅ [SUCCESS] Vehicle found: " + (vehicle.getVersion() != null && vehicle.getVersion().getModel() != null ? vehicle.getVersion().getModel().getModelName() : "Unknown"));
        model.addAttribute("vehicle", vehicle);
        return "evmPage/editVehicle";
    }

    // ===========================
    // 6️⃣ Xử lý cập nhật xe
    // ===========================
    @PostMapping("/edit/{vin}")
    public String updateVehicle(
            @PathVariable("vin") String vin,
            @RequestParam("colorName") String colorName,
            @RequestParam("modelName") String modelName,
            @RequestParam("manufactureYear") int manufactureYear,
            @RequestParam("engineNumber") String engineNumber,
            @RequestParam("ownerID") int ownerID,
            @RequestParam("currentDealerID") int currentDealerID,
            @RequestParam("versionID") int versionID,
            @RequestParam("status") String status,
            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail,
            Model model
    ) {
        System.out.println("🔍 [DEBUG] EVMVehicleController.updateVehicle called with VIN: " + vin);
        System.out.println("🔍 [DEBUG] Status value: " + status);
        try {
            // === Get existing vehicle ===
            DTOVehicle existingVehicle = dao.getVehicleByVIN(vin);
            if (existingVehicle == null) {
                model.addAttribute("error", "❌ Vehicle not found.");
                return "evmPage/editVehicle";
            }

            // === Update vehicle properties ===
            existingVehicle.setManufactureYear(manufactureYear);
            existingVehicle.setEngineNumber(engineNumber);
            existingVehicle.setStatus(VehicleStatus.valueOf(status));
            existingVehicle.setUpdatedAt(new java.sql.Timestamp(System.currentTimeMillis()));

            // === Update relationships ===
            // Set color relationship
            DTOVehicleColor color = new DTOVehicleColor();
            color.setColorName(colorName);
            existingVehicle.setColor(color);

            // Set version relationship
            DTOVehicleVersion version = new DTOVehicleVersion();
            version.setVersionID(versionID);
            existingVehicle.setVersion(version);

            // Set owner relationship
            DTOCustomer owner = new DTOCustomer();
            owner.setCustomerID(ownerID);
            existingVehicle.setOwner(owner);

            // Set current dealer relationship
            DTODealer currentDealer = new DTODealer();
            currentDealer.setDealerID(currentDealerID);
            existingVehicle.setCurrentDealer(currentDealer);

            // === Handle image upload ===
            if (thumbnail != null && !thumbnail.isEmpty()) {
                String uploadDir = "src/main/resources/static/uploads/vehicle/";
                String fileName = System.currentTimeMillis() + "_" + thumbnail.getOriginalFilename();

                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                Path filePath = uploadPath.resolve(fileName);
                Files.copy(thumbnail.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                
                System.out.println("✅ [SUCCESS] Image updated: " + fileName);
            }

            // === Call DAO to update vehicle ===
            boolean updated = dao.updateVehicle(existingVehicle);

            if (!updated) {
                model.addAttribute("error", "❌ Failed to update vehicle. Please try again.");
                return "evmPage/editVehicle";
            }

            model.addAttribute("success", true);
            model.addAttribute("message", "✅ Vehicle updated successfully!");

        } catch (IOException e) {
            model.addAttribute("error", "⚠️ Error uploading thumbnail: " + e.getMessage());
            e.printStackTrace();
            return "evmPage/editVehicle";
        } catch (Exception e) {
            model.addAttribute("error", "⚠️ Error: " + e.getMessage());
            e.printStackTrace();
            return "evmPage/editVehicle";
        }

        return "redirect:/evm/vehicle/detail/" + vin;
    }

    // ===========================
    // 7️⃣ Xử lý xóa xe
    // ===========================
    @PostMapping("/delete/{vin}")
    public String deleteVehicle(@PathVariable("vin") String vin, Model model, 
                               HttpServletRequest request) {
        System.out.println("🔍 [DEBUG] EVMVehicleController.deleteVehicle called with VIN: " + vin);
        
        // Validate VIN parameter
        if (vin == null || vin.trim().isEmpty()) {
            System.out.println("❌ [ERROR] Invalid VIN parameter");
            model.addAttribute("error", "❌ Invalid vehicle identifier.");
            return "redirect:/evm/vehicle/list";
        }
        
        try {
            // Check if vehicle exists before attempting deletion
            DTOVehicle existingVehicle = dao.getVehicleByVIN(vin);
            if (existingVehicle == null) {
                System.out.println("❌ [ERROR] Vehicle not found: " + vin);
                model.addAttribute("error", "❌ Vehicle not found.");
                return "redirect:/evm/vehicle/list";
            }
            
            // Attempt deletion
            boolean deleted = dao.deleteVehicle(vin);
            
            if (deleted) {
                System.out.println("✅ [SUCCESS] Vehicle deleted successfully: " + vin);
                String vehicleName = (existingVehicle.getVersion() != null && existingVehicle.getVersion().getModel() != null) 
                    ? existingVehicle.getVersion().getModel().getModelName() : "Unknown";
                model.addAttribute("message", "✅ Vehicle '" + vehicleName + "' deleted successfully!");
            } else {
                System.out.println("❌ [FAILED] Failed to delete vehicle: " + vin);
                model.addAttribute("error", "❌ Failed to delete vehicle. Please try again.");
            }
        } catch (Exception e) {
            System.out.println("❌ [ERROR] Exception while deleting vehicle: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "⚠️ Error deleting vehicle: " + e.getMessage());
        }

        return "redirect:/evm/vehicle/list";
    }
}
