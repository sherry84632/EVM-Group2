package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DAOPurchaseOrder;
import com.dealermanagementsysstem.project.Model.DTOPurchaseOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.sql.Timestamp;
import java.util.List;

@Controller
@RequestMapping("/orderdealer")
public class PurchaseOrderController {

    @Autowired
    private DAOPurchaseOrder daoPurchaseOrder;

    // 🔹 Hiển thị danh sách đơn hàng (HTML)
    @GetMapping("")
    public String showOrderList(Model model) {
        List<DTOPurchaseOrder> orders = daoPurchaseOrder.getAllPurchaseOrders();
        model.addAttribute("orders", orders);
        return "dealerPage/orderStatusList"; //
    }


    // 🔹 Hiển thị form tạo đơn hàng
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("order", new DTOPurchaseOrder());
        return "dealerPage/createADealerOrder"; // file HTML tạo đơn
    }

    // 🔹 Xử lý form POST tạo đơn hàng
    @PostMapping("/create")
    public String createOrder(
            @RequestParam(required = false) Integer dealerId,
            @RequestParam(required = false) Integer staffId,
            @RequestParam(required = false) String status,
            Model model) {

        DTOPurchaseOrder order = new DTOPurchaseOrder();
        order.setDealerId(dealerId != null ? dealerId : 9);
        order.setStaffId(staffId != null ? staffId : 11);
        order.setStatus(status != null ? status : "Pending");
        order.setCreatedAt(new Timestamp(System.currentTimeMillis()));

        daoPurchaseOrder.insertPurchaseOrder(order);

        // gửi thông báo sang trang success
        model.addAttribute("message", "The order has been created successfully!");
        return "dealerPage/success";
    }



    // 🔹 Lấy danh sách đơn hàng (JSON)
    @ResponseBody
    @GetMapping("/api")
    public List<DTOPurchaseOrder> getAllOrders() {
        return daoPurchaseOrder.getAllPurchaseOrders();
    }

    // 🔹 Lấy đơn hàng theo ID (JSON)
    @ResponseBody
    @GetMapping("/api/{id}")
    public DTOPurchaseOrder getOrderById(@PathVariable int id) {
        return daoPurchaseOrder.getPurchaseOrderById(id);
    }

    // 🔹 Cập nhật trạng thái đơn hàng
    @ResponseBody
    @PutMapping("/api/{id}/status")
    public String updateStatus(@PathVariable int id, @RequestParam String status) {
        boolean updated = daoPurchaseOrder.updatePurchaseOrderStatus(id, status);
        return updated ? "Updated successfully" : "Update failed";
    }

    // 🔹 Xóa đơn hàng
    @ResponseBody
    @DeleteMapping("/api/{id}")
    public String deleteOrder(@PathVariable int id) {
        int result = daoPurchaseOrder.deletePurchaseOrder(id);
        return result > 0 ? "Deleted successfully" : "Delete failed";
    }
}
