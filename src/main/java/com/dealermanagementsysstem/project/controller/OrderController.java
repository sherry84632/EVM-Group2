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

    // ======================================================
    // 1️⃣ HIỂN THỊ DANH SÁCH TẤT CẢ ĐƠN HÀNG
    // ======================================================
    @GetMapping
    public String listSaleOrders(Model model) {
        List<DTOSaleOrder> orders = dao.getAllSaleOrders();
        model.addAttribute("orders", orders);
        return "dealerPage/dealerCustomerOrderList";
    }

    // ======================================================
    // 2️⃣ FORM TẠO ĐƠN HÀNG MỚI
    // ======================================================
    @GetMapping("/new")
    public String showCreateForm(Model model, HttpSession session) {

        // ✅ Lấy user từ Spring Security
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

    // ======================================================
    // 3️⃣ XỬ LÝ SUBMIT FORM
    // ======================================================
    @PostMapping("/insert")
    public String insertSaleOrder(
            @RequestParam("quantity") int quantity,
            @RequestParam("customerID") int customerID,
            @RequestParam("staffID") int staffID,
            @RequestParam("vin") String vin,
            @RequestParam("quotationID") int quotationID,
            @RequestParam(value = "status", required = false, defaultValue = "Pending") String status,
            Model model
    ) {
        // ✅ Lấy user từ Spring Security
        System.out.println("CONCKCNCCCGFG " + quantity );
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        DAOAccount daoAccount = new DAOAccount();
        DTOAccount account = daoAccount.findAccountByEmail(email);

        if (account == null || account.getDealerId() == null) {
            model.addAttribute("error", "Tài khoản hiện tại không hợp lệ hoặc chưa đăng nhập!");
            return "redirect:/login";
        }

    // Debug removed: vin available via param if needed for logging framework

        Integer dealerID = account.getDealerId();

        DAOQuotation quotationDAO = new DAOQuotation();
        DTOQuotation quotation = quotationDAO.getQuotationById(quotationID);
        if (quotation == null || !("Approved".equalsIgnoreCase(quotation.getStatus()) || "Accepted".equalsIgnoreCase(quotation.getStatus()))) {
            model.addAttribute("error", "Quotation không hợp lệ hoặc chưa được duyệt!");
            return "redirect:/quotation/list";
        }

        // === Build DTO ===
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

        // === Detail ===
        DTOVehicle vehicle = new DTOVehicle();
        vehicle.setVIN(vin);

        DTOSaleOrderDetail detail = new DTOSaleOrderDetail();
        detail.setVehicle(vehicle);

        // ✅ FIX: Kiểm tra null trước khi gọi get(0)
        BigDecimal unitPrice = BigDecimal.ZERO;
        if (quotation.getQuotationDetails() != null && !quotation.getQuotationDetails().isEmpty()) {
            unitPrice = quotation.getQuotationDetails().get(0).getUnitPrice();
        }
        detail.setPrice(unitPrice);
        detail.setQuantity(quantity);

        List<DTOSaleOrderDetail> details = new ArrayList<>();
        details.add(detail);
        order.setDetail(details);

        // === Insert ===
        boolean success = dao.createSaleOrder(order);

        if (success) {
            model.addAttribute("message", "Tạo đơn hàng thành công!");
            return "redirect:/saleorder";
        } else {
            model.addAttribute("error", "Không thể tạo đơn hàng, vui lòng thử lại.");
            return "dealerPage/createSaleOrder";
        }
    }

    // ======================================================
    // 4️⃣ CHI TIẾT 1 ĐƠN HÀNG
    // ======================================================
    @GetMapping("/detail/{id}")
    public String viewOrderDetail(@PathVariable("id") int id, Model model) {
        DTOSaleOrder order = dao.getSaleOrderById(id);
        if (order == null) {
            model.addAttribute("error", "Không tìm thấy đơn hàng!");
            return "redirect:/saleorder";
        }
        model.addAttribute("order", order);
        return "dealerPage/saleOrderDetail";
    }
}
