package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DAODealerInventory;
import com.dealermanagementsysstem.project.Model.DAOAccount;
import com.dealermanagementsysstem.project.Model.DTODealerInventory;
import com.dealermanagementsysstem.project.util.SecurityUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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

    // Hiển thị danh sách xe theo DealerID của tài khoản đang đăng nhập
    @GetMapping
    public String showDealerInventory(Model model) {
        String email = SecurityUtil.getCurrentUserEmail();
        if (email == null) {
            model.addAttribute("error", "Bạn cần đăng nhập để xem inventory!");
            return "dealerPage/dealerInventory";
        }

        Integer dealerID = daoAccount.getDealerIdByEmail(email);
        if (dealerID == null) {
            model.addAttribute("error", "Không tìm thấy Dealer cho tài khoản hiện tại!");
            return "dealerPage/dealerInventory";
        }

        try {
            List<DTODealerInventory> vehicles = daoInventory.getVehiclesByDealerID(dealerID);
            model.addAttribute("vehicles", vehicles);
            model.addAttribute("dealerID", dealerID);
        } catch (Exception e) {
            model.addAttribute("error", "Đã xảy ra lỗi khi tải danh sách xe!");
        }
        return "dealerPage/dealerInventory";
    }
}
