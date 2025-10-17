package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/saleorder")
public class OrderController {

    private final DAOSaleOrder dao;

    // ⚙️ SỬA LẠI TÊN CONSTRUCTOR CHO TRÙNG CLASS
    public OrderController() {
        this.dao = new DAOSaleOrder();
    }

    // ✅ Hiển thị danh sách tất cả đơn hàng
    @GetMapping
    public String listSaleOrders(Model model) {
        List<DTOSaleOrder> orders = dao.getAllSaleOrders();
        model.addAttribute("orders", orders);
        return "dealerPage/saleOrderList";
    }

    // ✅ Hiển thị form tạo đơn hàng mới
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("order", new DTOSaleOrder());
        model.addAttribute("detail", new DTOSaleOrderDetail());
        return "dealerPage/createSaleOrder";
    }

    // ✅ Xử lý khi submit form tạo đơn hàng
    @PostMapping("/insert")
    public String insertSaleOrder(
            @RequestParam("customerID") int customerID,
            @RequestParam("dealerID") int dealerID,
            @RequestParam("staffID") int staffID,
            @RequestParam("vin") String vin,
            @RequestParam("price") java.math.BigDecimal price,
            @RequestParam(value = "status", required = false, defaultValue = "Pending") String status,
            Model model
    ) {
        System.out.println("🧩 [DEBUG] Creating SaleOrder for CustomerID: " + customerID + ", VIN: " + vin);

        DTOSaleOrder order = new DTOSaleOrder();
        order.setCustomer(new DTOCustomer());
        order.getCustomer().setCustomerID(customerID);

        order.setDealer(new DTODealer());
        order.getDealer().setDealerID(dealerID);

        order.setStaff(new DTODealerStaff());
        order.getStaff().setStaffID(staffID);

        order.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        order.setStatus(status);

        DTOVehicle vehicle = new DTOVehicle();
        vehicle.setVIN(vin);

        DTOSaleOrderDetail detail = new DTOSaleOrderDetail();
        detail.setVehicle(vehicle);
        detail.setPrice(price);

        List<DTOSaleOrderDetail> detailList = new ArrayList<>();
        detailList.add(detail);
        order.setDetail(detailList);

        boolean success = dao.insertSaleOrder(order);

        if (success) {
            System.out.println("✅ [SUCCESS] SaleOrder created successfully for VIN: " + vin);
            return "redirect:/saleorder";
        } else {
            System.out.println("❌ [FAILED] Failed to create SaleOrder for VIN: " + vin);
            model.addAttribute("error", "Failed to create sale order. Please check input data!");
            return "dealerPage/createSaleOrder";
        }
    }

    // ✅ Chi tiết 1 đơn hàng
    @GetMapping("/detail/{id}")
    public String viewOrderDetail(@PathVariable("id") int id, Model model) {
        List<DTOSaleOrder> list = dao.getAllSaleOrders();
        DTOSaleOrder order = list.stream()
                .filter(o -> o.getSaleOrderID() == id)
                .findFirst()
                .orElse(null);

        if (order == null) {
            model.addAttribute("error", "Order not found!");
            return "redirect:/saleorder";
        }

        model.addAttribute("order", order);
        return "dealerPage/saleOrderDetail";
    }
}
