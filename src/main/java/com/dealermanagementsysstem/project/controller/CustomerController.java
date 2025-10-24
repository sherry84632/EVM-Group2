package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DAOCustomer;
import com.dealermanagementsysstem.project.Model.DTOCustomer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class CustomerController {

    @Autowired
    private DAOCustomer daoCustomer;

    // ✅ Khi người dùng vào /customer → tự động chuyển hướng tới /customer/list
    @GetMapping({"/customer", "/customer/"})
    public String redirectCustomerToList() {
        return "redirect:/customer/list";
    }

    // ✅ Hiển thị danh sách khách hàng (Better List)
    @GetMapping("/customer/list")
    public String listCustomers(Model model) {
        List<DTOCustomer> customerList = daoCustomer.getAllCustomers();
        model.addAttribute("customers", customerList);
        return "dealerPage/betterCustomerListFinal";
                // ✅ Giao diện chính
    }

    // ✅ Form tạo mới Customer
    @GetMapping("/customer/create")
    public String showCreateForm(Model model) {
        model.addAttribute("customer", new DTOCustomer());
        return "dealerPage/customerCreate";
    }

    // ✅ Lưu customer mới
    @PostMapping("/customer/save")
    public String saveCustomer(@ModelAttribute("customer") DTOCustomer c,
                               RedirectAttributes redirectAttributes) {
        boolean success = daoCustomer.insertCustomer(c);

        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "✅ Customer added successfully!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Failed to add customer!");
        }

        return "redirect:/customer/list"; // ✅ Quay lại danh sách
    }

    // ✅ Mở trang chỉnh sửa Customer
    @GetMapping("/customer/edit/{id}")
    public String editCustomer(@PathVariable("id") int id, Model model, RedirectAttributes redirectAttributes) {
        DTOCustomer customer = daoCustomer.getCustomerById(id);
        if (customer == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Customer not found!");
            return "redirect:/customer/list";
        }
        model.addAttribute("customer", customer);
        return "dealerPage/customerEdit";
    }

    // ✅ Cập nhật Customer
    @PostMapping("/customer/update")
    public String updateCustomer(@ModelAttribute("customer") DTOCustomer c,
                                 RedirectAttributes redirectAttributes) {
        boolean success = daoCustomer.updateCustomer(c);

        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "✅ Customer updated successfully!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Failed to update customer!");
        }

        return "redirect:/customer/list"; // ✅ Trở về danh sách
    }

    // ✅ Xóa Customer (POST chuẩn RESTful)
    @PostMapping("/customer/delete/{id}")
    public String deleteCustomer(@PathVariable("id") int id,
                                 RedirectAttributes redirectAttributes) {
        boolean success = daoCustomer.deleteCustomer(id);

        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "🗑️ Customer deleted successfully!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Failed to delete customer!");
        }

        return "redirect:/customer/list"; // ✅ Quay về danh sách
    }


    // ✅ Tìm kiếm Customer
    @GetMapping("/customer/search")
    public String searchCustomer(@RequestParam("keyword") String keyword, Model model) {
        List<DTOCustomer> customerList = daoCustomer.searchCustomer(keyword);
        model.addAttribute("customers", customerList);
        model.addAttribute("keyword", keyword);
        return "dealerPage/betterCustomerListFinal";
    }
    // ✅ Hiển thị chi tiết khách hàng
    @GetMapping("/customer/detail/{id}")
    public String showCustomerDetail(@PathVariable("id") int id, Model model, RedirectAttributes redirectAttributes) {
        DTOCustomer customer = daoCustomer.getCustomerById(id);
        if (customer == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Customer not found!");
            return "redirect:/customer/list";
        }

        model.addAttribute("customer", customer);
        return "dealerPage/customerDetail"; // ✅ Đây là file HTML bạn vừa gửi
    }

}
