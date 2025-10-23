package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Controller
@RequestMapping("/saleorder")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private DAOSaleOrder daoSaleOrder;

    @Autowired
    private DAOQuotation daoQuotation;

    // ✅ [GET] Danh sách tất cả SaleOrder
    @GetMapping("/list")
    public String listSaleOrders(Model model) {
        log.info("GET /saleorder/list");
        List<DTOSaleOrder> orders = daoSaleOrder.getAllSaleOrders();
        model.addAttribute("orders", orders);
        return "dealerPage/saleOrderList"; // đường dẫn tới file .html hiển thị danh sách SaleOrder
    }

    // ✅ [POST] Tạo SaleOrder từ Quotation đã duyệt
    @PostMapping("/create-from-quotation/{quotationID}")
    public String createSaleOrderFromQuotation(@PathVariable int quotationID, Model model) {
        log.info("POST /saleorder/create-from-quotation/{}", quotationID);

        try {
            // Kiểm tra trạng thái quotation
            boolean approved = daoQuotation.isQuotationApproved(quotationID);
            if (!approved) {
                model.addAttribute("error", "Quotation này chưa được duyệt. Không thể tạo SaleOrder.");
                return "redirect:/quotation/list";
            }

            // Tạo SaleOrder mới
            int saleOrderID = daoSaleOrder.createSaleOrderFromQuotation(quotationID);

            if (saleOrderID > 0) {
                model.addAttribute("success", "Tạo SaleOrder thành công (ID: " + saleOrderID + ")");
                log.info("SaleOrder created successfully ID={}", saleOrderID);
                return "redirect:/saleorder/list";
            } else {
                model.addAttribute("error", "Không thể tạo SaleOrder. Vui lòng kiểm tra log.");
                return "redirect:/quotation/list";
            }

        } catch (Exception e) {
            log.error("Error creating SaleOrder from QuotationID={}", quotationID, e);
            model.addAttribute("error", "Lỗi hệ thống khi tạo SaleOrder.");
            return "redirect:/quotation/list";
        }
    }

    // ✅ [GET] Chi tiết một SaleOrder cụ thể
    @GetMapping("/detail/{saleOrderID}")
    public String getSaleOrderDetail(@PathVariable int saleOrderID, Model model) {
        log.info("GET /saleorder/detail/{}", saleOrderID);
        // Giả sử bạn có hàm getSaleOrderById() trong DAO (có thể thêm sau)
        // DTOSaleOrder order = daoSaleOrder.getSaleOrderById(saleOrderID);
        // model.addAttribute("order", order);
        return "dealerPage/saleOrderDetail";
    }
}
