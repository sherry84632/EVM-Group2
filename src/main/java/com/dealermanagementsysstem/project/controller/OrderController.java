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
            @RequestParam(value = "status", required = false, defaultValue = "CREATED") String status,
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

        // Normalize status input (ui might send Pending/Delivered etc.)
        String normalized = status.trim().toUpperCase();
        switch (normalized) {
            case "PENDING" -> normalized = "CREATED";
            case "DELIVERED" -> normalized = "COMPLETED";
            case "PROCESSING", "SHIPPED", "COMPLETED", "CANCELLED", "CREATED", "CONTRACT_SIGNED" -> {}
            default -> normalized = "CREATED";
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

        // === Build chi tiết đơn hàng (SaleOrderDetail) ===
        List<DTOSaleOrderDetail> details = new ArrayList<>();
        int computedTotalQty = 0;
        java.math.BigDecimal computedTotalAmount = java.math.BigDecimal.ZERO;
        DAOVehicle vehicleDAO = new DAOVehicle();
        if (quotation.getQuotationDetails() != null && !quotation.getQuotationDetails().isEmpty()) {
            for (DTOQuotationDetail qd : quotation.getQuotationDetails()) {
                int lineQty = qd.getQuantity();
                Integer versionId = qd.getVersion()!=null? qd.getVersion().getVersionID(): null;
                Integer colorId = qd.getColor()!=null? qd.getColor().getColorID(): null;
                List<Integer> vehicleIds = new ArrayList<>();
                if (versionId != null && colorId != null) {
                    vehicleIds = vehicleDAO.findAvailableVehicleIdsByVersionAndColor(versionId, colorId, lineQty);
                    if (vehicleIds.size() < lineQty) {
                        // fallback any status
                        vehicleIds = vehicleDAO.findVehicleIdsByVersionAndColorAllStatuses(versionId, colorId, lineQty);
                    }
                }
                if (vehicleIds.size() < lineQty) {
                    model.addAttribute("error", "Không đủ xe (Version="+versionId+", Color="+colorId+") để tạo đơn. Cần " + lineQty + ", chỉ có " + vehicleIds.size());
                    return "redirect:/quotation/detail/" + quotationID;
                }
                for (int i=0;i<lineQty;i++) {
                    DTOSaleOrderDetail sod = new DTOSaleOrderDetail();
                    sod.setSaleOrder(order);
                    DTOVehicle veh = new DTOVehicle();
                    veh.setVehicleID(vehicleIds.get(i));
                    sod.setVehicle(veh);
                    sod.setPrice(qd.getUnitPrice()!=null?qd.getUnitPrice():java.math.BigDecimal.ZERO);
                    sod.setQuantity(1); // each detail represents 1 physical vehicle
                    if (quotation.getDealer()!=null && quotation.getDealer().getPolicyID() > 0) {
                        DTODiscountPolicy policy = new DTODiscountPolicy(); policy.setPolicyID(quotation.getDealer().getPolicyID()); sod.setDiscountPolicy(policy);
                    }
                    details.add(sod);
                    computedTotalQty += 1;
                    computedTotalAmount = computedTotalAmount.add(sod.getPrice());
                }
            }
        } else {
            // fallback single line if no quotation details
            DTOSaleOrderDetail sod = new DTOSaleOrderDetail();
            if (vehicleId != null) {
                DTOVehicle veh = new DTOVehicle(); veh.setVehicleID(vehicleId); sod.setVehicle(veh);
            }
            sod.setPrice(java.math.BigDecimal.valueOf(quotation.getTotalPrice()));
            sod.setQuantity(quantity);
            if (quotation.getDealer()!=null && quotation.getDealer().getPolicyID()>0) {
                DTODiscountPolicy policy = new DTODiscountPolicy(); policy.setPolicyID(quotation.getDealer().getPolicyID()); sod.setDiscountPolicy(policy);
            }
            details.add(sod);
            computedTotalQty = quantity;
            computedTotalAmount = java.math.BigDecimal.valueOf(quotation.getTotalPrice());
        }
        order.setTotalQuantity(computedTotalQty);
        order.setTotalAmount(computedTotalAmount);
        order.setDetail(details);
        order.setStatus(SaleOrderStatus.valueOf(normalized));

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
