package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DAOCustomer;
import com.dealermanagementsysstem.project.Model.DAOTestDrive;
import com.dealermanagementsysstem.project.Model.DAOVehicle;
import com.dealermanagementsysstem.project.Model.DAODealer;
import com.dealermanagementsysstem.project.Model.DTOCustomer;
import com.dealermanagementsysstem.project.Model.DTOTestDrive;
import com.dealermanagementsysstem.project.Model.DTOAccount;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Controller
public class CustomerController {

    @Autowired
    private DAOCustomer daoCustomer;

    @Autowired
    private DAOTestDrive daoTestDrive;

    @Autowired
    private DAOVehicle daoVehicle;

    @Autowired
    private DAODealer daoDealer;

    // ✅ Khi người dùng vào /customer → tự động chuyển hướng tới /customer/list
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

    // ✅ Form tạo mới Customer
    @GetMapping("/customer/create")
    public String showCreateForm(Model model) {
        model.addAttribute("customer", new DTOCustomer());
        return "dealerPage/createANewCustomer";
    }

    // ✅ Lưu customer mới
    @PostMapping("/customer/save")
    public String saveCustomer(@ModelAttribute("customer") DTOCustomer c,
                               @RequestParam(value = "testDriveSchedule", required = false) String testDriveSchedule,
                               RedirectAttributes redirectAttributes) {

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

                    // ✅ Lấy dealerID từ customer (mặc định là 1 nếu không có)
                    Integer dealerID = (c.getDealer() != null && c.getDealer().getDealerID() > 0)
                                      ? c.getDealer().getDealerID()
                                      : 1; // Default dealer

                    // ✅ Lấy staffID từ dealer (staff đầu tiên của dealer)
                    Integer staffID = daoDealer.getFirstStaffIdByDealerId(dealerID);
                    if (staffID == null) {
                        System.out.println("⚠️ No staff found for DealerID=" + dealerID + ", TestDrive will have StaffID=NULL");
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
    // ✅ Hiển thị chi tiết khách hàng
    @GetMapping("/customer/detail/{id}")
    public String showCustomerDetail(@PathVariable("id") int id, Model model, RedirectAttributes redirectAttributes) {
        DTOCustomer customer = daoCustomer.getCustomerById(id);
        if (customer == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "❌ Customer not found!");
            return "redirect:/customer/list";
        }

        // ✅ Lấy test drive schedule (nếu có)
        DTOTestDrive testDrive = daoTestDrive.getTestDriveByCustomerId(id);

        model.addAttribute("customer", customer);
        model.addAttribute("testDrive", testDrive); // ✅ Thêm test drive vào model

        return "dealerPage/customerDetail";
    }

}
