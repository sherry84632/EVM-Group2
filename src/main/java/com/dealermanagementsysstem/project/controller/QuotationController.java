package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.*;
import com.dealermanagementsysstem.project.Model.DAOQuotation;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.List;

@Controller
@RequestMapping("/quotation")
public class QuotationController {

    private final DAOQuotation dao = new DAOQuotation();
    private final DAOPurchaseOrder purchaseOrderDao = new DAOPurchaseOrder();
    private final DAOQuotationDetail quotationDetailDao = new DAOQuotationDetail();
    private final DAOPurchaseOrderDetail purchaseOrderDetailDao = new DAOPurchaseOrderDetail();

    // ✅ 1. Hiển thị danh sách tất cả báo giá
    @GetMapping
    public String listQuotations(Model model) {
        List<DTOQuotation> list = dao.getAllQuotations();
        model.addAttribute("quotations", list);
        return "quotation-list"; // -> tên file HTML hiển thị danh sách
    }

    // ✅ 2. Hiển thị form thêm mới báo giá
    @GetMapping("/new")
    public String showNewForm(Model model) {
        DTOQuotation quotation = new DTOQuotation();
        model.addAttribute("quotation", quotation);
        return "quotation-form"; // -> form thêm mới
    }

    // ✅ 3. Thêm mới báo giá
    @PostMapping("/insert")
    public String insertQuotation(
            @RequestParam("dealerId") int dealerId,
            @RequestParam("customerId") int customerId,
            @RequestParam("staffId") int staffId,
            @RequestParam("colorId") int colorId,
            @RequestParam("quantity") int quantity,
            @RequestParam("unitPrice") java.math.BigDecimal unitPrice,
            @RequestParam(value = "vin", required = false) String vin,
            @RequestParam(value = "status", required = false, defaultValue = "Pending") String status
    ) {
        DTOQuotation q = new DTOQuotation();

        // Tạo các đối tượng liên kết
        DTODealer dealer = new DTODealer();
        dealer.setDealerID(dealerId);
        q.setDealer(dealer);

        DTOCustomer customer = new DTOCustomer();
        customer.setCustomerID(customerId);
        q.setCustomer(customer);

        // Staff placeholder: stored as staffId in Quotation table per schema

        q.setStatus(status);
        q.setCreatedAt(new Timestamp(System.currentTimeMillis()));

        Integer newQuotationId = dao.insertQuotationReturningId(dealerId, customerId, staffId, new Timestamp(System.currentTimeMillis()), status);
        if (newQuotationId == null) return "redirect:/quotation/new?error=1";

        DTOQuotationDetail d = new DTOQuotationDetail();
        d.setQuotationId(newQuotationId);
        d.setColorId(colorId);
        d.setQuantity(quantity);
        d.setUnitPrice(unitPrice);
        d.setVin(vin);
        quotationDetailDao.insertDetail(d);

        return "redirect:/quotation";
    }

    // ✅ 4. Tìm kiếm báo giá theo từ khóa
    @GetMapping("/search")
    public String searchQuotation(@RequestParam("keyword") String keyword, Model model) {
        List<DTOQuotation> list = dao.searchQuotation(keyword);
        model.addAttribute("quotations", list);
        model.addAttribute("keyword", keyword);
        return "quotation-list";
    }

    // ✅ 5. Cập nhật trạng thái báo giá
    @PostMapping("/update-status")
    public String updateStatus(@RequestParam("quotationId") int quotationId,
                               @RequestParam("newStatus") String newStatus) {
        boolean success = dao.updateQuotationStatus(quotationId, newStatus);
        return "redirect:/quotation";
    }

    // ✅ 6. Convert quotation -> purchase order (status Pending)
    @GetMapping("/{id}/convert-to-order")
    public String convertToOrder(@PathVariable("id") int id) {
        DTOQuotation q = dao.getQuotationById(id);
        if (q == null) return "redirect:/quotation";

        DTOPurchaseOrder order = new DTOPurchaseOrder();
        order.setDealerId(q.getDealer().getDealerID());
        // TODO: get staffId from session; use dealer staff placeholder for now
        order.setStaffId(1);
        order.setStatus("Pending");
        Integer orderId = purchaseOrderDao.addPurchaseOrderReturningId(order);
        if (orderId == null) {
            return "redirect:/quotation";
        }

        // Map QuotationDetail rows to PurchaseOrderDetail rows
        List<DTOQuotationDetail> details = quotationDetailDao.getDetailsByQuotationId(id);
        for (DTOQuotationDetail d : details) {
            DTOPurchaseOrderDetail pod = new DTOPurchaseOrderDetail();
            pod.setPurchaseOrderId(orderId);
            pod.setColorId(d.getColorId());
            pod.setQuantity(d.getQuantity());
            purchaseOrderDetailDao.addDetail(pod);
        }

        return "redirect:/orderdealer/list";
    }
}
