package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

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

    // ✅ Trang quản lý Discount (list + form + search)
    @GetMapping
    public String showDiscountManagementPage(
            @RequestParam(value = "keyword", required = false) String keyword,
            Model model
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName(); // ✅ Lấy email đang đăng nhập
        Integer dealerID = daoAccount.getDealerIdByEmail(email);

        if (dealerID == null) {
            model.addAttribute("error", "Không tìm thấy Dealer của email: " + email);
            return "dealerPage/createADealerDiscount";
        }

        List<DTODealerPriceAdjustment> discounts;
        if (keyword != null && !keyword.trim().isEmpty()) {
            discounts = daoDiscount.searchByPromotionNameAndDealer(keyword, dealerID);
            model.addAttribute("keyword", keyword);
        } else {
            discounts = daoDiscount.getDiscountsByDealer(dealerID);
        }

        // Add vehicle models to the model for dropdown
        List<DTOVehicleModel> vehicleModels = daoVehicleModel.getAllModels();
        model.addAttribute("vehicleModels", vehicleModels);

        model.addAttribute("discounts", discounts);
        model.addAttribute("discount", new DTODealerPriceAdjustment());
        return "dealerPage/createADealerDiscount";
    }

    // ✅ Tạo discount mới (POST)
    @PostMapping("/insert")
    public String insertDiscount(
            @RequestParam("promotionName") String promotionName,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam("discountPercent") Double discountPercent,
            @RequestParam("modelID") int modelID,
            @RequestParam(value = "notes", required = false) String notes,
            Model model
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName(); // ✅ Lấy email đang đăng nhập
        Integer dealerID = daoAccount.getDealerIdByEmail(email);

        if (dealerID == null) {
            model.addAttribute("error", "Không tìm thấy Dealer của email: " + email);
            return "dealerPage/createADealerDiscount";
        }

        DTODealerPriceAdjustment d = new DTODealerPriceAdjustment();
        d.setPromotionName(promotionName);
        d.setStartDate(startDate);
        d.setEndDate(endDate);
        d.setDiscountPercent(discountPercent);

        // Set VehicleModel object
        DTOVehicleModel vehicleModel = new DTOVehicleModel();
        vehicleModel.setModelID(modelID);
        d.setVehicleModel(vehicleModel);

        // Set Dealer object
        DTODealer dealer = new DTODealer();
        dealer.setDealerID(dealerID);
        d.setDealer(dealer);

        d.setNotes(notes);
        d.setDiscountAmount(0.0);

        boolean success = daoDiscount.createDiscount(d);

        if (success) {
            model.addAttribute("message", "Tạo discount thành công!");
        } else {
            model.addAttribute("error", "Không thể tạo discount. Vui lòng kiểm tra dữ liệu!");
        }

        // ✅ Load lại danh sách discount của dealer đó
        List<DTODealerPriceAdjustment> discounts = daoDiscount.getDiscountsByDealer(dealerID);
        model.addAttribute("discounts", discounts);
        model.addAttribute("discount", new DTODealerPriceAdjustment());
        return "dealerPage/createADealerDiscount";
    }
}