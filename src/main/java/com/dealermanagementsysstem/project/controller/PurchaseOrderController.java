package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.DAOPurchaseOrder;
import com.dealermanagementsysstem.project.Model.DAOPurchaseOrderDetail;
import com.dealermanagementsysstem.project.Model.DTOPurchaseOrderDetail;
import com.dealermanagementsysstem.project.Model.DTOPurchaseOrder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession; // để lấy Dealer từ session
import java.util.List;

@Controller
@RequestMapping("/orderdealer")
public class PurchaseOrderController {

    private final DAOPurchaseOrder dao = new DAOPurchaseOrder();
    private final DAOPurchaseOrderDetail detailDao = new DAOPurchaseOrderDetail();

    // ✅ READ - Danh sách đơn hàng
    @GetMapping("/list")
    public String listOrders(Model model) {
        List<DTOPurchaseOrder> orders = dao.getAllPurchaseOrders();
        model.addAttribute("orders", orders);
        return "evmPage/evmOrderList";
    }

    // ✅ CREATE - Hiển thị form tạo đơn hàng
    @GetMapping("/create")
    public String showCreateForm() {
        return "evmPage/createPurchaseOrder";
    }

    // ✅ CREATE - Xử lý tạo đơn hàng (tự động DealerID & OrderID)
    @PostMapping("/create")
    public String createOrder(
            @RequestParam("staffId") int staffId,
            @RequestParam(value = "status", defaultValue = "Pending") String status,
            HttpSession session
    ) {
        DTOPurchaseOrder order = new DTOPurchaseOrder();

        // 🔹 Tự động lấy DealerID từ session login
        // (khi đăng nhập dealer, bạn lưu dealerID vào session)
        Integer dealerId = (Integer) session.getAttribute("dealerId");
        if (dealerId == null) {
            // Nếu chưa có session, fallback mặc định (demo)
            dealerId = dao.getDealerIdByAccount("dealerA");
        }

        order.setDealerId(dealerId);
        order.setStaffId(staffId);
        order.setStatus(status);

        // DB sẽ tự sinh PurchaseOrderID (IDENTITY)
        dao.addPurchaseOrder(order);

        return "redirect:/dealerPage/orderStatusList";
    }

    // ✅ UPDATE - Hiển thị form chỉnh sửa
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable int id, Model model) {
        DTOPurchaseOrder order = dao.getPurchaseOrderById(id);
        model.addAttribute("details", detailDao.getDetailsByOrderId(id));
        model.addAttribute("order", order);
        return "evmPage/editPurchaseOrder";
    }

    // ✅ UPDATE - Xử lý cập nhật đơn hàng
    @PostMapping("/edit/{id}")
    public String updateOrder(
            @PathVariable int id,
            @RequestParam("staffId") int staffId,
            @RequestParam("status") String status
    ) {
        DTOPurchaseOrder order = dao.getPurchaseOrderById(id);

        if (order != null) {
            order.setStaffId(staffId);
            order.setStatus(status);
            dao.updatePurchaseOrder(order);
        }

        return "redirect:/orderdealer/list";
    }

    // ✅ DELETE
    @GetMapping("/delete/{id}")
    public String deleteOrder(@PathVariable int id) {
        dao.deletePurchaseOrder(id);
        return "redirect:/orderdealer/list";
    }

    // Details management
    @PostMapping("/details/add")
    public String addDetail(@ModelAttribute DTOPurchaseOrderDetail detail, @RequestParam("purchaseOrderId") int orderId) {
        detail.setPurchaseOrderId(orderId);
        detailDao.addDetail(detail);
        return "redirect:/orderdealer/edit/" + orderId;
    }

    @PostMapping("/details/delete")
    public String deleteDetail(@RequestParam("poDetailId") int poDetailId,
                               @RequestParam("purchaseOrderId") int orderId) {
        detailDao.deleteDetail(poDetailId);
        return "redirect:/orderdealer/edit/" + orderId;
    }
}
