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
@RequestMapping({"/order","/saleorder"})
public class OrderController {

    private final DAOSaleOrder dao = new DAOSaleOrder();

    // ======================================================
    // 1️⃣  DANH SÁCH TẤT CẢ SALE ORDER
    // ======================================================
    @GetMapping
    public String listSaleOrders(Model model) {
        List<DTOSaleOrder> orders = dao.getAllSaleOrders();
        model.addAttribute("orders", orders);
        return "dealerPage/dealerCustomerOrderList";
    }

    // ======================================================
    // 2️⃣  FORM TẠO SALE ORDER MỚI
    // ======================================================
    @GetMapping("/new")
    public String showCreateForm(Model model, HttpSession session) {

        // ✅ Lấy thông tin người dùng đăng nhập
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        DAOAccount daoAccount = new DAOAccount();
        DTOAccount account = daoAccount.findAccountByEmail(email);

        if (account == null || account.getDealerStaff() == null) {
            model.addAttribute("error", "Bạn cần đăng nhập bằng tài khoản dealer!");
            return "redirect:/login";
        }

        // ✅ Lấy danh sách quotation đã duyệt cho dealer này
        DAOQuotation quotationDAO = new DAOQuotation();
        List<DTOQuotation> approvedQuotations = quotationDAO.getQuotationsByDealer(account.getDealerStaff().getDealer().getDealerID())
                .stream()
                .filter(q -> q.getStatus() == QuotationStatus.APPROVED)
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
    // 3️⃣  XỬ LÝ SUBMIT FORM TẠO SALE ORDER
    // ======================================================
    @PostMapping("/insert")
    public String insertSaleOrder(
            @RequestParam("quantity") int quantity,
            @RequestParam("customerID") int customerID,
            @RequestParam("staffID") int staffID,
            @RequestParam(value = "vehicleId", required = false) Integer vehicleId,
            @RequestParam("quotationID") int quotationID,
            @RequestParam(value = "status", required = false, defaultValue = "Pending") String status,
            Model model
    ) {
        // ✅ Lấy thông tin tài khoản hiện tại
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        DAOAccount daoAccount = new DAOAccount();
        DTOAccount account = daoAccount.findAccountByEmail(email);

        if (account == null || account.getDealerStaff()== null) {
            model.addAttribute("error", "Tài khoản hiện tại không hợp lệ hoặc chưa đăng nhập!");
            return "redirect:/login";
        }

        int dealerID = account.getDealerStaff().getDealer().getDealerID();

        // ✅ Lấy quotation được chọn
        DAOQuotation quotationDAO = new DAOQuotation();
        DTOQuotation quotation = quotationDAO.getQuotationById(quotationID);
        if (quotation == null || quotation.getStatus() != QuotationStatus.APPROVED) {
            model.addAttribute("error", "Quotation không hợp lệ hoặc chưa đư���c duyệt!");
            return "redirect:/quotation/list";
        }

        // === Build DTO chính ===
        DTOSaleOrder order = new DTOSaleOrder();

        DTOCustomer customer = new DTOCustomer();
        customer.setCustomerID(customerID);
        order.setCustomer(customer);

        DTODealer dealer = new DTODealer();
        dealer.setDealerID(dealerID);
        order.setDealer(dealer);

        DTODealerStaff staff = new DTODealerStaff();
        staff.setStaffID(staffID);
        order.setStaff(staff);

        // Set quotation relationship
        DTOQuotation quotationRef = new DTOQuotation();
        quotationRef.setQuotationID(quotationID);
        order.setQuotation(quotationRef);

        order.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        order.setStatus(SaleOrderStatus.valueOf(status.toUpperCase()));
        order.setTotalQuantity(quantity);

        // Calculate total amount from quotation
        BigDecimal totalAmount = quotation.getTotalPrice() > 0 ?
            BigDecimal.valueOf(quotation.getTotalPrice()) : BigDecimal.ZERO;
        order.setTotalAmount(totalAmount);

        // === Build chi tiết đơn hàng (SaleOrderDetail) ===
        DTOVehicle vehicle = new DTOVehicle();
        vehicle.setVehicleID(vehicleId);

        DTOSaleOrderDetail detail = new DTOSaleOrderDetail();
        detail.setVehicle(vehicle);

        BigDecimal unitPrice = BigDecimal.ZERO;
        if (quotation.getQuotationDetails() != null && !quotation.getQuotationDetails().isEmpty()) {
            unitPrice = quotation.getQuotationDetails().get(0).getUnitPrice();
        }
        detail.setPrice(unitPrice);

        // Set discount policy if available
        if (quotation.getDealer() != null && quotation.getDealer().getPolicyID() > 0) {
            DTODiscountPolicy discountPolicy = new DTODiscountPolicy();
            discountPolicy.setPolicyID(quotation.getDealer().getPolicyID());
            detail.setDiscountPolicy(discountPolicy);
        }

        List<DTOSaleOrderDetail> details = new ArrayList<>();
        details.add(detail);
        order.setDetail(details);

        // If vehicleId not provided attempt to derive from first quotation detail (placeholder logic)
        if (vehicleId == null) {
            if (quotation != null && quotation.getQuotationDetails()!=null && !quotation.getQuotationDetails().isEmpty()) {
                // We don't have a concrete vehicle, create a synthetic negative id to satisfy FK if allowed
                // Preferably you should map SaleOrderDetail to Version/Color instead of Vehicle when not in inventory yet
                vehicleId = quotation.getQuotationDetails().get(0).getVersion()!=null ? quotation.getQuotationDetails().get(0).getVersion().getVersionID() : 0;
            } else {
                vehicleId = 0; // fallback
            }
        }

        // === Gọi DAO để insert ===
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
    // 4️⃣  XEM CHI TIẾT SALE ORDER
    // ======================================================
    @GetMapping("/detail/{id}")
    public String viewOrderDetail(@PathVariable("id") int id, Model model) {
        DTOSaleOrder order = dao.getSaleOrderById(id);
        if (order == null) {
            model.addAttribute("error", "Không tìm thấy đơn hàng!");
            return "redirect:/saleorder";
        }
        model.addAttribute("order", order);
        return "dealerPage/dealerCustomerOrderDetail";
    }

    // ======================================================
    // 5️⃣  LẤY CHI TIẾT 1 SALE ORDER DETAIL (DỰA VÀO VehicleID)
    // ======================================================
    @GetMapping("/detail/item/{detailId}")
    @ResponseBody
    public DTOSaleOrderDetail getDetailItem(@PathVariable("detailId") int detailId) {
        // ✅ Lấy chi tiết đơn hàng qua DAO
        DAOSaleOrder dao = new DAOSaleOrder();
        return dao.getDetailById(detailId);
    }

    // ======================================================
    // 🟢 UPDATE STATUS ĐƠN HÀNG
    // ======================================================
    @PostMapping("/updateStatus")
    public String updateStatus(
            @RequestParam("saleOrderID") int saleOrderID,
            @RequestParam("status") String status,
            Model model
    ) {
        boolean success = dao.updateSaleOrderStatus(saleOrderID, String.valueOf(SaleOrderStatus.valueOf(status.toUpperCase())));
        if (success) {
            // ✅ Nếu trạng thái là "COMPLETED", xóa xe khỏi inventory
            if (SaleOrderStatus.valueOf(status.toUpperCase()) == SaleOrderStatus.COMPLETED) {
                DTOSaleOrder order = dao.getSaleOrderById(saleOrderID);
                if (order != null && order.getDetail() != null) {
                    DAODealerInventory inventoryDAO = new DAODealerInventory();
                    for (DTOSaleOrderDetail detail : order.getDetail()) {
                        Integer vehicleId = detail.getVehicle().getVehicleID();
                        boolean removed = inventoryDAO.removeVehicleByID(vehicleId);
                        if (!removed) {
                            System.out.println("⚠️ Không thể xóa VehicleID " + vehicleId + " khỏi inventory");
                        }
                    }
                }
            }
            model.addAttribute("message", "Cập nhật trạng thái đơn hàng thành công!");
        } else {
            model.addAttribute("error", "Không thể cập nhật trạng thái đơn hàng!");
            System.out.println("Cap nhat ko thanh coing");
            System.out.println(saleOrderID);
            System.out.println(status);
        }
        return "redirect:/saleorder";
    }

}
