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

@Controller
@RequestMapping("/evm/vehicle")
public class EVMVehicleController {

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
    // 2️⃣ Chi tiết xe theo VIN
    // ===========================
    @GetMapping("/detail/{vin}")
    public String vehicleDetail(@PathVariable String vin, Model model) {
        DTOVehicle vehicle = dao.getVehicleByVIN(vin);
        if (vehicle == null) {
            model.addAttribute("error", "Vehicle not found for VIN: " + vin);
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
    // 📷 Trả ảnh vehicle theo VIN
    // ===========================
    @GetMapping("/showImage/{vin}")
    @ResponseBody
    public ResponseEntity<byte[]> showVehicleImage(@PathVariable String vin) {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (Files.exists(uploadPath)) {
                return Files.list(uploadPath)
                        .filter(path -> path.getFileName().toString().contains(vin))
                        .findFirst()
                        .map(path -> {
                            try {
                                byte[] bytes = Files.readAllBytes(path);
                                return ResponseEntity.ok().header("Content-Type", "image/jpeg").body(bytes);
                            } catch (IOException e) {
                                return ResponseEntity.notFound().build();
                            }
                        })
                        .orElseGet(() -> ResponseEntity.notFound().build());
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
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
                return ResponseEntity.ok()
                        .header("Content-Type", "image/jpeg")
                        .body(Files.readAllBytes(imagePath));
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
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
            @RequestParam(value = "vin", required = false) String vin,
            @RequestParam(value = "colorName", required = false) String colorName,
            @RequestParam(value = "modelName", required = false) String modelName,
            @RequestParam(value = "manufactureYear", required = false, defaultValue = "0") int manufactureYear,
            @RequestParam(value = "engineNumber", required = false) String engineNumber,
            @RequestParam(value = "ownerID", required = false, defaultValue = "0") int ownerID,
            @RequestParam(value = "currentDealerID", required = false, defaultValue = "0") int currentDealerID,
            @RequestParam(value = "versionID", required = false, defaultValue = "0") int versionID,
            @RequestParam(value = "status", required = false, defaultValue = "AVAILABLE") String status,
            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail,
            Model model
    ) {
        if (vin == null || vin.isBlank()) {
            model.addAttribute("error", "VIN is required");
            return "evmPage/createANewVehicleToList";
        }
        if (manufactureYear <= 0) {
            model.addAttribute("error", "Manufacture year is required");
            return "evmPage/createANewVehicleToList";
        }
        if (versionID <= 0) {
            model.addAttribute("error", "Version ID is required");
            return "evmPage/createANewVehicleToList";
        }

        try {
            // === Validate color ===
            Integer colorID = null;
            if (colorName != null && !colorName.isBlank()) {
                colorID = dao.getColorIdByName(colorName);
                if (colorID == null) {
                    model.addAttribute("error", "Color not found: " + colorName);
                    return "evmPage/createANewVehicleToList";
                }
            }

            // === Build vehicle ===
            DTOVehicle vehicle = new DTOVehicle();
            vehicle.setVIN(vin);
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
            if (ownerID > 0) {
                DTOCustomer owner = new DTOCustomer();
                owner.setCustomerID(ownerID);
                vehicle.setOwner(owner);
            }
            if (currentDealerID > 0) {
                DTODealer dealer = new DTODealer();
                dealer.setDealerID(currentDealerID);
                vehicle.setCurrentDealer(dealer);
            }

            // === Handle image upload ===
            if (thumbnail != null && !thumbnail.isEmpty()) {
                Path uploadPath = Paths.get(UPLOAD_DIR);
                if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
                String fileName = System.currentTimeMillis() + "_" + thumbnail.getOriginalFilename();
                Files.copy(thumbnail.getInputStream(), uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            }

            dao.insertVehicle(vehicle);
            model.addAttribute("message", "✅ Vehicle created successfully!");
            return "redirect:/evm/vehicle/list";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", "Invalid status: " + status);
            return "evmPage/createANewVehicleToList";
        } catch (IOException e) {
            model.addAttribute("error", "Error uploading thumbnail: " + e.getMessage());
            return "evmPage/createANewVehicleToList";
        } catch (Exception e) {
            model.addAttribute("error", "Unexpected error: " + e.getMessage());
            return "evmPage/createANewVehicleToList";
        }
    }

    // ===========================
    // 5️⃣ Form chỉnh sửa xe
    // ===========================
    @GetMapping("/edit/{vin}")
    public String showEditForm(@PathVariable String vin, Model model) {
        DTOVehicle vehicle = dao.getVehicleByVIN(vin);
        if (vehicle == null) {
            model.addAttribute("error", "Vehicle not found for VIN: " + vin);
            return "evmPage/editVehicle";
        }
        model.addAttribute("vehicle", vehicle);
        return "evmPage/editVehicle";
    }

    // ===========================
    // 6️⃣ Xử lý cập nhật xe
    // ===========================
    @PostMapping("/edit/{vin}")
    public String updateVehicle(
            @PathVariable String vin,
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
        try {
            DTOVehicle existing = dao.getVehicleByVIN(vin);
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

            DTOCustomer owner = new DTOCustomer();
            owner.setCustomerID(ownerID);
            existing.setOwner(owner);

            DTODealer dealer = new DTODealer();
            dealer.setDealerID(currentDealerID);
            existing.setCurrentDealer(dealer);

            if (thumbnail != null && !thumbnail.isEmpty()) {
                Path uploadPath = Paths.get(UPLOAD_DIR);
                if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
                String fileName = System.currentTimeMillis() + "_" + thumbnail.getOriginalFilename();
                Files.copy(thumbnail.getInputStream(), uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            }

            boolean updated = dao.updateVehicle(existing);
            if (!updated) {
                model.addAttribute("error", "Failed to update vehicle. Please try again.");
                return "evmPage/editVehicle";
            }
            return "redirect:/evm/vehicle/detail/" + vin;
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
    @PostMapping("/delete/{vin}")
    public String deleteVehicle(@PathVariable String vin, Model model) {
        if (vin == null || vin.isBlank()) {
            model.addAttribute("error", "Invalid vehicle identifier.");
            return "redirect:/evm/vehicle/list";
        }
        try {
            DTOVehicle existing = dao.getVehicleByVIN(vin);
            if (existing == null) {
                model.addAttribute("error", "Vehicle not found.");
                return "redirect:/evm/vehicle/list";
            }
            boolean deleted = dao.deleteVehicle(vin);
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
