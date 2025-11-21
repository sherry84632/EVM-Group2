package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/discount")
public class DealerPriceAdjustmentController {

    private final DAODealerPriceAdjustment daoDiscount;
    private final DAOAccount daoAccount;
    private final DAOVehicleModel daoVehicleModel;

    public DealerPriceAdjustmentController() {
        this.daoDiscount = new DAODealerPriceAdjustment();
        this.daoAccount = new DAOAccount();
        this.daoVehicleModel = new DAOVehicleModel();
    }

    //  Trang quản lý Discount (list + form + search)
    @GetMapping
    public String showDiscountManagementPage(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "vehicleSearch", required = false) String vehicleSearch,
            Model model
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName(); //  Lấy email đang đăng nhập
        Integer dealerID = daoAccount.getDealerIdByEmail(email);

        if (dealerID == null) {
            model.addAttribute("error", "Không tìm thấy Dealer của email: " + email);
            return "dealerPage/createADealerDiscount";
        }

        // Lấy danh sách discount
        List<DTODealerPriceAdjustment> discounts;
        if (keyword != null && !keyword.trim().isEmpty()) {
            discounts = daoDiscount.searchByPromotionNameAndDealer(keyword, dealerID);
            model.addAttribute("keyword", keyword);
        } else {
            discounts = daoDiscount.getDiscountsByDealer(dealerID);
        }

        // Lấy danh sách mẫu xe để chọn
        List<DTOVehicleModel> vehicleModels = daoVehicleModel.getAllModels();
        if (vehicleSearch != null && !vehicleSearch.trim().isEmpty()) {
            vehicleModels = vehicleModels.stream()
                .filter(v -> v.getModelName().toLowerCase().contains(vehicleSearch.toLowerCase()))
                .toList();
            model.addAttribute("vehicleSearch", vehicleSearch);
        }

        model.addAttribute("vehicleModels", vehicleModels);
        model.addAttribute("discounts", discounts);
        model.addAttribute("discount", new DTODealerPriceAdjustment());
        return "dealerPage/createADealerDiscount";
    }

    //  Tạo discount mới (POST)
    @PostMapping("/insert")
    public String insertDiscount(
            @RequestParam("promotionName") String promotionName,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam("discountPercent") Double discountPercent,
            @RequestParam("applyScope") String applyScope,
            @RequestParam(value = "selectedModels", required = false) List<Integer> selectedModels,
            @RequestParam(value = "notes", required = false) String notes,
            Model model
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Integer dealerID = daoAccount.getDealerIdByEmail(email);

        if (dealerID == null) {
            model.addAttribute("error", "Không tìm thấy Dealer của email: " + email);
            return "dealerPage/createADealerDiscount";
        }

        if (discountPercent == null || discountPercent < 0) {
            model.addAttribute("error", "Discount percent không hợp lệ");
        }
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            model.addAttribute("error", "End Date phải sau hoặc bằng Start Date");
        }

        // Build single DTO
        DTODealerPriceAdjustment dto = new DTODealerPriceAdjustment();
        DTODealer dealer = new DTODealer();
        dealer.setDealerID(dealerID);
        dto.setDealer(dealer);
        dto.setPromotionName(promotionName);
        dto.setStartDate(startDate);
        dto.setEndDate(endDate);
        dto.setDiscountPercent(discountPercent);
        dto.setDiscountAmount(0.0);
        dto.setNotes(notes);

        if ("ALL".equalsIgnoreCase(applyScope)) {
            // Represent ALL by leaving vehicleModel null and applicableModelIDs null
            dto.setVehicleModel(null);
            dto.setApplicableModelIDs(null);
        } else {
            if (selectedModels == null || selectedModels.isEmpty()) {
                model.addAttribute("error", "Vui lòng chọn ít nhất một mẫu xe");
            } else {
                // Store all IDs as comma-separated string
                String ids = selectedModels.stream().map(String::valueOf).reduce((a,b)->a+","+b).orElse("");
                dto.setApplicableModelIDs(ids);
                // For UI convenience set first model as vehicleModel (primary) if needed
                DTOVehicleModel primary = new DTOVehicleModel();
                primary.setModelID(selectedModels.get(0));
                dto.setVehicleModel(primary);
            }
        }

        boolean success = false;
        if (model.getAttribute("error") == null) {
            // Persist single row: need DAO adaptation -> createDiscountMulti aware of applicableModelIDs
            success = daoDiscount.createDiscount(dto); // Reuse existing insert (ModelID must exist even if ALL)
        }

        if (success) {
            model.addAttribute("message", "Tạo discount thành công!");
        } else if (model.getAttribute("error") == null) {
            model.addAttribute("error", "Không thể tạo discount. Kiểm tra dữ liệu!");
        }

        List<DTODealerPriceAdjustment> discounts = daoDiscount.getDiscountsByDealer(dealerID);
        model.addAttribute("discounts", discounts);
        model.addAttribute("vehicleModels", daoVehicleModel.getAllModels());
        return "dealerPage/createADealerDiscount";
    }

    //  API endpoint to get vehicle details
    @GetMapping("/vehicle-detail/{modelId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getVehicleDetail(@PathVariable int modelId) {
        DTOVehicleModel vehicle = daoVehicleModel.getModelById(modelId);

        if (vehicle == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("modelId", vehicle.getModelID());
        response.put("modelName", vehicle.getModelName());
        response.put("brand", vehicle.getBrand());
        response.put("year", vehicle.getYear());
        response.put("bodyType", vehicle.getBodyType());
        response.put("basePrice", vehicle.getBasePrice());
        response.put("description", vehicle.getDescription());
        response.put("hasImage", vehicle.getModelImage() != null);

        return ResponseEntity.ok(response);
    }

    //  API endpoint to get vehicle image
    @GetMapping("/vehicle-image/{modelId}")
    public ResponseEntity<byte[]> getVehicleImage(@PathVariable int modelId) {
        DTOVehicleModel vehicle = daoVehicleModel.getModelById(modelId);

        if (vehicle == null || vehicle.getModelImage() == null) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG); // or IMAGE_PNG depending on your stored format
        headers.setContentLength(vehicle.getModelImage().length);

        return new ResponseEntity<>(vehicle.getModelImage(), headers, HttpStatus.OK);
    }

    //  API endpoint to get discount detail
    @GetMapping("/api/detail/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDiscountDetail(@PathVariable int id) {
        DTODealerPriceAdjustment dto = daoDiscount.getDiscountById(id);
        if (dto == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Discount not found"));
        }
        Map<String, Object> data = new HashMap<>();
        data.put("adjustmentID", dto.getAdjustmentID());
        data.put("promotionName", dto.getPromotionName());
        data.put("discountPercent", dto.getDiscountPercent());
        data.put("startDate", dto.getStartDate());
        data.put("endDate", dto.getEndDate());
        data.put("notes", dto.getNotes());
        if (dto.getVehicleModel() != null) {
            data.put("modelID", dto.getVehicleModel().getModelID());
            data.put("modelName", dto.getVehicleModel().getModelName());
        }
        return ResponseEntity.ok(data);
    }

    @GetMapping("/edit/{id}")
    public String editDiscount(@PathVariable int id, Model model,
                               @RequestParam(value = "vehicleSearch", required = false) String vehicleSearch) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Integer dealerID = daoAccount.getDealerIdByEmail(email);
        if (dealerID == null) {
            model.addAttribute("error", "Không tìm thấy Dealer của email: " + email);
            return "dealerPage/createADealerDiscount";
        }
        DTODealerPriceAdjustment discount = daoDiscount.getDiscountById(id);
        if (discount == null) {
            model.addAttribute("error", "Discount không tồn tại");
            return "redirect:/discount";
        }
        // Load vehicle models
        List<DTOVehicleModel> vehicleModels = daoVehicleModel.getAllModels();
        if (vehicleSearch != null && !vehicleSearch.isBlank()) {
            vehicleModels = vehicleModels.stream()
                    .filter(v -> v.getModelName().toLowerCase().contains(vehicleSearch.toLowerCase()))
                    .toList();
            model.addAttribute("vehicleSearch", vehicleSearch);
        }
        model.addAttribute("vehicleModels", vehicleModels);
        model.addAttribute("discounts", daoDiscount.getDiscountsByDealer(dealerID));
        model.addAttribute("editDiscount", discount);
        // Determine scope & selected models
        if (discount.getApplicableModelIDs() == null) {
            if (discount.getVehicleModel() == null) {
                model.addAttribute("applyScope", "ALL");
            } else {
                model.addAttribute("applyScope", "SPECIFIC_SINGLE");
                model.addAttribute("selectedModelIds", List.of(discount.getVehicleModel().getModelID()));
            }
        } else {
            model.addAttribute("applyScope", "SPECIFIC");
            List<Integer> ids = java.util.Arrays.stream(discount.getApplicableModelIDs().split(","))
                    .filter(s -> !s.isBlank())
                    .map(Integer::valueOf)
                    .toList();
            model.addAttribute("selectedModelIds", ids);
        }
        return "dealerPage/createADealerDiscount";
    }

    @PostMapping("/update/{id}")
    public String updateDiscount(@PathVariable int id,
                                 @RequestParam("promotionName") String promotionName,
                                 @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                 @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                 @RequestParam("discountPercent") Double discountPercent,
                                 @RequestParam("applyScope") String applyScope,
                                 @RequestParam(value = "selectedModels", required = false) List<Integer> selectedModels,
                                 @RequestParam(value = "notes", required = false) String notes,
                                 Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Integer dealerID = daoAccount.getDealerIdByEmail(email);
        if (dealerID == null) {
            model.addAttribute("error", "Không tìm thấy Dealer");
            return "redirect:/discount";
        }
        DTODealerPriceAdjustment existing = daoDiscount.getDiscountById(id);
        if (existing == null) {
            model.addAttribute("error", "Discount không tồn tại");
            return "redirect:/discount";
        }
        existing.setPromotionName(promotionName);
        existing.setStartDate(startDate);
        existing.setEndDate(endDate);
        existing.setDiscountPercent(discountPercent);
        existing.setNotes(notes);
        existing.setDiscountAmount(0.0);
        if ("ALL".equalsIgnoreCase(applyScope)) {
            existing.setVehicleModel(null);
            existing.setApplicableModelIDs(null);
        } else {
            if (selectedModels == null || selectedModels.isEmpty()) {
                model.addAttribute("error", "Vui lòng chọn ít nhất một mẫu xe");
                return "redirect:/discount/edit/" + id;
            }
            if (selectedModels.size() == 1) {
                DTOVehicleModel vm = new DTOVehicleModel();
                vm.setModelID(selectedModels.get(0));
                existing.setVehicleModel(vm);
                existing.setApplicableModelIDs(null);
            } else {
                String ids = selectedModels.stream().map(String::valueOf).reduce((a,b)->a+","+b).orElse("");
                existing.setApplicableModelIDs(ids);
                DTOVehicleModel primary = new DTOVehicleModel();
                primary.setModelID(selectedModels.get(0));
                existing.setVehicleModel(primary);
            }
        }
        boolean success = daoDiscount.updateDiscount(existing);
        if (success) model.addAttribute("message", "Cập nhật discount thành công!");
        else model.addAttribute("error", "Cập nhật thất bại!");
        return "redirect:/discount";
    }

    @PostMapping("/delete/{id}")
    public String deleteDiscount(@PathVariable int id, Model model) {
        boolean success = daoDiscount.deleteDiscount(id);
        if (success) model.addAttribute("message", "Xóa discount thành công!");
        else model.addAttribute("error", "Xóa discount thất bại!");
        return "redirect:/discount";
    }

    //  Show discount detail page
    @GetMapping("/detail/{id}")
    @Deprecated // Detail view removed: badges open modals directly
    public String showDiscountDetail(@PathVariable int id) {
        // Always redirect to main discount management page; detail page deprecated.
        return "redirect:/discount";
    }
}
