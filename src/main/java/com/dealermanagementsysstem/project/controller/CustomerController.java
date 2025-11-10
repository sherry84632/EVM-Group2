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

    // ✅ Hiển thị danh sách khách hàng (Better List) - FILTERED BY DEALER
    @GetMapping("/customer/list")
    public String listCustomers(Model model, HttpSession session) {
        // Get current logged-in account
        DTOAccount loggedInAccount = (DTOAccount) session.getAttribute("loggedInAccount");

        List<DTOCustomer> customerList;

        // Filter by dealer if user is DEALER or DEALERSTAFF
        if (loggedInAccount != null && loggedInAccount.getDealerStaff() != null
            && loggedInAccount.getDealerStaff().getDealer() != null) {
            int dealerID = loggedInAccount.getDealerStaff().getDealer().getDealerID();
            customerList = daoCustomer.getCustomersByDealerId(dealerID);
            model.addAttribute("dealerFiltered", true);
            model.addAttribute("dealerID", dealerID);
        } else {
            // Admin/EVM - show all customers
            customerList = daoCustomer.getAllCustomers();
            model.addAttribute("dealerFiltered", false);
        }

        model.addAttribute("customers", customerList);
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
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {

        // ✅ Lấy dealerID từ account đang đăng nhập (fix bug default = 1)
        DTOAccount loggedInAccount = (DTOAccount) session.getAttribute("loggedInAccount");
        if (loggedInAccount != null && loggedInAccount.getDealerStaff() != null
            && loggedInAccount.getDealerStaff().getDealer() != null) {
            int dealerID = loggedInAccount.getDealerStaff().getDealer().getDealerID();

            // Set dealerID vào customer
            DTODealer dealer = new DTODealer();
            dealer.setDealerID(dealerID);
            c.setDealer(dealer);

            System.out.println("✅ Creating customer for DealerID=" + dealerID);
        } else {
            System.out.println("⚠️ No dealer found in session, customer will have DealerID=NULL");
        }

        // ✅ Lưu customer và lấy customerID
        int newCustomerID = daoCustomer.insertCustomer(c);

        if (newCustomerID > 0) {
            // ✅ Nếu có test drive schedule, lưu vào bảng TestDrive
            if (testDriveSchedule != null && !testDriveSchedule.isEmpty()) {
                try {
                    LocalDateTime testDateTime = LocalDateTime.parse(testDriveSchedule);
                    Date testDate = Date.from(testDateTime.atZone(ZoneId.systemDefault()).toInstant());

                    // ✅ Tìm VehicleID từ vehicleInterest (nếu có)
                    Integer vehicleID = null;
                    if (c.getVehicleInterest() != null && !c.getVehicleInterest().trim().isEmpty()) {
                        vehicleID = daoVehicle.findAvailableVehicleByModelName(c.getVehicleInterest());
                        if (vehicleID == null) {
                            System.out.println("⚠️ No available vehicle found for: " + c.getVehicleInterest());
                        }
                    }

                    // ✅ Lấy dealerID từ customer (đã được set từ session ở trên)
                    Integer dealerID = (c.getDealer() != null && c.getDealer().getDealerID() > 0)
                                      ? c.getDealer().getDealerID()
                                      : null; // Không dùng default nữa

                    // ✅ Lấy staffID từ dealer (staff đầu tiên của dealer)
                    Integer staffID = null;
                    if (dealerID != null) {
                        staffID = daoDealer.getFirstStaffIdByDealerId(dealerID);
                        if (staffID == null) {
                            System.out.println("⚠️ No staff found for DealerID=" + dealerID + ", TestDrive will have StaffID=NULL");
                        }
                    }

                    // ✅ Tạo test drive với đầy đủ thông tin
                    boolean testDriveSaved = daoTestDrive.insertTestDrive(
                        newCustomerID, testDate, vehicleID, dealerID, staffID
                    );

                    if (testDriveSaved) {
                        if (vehicleID != null) {
                            redirectAttributes.addFlashAttribute("successMessage",
                                "✅ Customer and Test Drive added successfully with vehicle!");
                        } else {
                            redirectAttributes.addFlashAttribute("successMessage",
                                "✅ Customer and Test Drive added successfully (vehicle will be assigned later)!");
                        }
                    } else {
                        redirectAttributes.addFlashAttribute("successMessage",
                            "✅ Customer added but test drive failed to save!");
                    }
                } catch (Exception e) {
                    System.out.println("⚠️ Failed to save test drive: " + e.getMessage());
                    e.printStackTrace();
                    redirectAttributes.addFlashAttribute("successMessage",
                        "✅ Customer added successfully but test drive failed!");
                }
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "✅ Customer added successfully!");
            }
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Failed to add customer!");
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

    // ✅ Cập nhật Customer
    @PostMapping("/customer/update")
    public String updateCustomer(@ModelAttribute("customer") DTOCustomer c,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {

        // ✅ IMPORTANT: Preserve dealerID from existing customer (fix bug: dealerID → NULL after update)
        // Form doesn't submit dealerID, so we need to get it from the existing record
        DTOCustomer existingCustomer = daoCustomer.getCustomerById(c.getCustomerID());
        if (existingCustomer != null && existingCustomer.getDealer() != null) {
            // Preserve existing dealer
            c.setDealer(existingCustomer.getDealer());
            System.out.println("✅ Preserving DealerID=" + existingCustomer.getDealer().getDealerID() + " for customer update");
        } else {
            // Fallback: try to get from session (same as create)
            DTOAccount loggedInAccount = (DTOAccount) session.getAttribute("loggedInAccount");
            if (loggedInAccount != null && loggedInAccount.getDealerStaff() != null
                && loggedInAccount.getDealerStaff().getDealer() != null) {
                int dealerID = loggedInAccount.getDealerStaff().getDealer().getDealerID();
                DTODealer dealer = new DTODealer();
                dealer.setDealerID(dealerID);
                c.setDealer(dealer);
                System.out.println("⚠️ No existing dealer, using session DealerID=" + dealerID);
            } else {
                System.out.println("⚠️ Cannot determine dealerID for customer update");
            }
        }

        boolean success = daoCustomer.updateCustomer(c);

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

    // ✅ Tìm kiếm Customer - FILTERED BY DEALER
    @GetMapping("/customer/search")
    public String searchCustomer(@RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
                                 Model model, HttpSession session) {
        // Get current logged-in account
        DTOAccount loggedInAccount = (DTOAccount) session.getAttribute("loggedInAccount");

        List<DTOCustomer> customerList;

        // Nếu keyword trống hoặc null → Hiển thị full list (filtered by dealer)
        if (keyword == null || keyword.trim().isEmpty()) {
            // Filter by dealer if user is DEALER or DEALERSTAFF
            if (loggedInAccount != null && loggedInAccount.getDealerStaff() != null
                && loggedInAccount.getDealerStaff().getDealer() != null) {
                int dealerID = loggedInAccount.getDealerStaff().getDealer().getDealerID();
                customerList = daoCustomer.getCustomersByDealerId(dealerID);
                model.addAttribute("dealerFiltered", true);
                model.addAttribute("dealerID", dealerID);
            } else {
                customerList = daoCustomer.getAllCustomers();
                model.addAttribute("dealerFiltered", false);
            }
            System.out.println("ℹ️ Search with empty keyword → Returning customers (" + customerList.size() + " found)");
        } else {
            // Search with keyword - also filter by dealer
            if (loggedInAccount != null && loggedInAccount.getDealerStaff() != null
                && loggedInAccount.getDealerStaff().getDealer() != null) {
                int dealerID = loggedInAccount.getDealerStaff().getDealer().getDealerID();
                customerList = daoCustomer.searchCustomerByDealerId(keyword.trim(), dealerID);
                model.addAttribute("dealerFiltered", true);
                model.addAttribute("dealerID", dealerID);
            } else {
                customerList = daoCustomer.searchCustomer(keyword.trim());
                model.addAttribute("dealerFiltered", false);
            }
            System.out.println("🔍 Search for: '" + keyword + "' → Found " + customerList.size() + " customers");
        }

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
