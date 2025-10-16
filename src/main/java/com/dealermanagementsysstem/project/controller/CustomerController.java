package com.dealermanagementsysstem.project.controller;
import java.sql.Date;
import com.dealermanagementsysstem.project.Model.DAOCustomer;
import com.dealermanagementsysstem.project.Model.DTOCustomer;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.List;

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
        return "customer-list";
    }

    // ✅ Hiển thị form thêm mới
    @GetMapping("/new")
    public String showNewForm(Model model) {
        model.addAttribute("customer", new DTOCustomer());
        return "customer-form";
    }

    // ✅ Tìm kiếm khách hàng theo tên hoặc email
    @GetMapping("/search")
    public String searchCustomers(@RequestParam("keyword") String keyword, Model model) {
        List<DTOCustomer> list = dao.searchCustomer(keyword);
        model.addAttribute("customers", list);
        model.addAttribute("keyword", keyword);
        return "customer-list";
    }
    
<<<<<<< Updated upstream
    // ✅ Thêm mới khách hàng
=======
    // ✅ Hiển thị form thêm mới
    @GetMapping("/new")
    public String showNewForm(Model model) {
        model.addAttribute("customer", new DTOCustomer());
        return "customer-form";
    }



>>>>>>> Stashed changes
    @PostMapping("/insert")
    public String insertCustomer(
            @RequestParam("FullName") String fullName,
            @RequestParam("Phone") String phone,
            @RequestParam("Email") String email,
            @RequestParam("Address") String address,
            @RequestParam(value = "CreatedAt", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") Timestamp createdAt,
            @RequestParam(value = "BirthDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date birthDate,
            @RequestParam(value = "Note", required = false) String note,
            @RequestParam(value = "TestDriveSchedule", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") Timestamp testDriveSchedule,
            @RequestParam(value = "VehicleInterest", required = false) String vehicleInterest
    ) {
        DTOCustomer c = new DTOCustomer();
        c.setFullName(fullName);
        c.setPhone(phone);
        c.setEmail(email);
        c.setAddress(address);
        c.setCreatedAt(createdAt);
        c.setBirthDate(birthDate);
        c.setNote(note);
        c.setTestDriveSchedule(testDriveSchedule);
        c.setVehicleInterest(vehicleInterest);

        if (dao.insertCustomer(c)) {
            return "redirect:/customer";
        } else {
            return "redirect:/customer/new?error=invalid";
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
        return "customer-form";
    }

    // ✅ Cập nhật khách hàng
    @PostMapping("/update")
    public String updateCustomer(@ModelAttribute("customer") DTOCustomer c) {
        if (dao.updateCustomer(c)) {
            return "redirect:/customer";
        } else {
            return "redirect:/customer/edit/" + c.getCustomerID() + "?error=invalid";
        }
    }

    // ✅ Xóa khách hàng
    @GetMapping("/delete/{id}")
    public String deleteCustomer(@PathVariable("id") int id) {
        dao.deleteCustomer(id);
        return "redirect:/customer";
    }
}
