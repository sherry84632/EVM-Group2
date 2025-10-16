package com.dealermanagementsysstem.project.controller;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.dealermanagementsysstem.project.Model.DAOCustomer;
import com.dealermanagementsysstem.project.Model.DTOCustomer;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/customer")
public class CustomerController {

    private final DAOCustomer dao;

    public CustomerController() {
        this.dao = new DAOCustomer();
    }

    // ✅ Danh sách tất cả khách hàng
    @GetMapping
    public String listCustomers(Model model) {
        List<DTOCustomer> list = dao.getAllCustomers();
        model.addAttribute("customers", list);
        return "dealerPage/customerList";
    }

    // ✅ Hiển thị form thêm mới khách hàng
    @GetMapping("/new")
    public String showNewForm(Model model) {
        model.addAttribute("customer", new DTOCustomer());
        return "dealerPage/createANewCustomer";
    }

    // ✅ Tìm kiếm khách hàng theo tên hoặc email
    @GetMapping("/search")
    public String searchCustomers(@RequestParam("keyword") String keyword, Model model) {
        List<DTOCustomer> list = dao.searchCustomer(keyword);
        model.addAttribute("customers", list);
        model.addAttribute("keyword", keyword);
        return "dealerPage/customerManagement";
    }

    // ✅ Thêm mới khách hàng (dùng RequestParam)
    @PostMapping("/insert")
    public String insertCustomer(
            @RequestParam("FullName") String fullName,
            @RequestParam("Phone") String phone,
            @RequestParam("Email") String email,
            @RequestParam("Address") String address,
            @RequestParam(value = "CreatedAt", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAt,
            @RequestParam(value = "BirthDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate birthDate,
            @RequestParam(value = "Note", required = false) String note,
            @RequestParam(value = "TestDriveSchedule", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime testDriveSchedule,
            @RequestParam(value = "VehicleInterest", required = false) String vehicleInterest,
            Model model
    ) {
        System.out.println("🧩 [DEBUG] Request received to insert new customer: " + fullName);

        DTOCustomer c = new DTOCustomer();
        c.setFullName(fullName);
        c.setPhone(phone);
        c.setEmail(email);
        c.setAddress(address);

        // ✅ Convert LocalDateTime → Timestamp
        if (createdAt != null)
            c.setCreatedAt(Timestamp.valueOf(createdAt));
        if (birthDate != null)
            c.setBirthDate(java.sql.Date.valueOf(birthDate));
        if (testDriveSchedule != null)
            c.setTestDriveSchedule(Timestamp.valueOf(testDriveSchedule));

        c.setNote(note);
        c.setVehicleInterest(vehicleInterest);

        boolean success = dao.insertCustomer(c);

        if (success) {
            System.out.println("✅ [SUCCESS] Customer created successfully: " + fullName);
            model.addAttribute("message", "Customer created successfully!");
            return "redirect:/customer";
        } else {
            System.out.println("❌ [FAILED] Failed to create customer: " + fullName);
            model.addAttribute("error", "Failed to create customer. Please check input data!");
            return "dealerPage/createANewCustomer";
        }
    }

    // ✅ Hiển thị form chỉnh sửa
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") int id, Model model) {
        DTOCustomer existing = dao.getAllCustomers().stream()
                .filter(c -> c.getCustomerID() == id)
                .findFirst()
                .orElse(null);

        model.addAttribute("customer", existing);
        return "dealerPage/customerDetail";
    }

    // ✅ Cập nhật khách hàng
    @PostMapping("/update")
    public String updateCustomer(@ModelAttribute("customer") DTOCustomer c) {
        boolean success = dao.updateCustomer(c);

        if (success) {
            System.out.println("✅ [SUCCESS] Updated customer: " + c.getFullName());
            return "redirect:/customer";
        } else {
            System.out.println("❌ [FAILED] Failed to update customer: " + c.getFullName());
            return "redirect:/customer/edit/" + c.getCustomerID() + "?error=invalid";
        }
    }

    // ✅ Xóa khách hàng
    @GetMapping("/delete/{id}")
    public String deleteCustomer(@PathVariable("id") int id) {
        boolean success = dao.deleteCustomer(id);
        if (success) {
            System.out.println("🗑️ [DELETED] Customer ID " + id + " deleted successfully.");
        } else {
            System.out.println("⚠️ [FAILED] Failed to delete customer ID: " + id);
        }
        return "redirect:/customer";
    }
}
