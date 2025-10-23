// src/main/java/com/dealermanagementsysstem/project/controller/OrderController.java
package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/saleorder")
public class OrderController {

    private final DAOSaleOrder dao = new DAOSaleOrder();

    // (Temporary) Landing page since listing retrieval methods are absent in DAO
    @GetMapping
    public String landing(Model model) {
        model.addAttribute("message", "Listing not available (DAO missing getAllSaleOrders).");
        model.addAttribute("orders", List.of());
        return "dealerPage/dealerCustomerOrderList";
    }

    // Form create
    @GetMapping("/new")
    public String showCreateForm(Model model, HttpSession session) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        DAOAccount daoAccount = new DAOAccount();
        DTOAccount account = daoAccount.findAccountByEmail(email);

        if (account == null || account.getDealerId() == null) {
            model.addAttribute("error", "Bạn cần đăng nhập bằng tài khoản dealer!");
            return "redirect:/login";
        }

        DAOQuotation quotationDAO = new DAOQuotation();
        List<DTOQuotation> approvedQuotations = quotationDAO.getQuotationsByDealer(account.getDealerId())
                .stream()
                .filter(q -> {
                    String s = q.getStatus();
                    return s != null && (s.equalsIgnoreCase("Approved") || s.equalsIgnoreCase("Accepted"));
                })
                .toList();

        if (approvedQuotations.isEmpty()) {
            model.addAttribute("error", "Không có quotation nào được duyệt!");
            return "dealerPage/noQuotations";
        }

        model.addAttribute("order", new DTOSaleOrder());
        model.addAttribute("quotations", approvedQuotations);
        return "dealerPage/createSaleOrder";
    }

    // Insert
    @PostMapping("/insert")
    public String insertSaleOrder(@RequestParam("quantity") int quantity,
                                  @RequestParam("customerID") int customerID,
                                  @RequestParam("staffID") int staffID,
                                  @RequestParam("vin") String vin,
                                  @RequestParam("quotationID") int quotationID,
                                  @RequestParam(value = "status", required = false, defaultValue = "Pending") String status,
                                  Model model) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        DAOAccount daoAccount = new DAOAccount();
        DTOAccount account = daoAccount.findAccountByEmail(email);

        if (account == null || account.getDealerId() == null) {
            model.addAttribute("error", "Tài khoản hiện tại không hợp lệ hoặc chưa đăng nhập!");
            return "redirect:/login";
        }

        Integer dealerID = account.getDealerId();

        DAOQuotation quotationDAO = new DAOQuotation();
        DTOQuotation quotation = quotationDAO.getQuotationById(quotationID);
        if (quotation == null || !("Approved".equalsIgnoreCase(quotation.getStatus()) ||
                "Accepted".equalsIgnoreCase(quotation.getStatus()))) {
            model.addAttribute("error", "Quotation không hợp lệ hoặc chưa được duyệt!");
            return "redirect:/quotation/list";
        }

        DTOSaleOrder order = new DTOSaleOrder();

        DTOCustomer customer = new DTOCustomer();
        customer.setCustomerID(customerID);
        order.setCustomer(customer);

        DTODealer dealer = new DTODealer();
        dealer.setDealerID(dealerID);
        dealer.setPolicyID(quotation.getDealer().getPolicyID());
        order.setDealer(dealer);

        DTODealerStaff staff = new DTODealerStaff();
        staff.setStaffID(staffID);
        order.setStaff(staff);

        order.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        order.setStatus(status);

        DTOVehicle vehicle = new DTOVehicle();
        vehicle.setVIN(vin);

        DTOSaleOrderDetail detail = new DTOSaleOrderDetail();
        detail.setVehicle(vehicle);
        BigDecimal unitPrice = BigDecimal.ZERO;
        if (quotation.getQuotationDetails() != null && !quotation.getQuotationDetails().isEmpty()) {
            unitPrice = quotation.getQuotationDetails().get(0).getUnitPrice();
        }
        detail.setPrice(unitPrice);
        detail.setQuantity(quantity);

        List<DTOSaleOrderDetail> details = new ArrayList<>();
        details.add(detail);
        order.setDetail(details);

        boolean success = dao.createSaleOrder(order);

        if (success) {
            model.addAttribute("message", "Tạo đơn hàng thành công!");
            return "redirect:/saleorder";
        } else {
            model.addAttribute("error", "Không thể tạo đơn hàng, vui lòng thử lại.");
            return "dealerPage/createSaleOrder";
        }
    }

    // Update status only
    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable("id") int id,
                               @RequestParam("status") String status,
                               Model model) {
        boolean ok = dao.updateSaleOrderStatus(id, status);
        if (!ok) {
            model.addAttribute("error", "Cập nhật trạng thái thất bại!");
        }
        return "redirect:/saleorder";
    }

    // Full update (expects form posts all fields & one detail; expand as needed)
    @PostMapping("/update")
    public String updateSaleOrder(@RequestParam("saleOrderID") int saleOrderID,
                                  @RequestParam("customerID") int customerID,
                                  @RequestParam("dealerID") int dealerID,
                                  @RequestParam("staffID") int staffID,
                                  @RequestParam("status") String status,
                                  @RequestParam("vin") String vin,
                                  @RequestParam("quantity") int quantity,
                                  @RequestParam("price") BigDecimal price,
                                  Model model) {

        DTOSaleOrder order = new DTOSaleOrder();
        order.setSaleOrderID(saleOrderID);

        DTOCustomer c = new DTOCustomer();
        c.setCustomerID(customerID);
        order.setCustomer(c);

        DTODealer d = new DTODealer();
        d.setDealerID(dealerID);
        // PolicyID must be supplied by form or looked up; placeholder 0
        d.setPolicyID(0);
        order.setDealer(d);

        DTODealerStaff s = new DTODealerStaff();
        s.setStaffID(staffID);
        order.setStaff(s);

        order.setStatus(status);

        DTOVehicle vehicle = new DTOVehicle();
        vehicle.setVIN(vin);

        DTOSaleOrderDetail det = new DTOSaleOrderDetail();
        det.setVehicle(vehicle);
        det.setPrice(price);
        det.setQuantity(quantity);

        order.setDetail(List.of(det));

        boolean ok = dao.updateSaleOrder(order);
        if (!ok) {
            model.addAttribute("error", "Cập nhật đơn hàng thất bại!");
        }
        return "redirect:/saleorder";
    }
}
