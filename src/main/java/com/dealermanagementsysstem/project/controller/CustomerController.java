package com.dealermanagementsysstem.project.controller;


import com.dealermanagementsysstem.project.Model.DTOCustomer;
import com.dealermanagementsysstem.project.Model.DTOTestDrive;
import com.dealermanagementsysstem.project.dto.CustomerForm;
import com.dealermanagementsysstem.project.mapper.CustomerMapper;
import com.dealermanagementsysstem.project.service.CustomerService;
import com.dealermanagementsysstem.project.service.TestDriveService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class CustomerController {

    private static final Logger log = LoggerFactory.getLogger(CustomerController.class);


    private final CustomerService customerService;
    private final TestDriveService testDriveService;
    private final CustomerMapper customerMapper;

    public CustomerController(CustomerService customerService, TestDriveService testDriveService, CustomerMapper customerMapper) {
        this.customerService = customerService;
        this.testDriveService = testDriveService;
        this.customerMapper = customerMapper;
    }

    @GetMapping({"/customer", "/customer/"})
    public String redirectCustomerToList() {
        return "redirect:/customer/list";
    }

    @GetMapping("/customer/list")
    public String listCustomers(Model model) {
        model.addAttribute("customers", customerService.getCustomers());
        return "dealerPage/betterCustomerListFinal";
    }

    @GetMapping("/customer/create")
    public String showCreateForm(Model model) {
        if (!model.containsAttribute("customerForm")) {
            model.addAttribute("customerForm", new CustomerForm());
        }
        return "dealerPage/createANewCustomer";
    }

    @PostMapping("/customer/save")
    public String saveCustomer(@Valid @ModelAttribute("customerForm") CustomerForm customerForm,
                               BindingResult bindingResult,
                               @RequestParam(value = "testDriveSchedule", required = false) String testDriveSchedule,
                               RedirectAttributes redirectAttributes,
                               Model model) {

        if (bindingResult.hasErrors()) {
            return "dealerPage/createANewCustomer";
        }

        try {
            customerService.createCustomerWithTestDrive(customerForm, testDriveSchedule);
            redirectAttributes.addFlashAttribute("successMessage", "✅ Customer saved successfully");
            return "redirect:/customer/list";
        } catch (Exception e) {
            log.error("Error creating customer: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "❌ Failed to save customer: " + e.getMessage());
            return "dealerPage/createANewCustomer";
        }
    }

    @GetMapping("/customer/edit/{id}")
    public String editCustomer(@PathVariable("id") int id, Model model, RedirectAttributes redirectAttributes) {
        try {
            DTOCustomer customer = customerService.getCustomer(id);
            model.addAttribute("customerForm", customerMapper.toCustomerForm(customer));
            model.addAttribute("customer", customer);
            model.addAttribute("CustomerID", id);
            return "dealerPage/customerEdit";
        } catch (Exception e) {
            log.error("Error loading customer for edit: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Customer not found: " + e.getMessage());
            return "redirect:/customer/list";
        }
    }

    @PostMapping("/customer/update/{id}")
    public String updateCustomer(@PathVariable("id") Integer id,
                                 @Valid @ModelAttribute("customerForm") CustomerForm customerForm,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        if (bindingResult.hasErrors()) {
            return prepareEditView(id, customerForm, model, redirectAttributes);
        }

        if (id == null || id <= 0) {
            log.error("Invalid customer ID: {}", id);
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Invalid customer ID");
            return "redirect:/customer/list";
        }

        try {
            customerService.updateCustomer(id, customerForm);
            redirectAttributes.addFlashAttribute("successMessage", "✅ Customer updated successfully");
            return "redirect:/customer/list";
        } catch (com.dealermanagementsysstem.project.exception.BusinessException e) {
            log.error("Business exception updating customer {}: {}", id, e.getMessage(), e);
            model.addAttribute("errorMessage", "❌ " + e.getMessage());
            return prepareEditView(id, customerForm, model, redirectAttributes);
        } catch (Exception e) {
            log.error("Unexpected error updating customer {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Failed to update customer: " + e.getMessage());
            return "redirect:/customer/edit/" + id;
        }
    }

    private String prepareEditView(Integer id, CustomerForm customerForm, Model model, RedirectAttributes redirectAttributes) {
        try {
            DTOCustomer customer = customerService.getCustomer(id);
            model.addAttribute("customer", customer);
            model.addAttribute("customerForm", customerForm);
            model.addAttribute("CustomerID", id);
            return "dealerPage/customerEdit";
        } catch (Exception e) {
            log.error("Error preparing edit view: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Customer not found: " + e.getMessage());
            return "redirect:/customer/list";
        }
    }

    @PostMapping("/customer/delete/{id}")
    public String deleteCustomer(@PathVariable("id") int id, RedirectAttributes redirectAttributes) {
        customerService.deleteCustomer(id);
        redirectAttributes.addFlashAttribute("successMessage", "🗑️ Customer deleted successfully!");
        return "redirect:/customer/list";
    }

    @GetMapping("/customer/search")
    public String searchCustomer(@RequestParam(value = "keyword", required = false, defaultValue = "") String keyword, Model model) {
        List<DTOCustomer> customerList = customerService.searchCustomer(keyword);
        model.addAttribute("customers", customerList);
        model.addAttribute("keyword", keyword);
        return "dealerPage/betterCustomerListFinal";
    }

    @GetMapping("/customer/detail/{id}")
    public String showCustomerDetail(@PathVariable("id") int id, Model model) {
        DTOCustomer customer = customerService.getCustomer(id);
        DTOTestDrive testDrive = testDriveService.findByCustomerID(id);
        model.addAttribute("customer", customer);
        model.addAttribute("testDrive", testDrive);
        return "dealerPage/customerDetail";
    }

}
