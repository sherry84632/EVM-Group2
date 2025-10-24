package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DAODealerInventory;
import com.dealermanagementsysstem.project.Model.DAOAccount;
import com.dealermanagementsysstem.project.Model.DTODealerInventory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    public DealerInventoryController() {
        this.daoInventory = new DAODealerInventory();
        this.daoAccount = new DAOAccount();
    }

    // ✅ Hiển thị danh sách xe theo DealerID của tài khoản đang đăng nhập
    @GetMapping
    public String showDealerInventory(Model model) {
        System.out.println("🏪 [INVENTORY] Loading dealer inventory page...");
        try {
            // 🔹 Lấy email của người đang đăng nhập
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            System.out.println("   📧 Email: " + email);

            // 🔹 Lấy DealerID dựa theo email
            Integer dealerID = daoAccount.getDealerIdByEmail(email);
            System.out.println("   🏢 DealerID: " + dealerID);

            if (dealerID == null) {
                System.out.println("   ❌ DealerID not found for email: " + email);
                model.addAttribute("error", "Không tìm thấy Dealer ID cho tài khoản hiện tại!");
                return "dealerPage/dealerInventory";
            }

            // 🔹 Lấy danh sách xe
            List<DTODealerInventory> vehicles = daoInventory.getVehiclesByDealerID(dealerID);
            System.out.println("   🚗 Found " + vehicles.size() + " vehicles in inventory");

            if (vehicles.size() > 0) {
                System.out.println("   📋 Listing vehicles:");
                for (int i = 0; i < Math.min(5, vehicles.size()); i++) {
                    DTODealerInventory v = vehicles.get(i);
                    System.out.println("      " + (i+1) + ". VIN: " + v.getVin() +
                                     ", Status: " + v.getStatus() +
                                     ", ReceivedDate: " + v.getReceivedDate());
                }
                if (vehicles.size() > 5) {
                    System.out.println("      ... and " + (vehicles.size() - 5) + " more");
                }
            } else {
                System.out.println("   ⚠️  Inventory is EMPTY for DealerID: " + dealerID);
            }

            model.addAttribute("vehicles", vehicles);
            model.addAttribute("dealerID", dealerID);

        } catch (Exception e) {
            System.out.println("   ❌ ERROR loading inventory:");
            e.printStackTrace();
            model.addAttribute("error", "Đã xảy ra lỗi khi tải danh sách xe!");
        }

        return "dealerPage/dealerInventory";
    }
}
