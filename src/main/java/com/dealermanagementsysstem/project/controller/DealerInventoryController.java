package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DAODealerInventory;
import com.dealermanagementsysstem.project.Model.DAOAccount;
import com.dealermanagementsysstem.project.Model.DTODealerInventory;
import com.dealermanagementsysstem.project.util.SecurityUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.List;

@Controller
@RequestMapping("/dealer-inventory")
public class DealerInventoryController {

    private final DAODealerInventory daoInventory;
    private final DAOAccount daoAccount;

    public DealerInventoryController(DAODealerInventory daoInventory, DAOAccount daoAccount) {
        this.daoInventory = daoInventory;
        this.daoAccount = daoAccount;
    }

    @GetMapping("/view")
    public String viewDetail(@RequestParam("vin") String vin, Model model) {
        var opt = daoInventory.getDetailByVin(vin);
        if (opt.isEmpty()) {
            model.addAttribute("error", "Vehicle not found for VIN: " + vin);
            return "dealerPage/dealerInventory";
        }
        model.addAttribute("detail", opt.get());
        return "dealerPage/dealerInventoryDetail";
    }

    // Hiển thị danh sách xe theo DealerID của tài khoản đang đăng nhập
    @GetMapping
    public String showDealerInventory(Model model,
                                      @RequestParam(value = "keyword", required = false) String keyword,
                                      @RequestParam(value = "vin", required = false) String vin,
                                      @RequestParam(value = "modelId", required = false) Integer modelId,
                                      @RequestParam(value = "versionId", required = false) Integer versionId,
                                      @RequestParam(value = "colorId", required = false) Integer colorId,
                                      @RequestParam(value = "status", required = false) String status,
                                      @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate from,
                                      @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate to) {
        String email = SecurityUtil.getCurrentUserEmail();
        
        System.out.println(" Dealer Inventory Access - Email: " + email);
        
        if (email == null) {
            model.addAttribute("error", "Bạn cần đăng nhập để xem inventory!");
            return "dealerPage/dealerInventory";
        }

        Integer dealerID = daoAccount.getDealerIdByEmail(email);
        
        System.out.println(" Resolved DealerID: " + dealerID + " for email: " + email);
        
        if (dealerID == null) {
            System.out.println(" ERROR: Could not find DealerID for email: " + email);
            model.addAttribute("error", "Không tìm thấy Dealer cho tài khoản hiện tại!");
            return "dealerPage/dealerInventory";
        }

        try {
            List<DTODealerInventory> vehicles;
            java.sql.Date receivedFrom = (from != null ? java.sql.Date.valueOf(from) : null);
            java.sql.Date receivedTo = (to != null ? java.sql.Date.valueOf(to) : null);

            if ((vin != null && !vin.isBlank()) || (modelId != null && modelId > 0) || (versionId != null && versionId > 0)
                    || (colorId != null && colorId > 0) || (status != null && !status.isBlank())
                    || (receivedFrom != null && receivedTo != null)) {
                vehicles = daoInventory.getVehiclesByDealerIDWithFilters(dealerID, vin, modelId, versionId, colorId, status, receivedFrom, receivedTo);
                System.out.println(" Filtered inventory for DealerID " + dealerID + ": " + vehicles.size() + " vehicles");
            } else if (keyword != null && !keyword.trim().isEmpty()) {
                vehicles = daoInventory.getVehiclesByDealerIDWithKeyword(dealerID, keyword);
                System.out.println(" Keyword search for DealerID " + dealerID + ": " + vehicles.size() + " vehicles");
            } else {
                vehicles = daoInventory.getVehiclesByDealerID(dealerID);
                System.out.println(" All inventory for DealerID " + dealerID + ": " + vehicles.size() + " vehicles");
            }
            
            model.addAttribute("vehicles", vehicles);
            model.addAttribute("dealerID", dealerID);
            model.addAttribute("keyword", keyword);
            model.addAttribute("vin", vin);
            model.addAttribute("modelId", modelId);
            model.addAttribute("versionId", versionId);
            model.addAttribute("colorId", colorId);
            model.addAttribute("status", status);
            model.addAttribute("from", from);
            model.addAttribute("to", to);
            // dropdown data
            model.addAttribute("models", daoInventory.getAllModels());
            if (modelId != null && modelId > 0) model.addAttribute("versions", daoInventory.getVersionsByModel(modelId));
            if ((modelId != null && modelId > 0) || (versionId != null && versionId > 0)) {
                model.addAttribute("colors", daoInventory.getColorsByModelVersion(modelId, versionId));
            }
            // summary
            com.dealermanagementsysstem.project.Model.DTOInventorySummary summary = daoInventory.getInventorySummary(dealerID, vin, modelId, versionId, colorId, status, receivedFrom, receivedTo);
            model.addAttribute("summary", summary);
        } catch (Exception e) {
            model.addAttribute("error", "Đã xảy ra lỗi khi tải danh sách xe!");
        }
        return "dealerPage/dealerInventory";
    }

    @GetMapping("/models")
    @ResponseBody
    public List<com.dealermanagementsysstem.project.Model.DTOVehicleModel> getModels() {
        return daoInventory.getAllModels();
    }

    @GetMapping("/versions")
    @ResponseBody
    public List<com.dealermanagementsysstem.project.Model.DTOVehicleVersion> getVersions(@RequestParam("modelId") int modelId) {
        return daoInventory.getVersionsByModel(modelId);
    }

    @GetMapping("/colors")
    @ResponseBody
    public List<com.dealermanagementsysstem.project.Model.DTOVehicleColor> getColors(@RequestParam(value = "modelId", required = false) Integer modelId,
                                                                                     @RequestParam(value = "versionId", required = false) Integer versionId) {
        return daoInventory.getColorsByModelVersion(modelId, versionId);
    }
}
