package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DAODealer;
import com.dealermanagementsysstem.project.Model.DTODealer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.sql.SQLException;
import java.util.List;

@Controller
@RequestMapping("/dealer")
public class DealerController {

    @Autowired
    private DAODealer daoDealer;

    @GetMapping("/management")
    public String listDealers(Model model, @ModelAttribute("message") String message) throws SQLException {
        List<DTODealer> dealers = daoDealer.getAllDealers();
        model.addAttribute("dealers", dealers);

        // Nếu có message thì hiển thị
        if (message != null && !message.isEmpty()) {
            model.addAttribute("message", message);
        }

        return "evmPage/dealer-management";
    }

    @PostMapping("/create")
    public String createDealer(@ModelAttribute DTODealer d, RedirectAttributes redirectAttributes) throws SQLException {
        d.setDealerID(0); // 🔹 thêm dòng này để tránh lỗi binding int rỗng
        daoDealer.insertDealer(d);
        redirectAttributes.addFlashAttribute("message", "✅ Dealer created successfully!");
        return "redirect:/dealer/management";
    }

    // 🟠 UPDATE
    @PostMapping("/{id}/update")
    public String updateDealer(@PathVariable int id, @ModelAttribute DTODealer d, RedirectAttributes redirectAttributes) throws SQLException {
        d.setDealerID(id);
        daoDealer.updateDealer(d);
        redirectAttributes.addFlashAttribute("message", "🟡 Dealer updated successfully!");
        return "redirect:/dealer/management";
    }

    // 🔴 DELETE - with error handling for foreign key constraints
    @PostMapping("/{id}/delete")
    public String deleteDealer(@PathVariable int id, RedirectAttributes redirectAttributes) {
        try {
            // Check if dealer has any customers
            // Note: This check happens at database level via foreign key
            daoDealer.deleteDealer(id);
            redirectAttributes.addFlashAttribute("message", "✅ Dealer deleted successfully!");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            // Handle foreign key constraint violation
            if (e.getMessage() != null && (
                e.getMessage().contains("REFERENCE constraint") ||
                e.getMessage().contains("foreign key") ||
                e.getMessage().contains("FKjxm2geivgydugseqtjf842mg"))) {

                redirectAttributes.addFlashAttribute("message",
                    "❌ Cannot delete dealer! This dealer still has customers or orders associated. " +
                    "Please reassign or delete related data first.");
                redirectAttributes.addFlashAttribute("messageType", "error");

                System.out.println("⚠️ Cannot delete dealer ID " + id + ": Has related customers/orders");
            } else {
                // Other database errors
                redirectAttributes.addFlashAttribute("message",
                    "❌ Error deleting dealer: " + e.getMessage());
                redirectAttributes.addFlashAttribute("messageType", "error");

                System.out.println("❌ Error deleting dealer ID " + id + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        return "redirect:/dealer/management";
    }
}
