package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping({"/order","/saleorder"})
public class OrderController {

    private final DAOSaleOrder dao = new DAOSaleOrder();
    private final DAOPurchaseOrder purchaseOrderDAO = new DAOPurchaseOrder();
    private final DAOPurchaseOrderDetail purchaseOrderDetailDAO = new DAOPurchaseOrderDetail();

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
    public String showCreateForm(Model model, HttpSession session) { // session kept for future enhancements

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
            @RequestParam(value = "quantity", required = false) Integer quantity,
            @RequestParam("customerID") int customerID,
            @RequestParam("staffID") int staffID,
            @RequestParam(value = "vehicleId", required = false) Integer vehicleId,
            @RequestParam("quotationID") int quotationID,
            @RequestParam(value = "status", required = false, defaultValue = "CREATED") String status,
            Model model,
            RedirectAttributes ra
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
        double baseDiscountPct = quotation.getDiscountPercent()!=null ? quotation.getDiscountPercent() : 0.0; // primitive
        if (quotation.getQuotationDetails() != null && !quotation.getQuotationDetails().isEmpty()) {
            for (DTOQuotationDetail qd : quotation.getQuotationDetails()) {
                int lineQty = qd.getQuantity();
                Integer versionId = qd.getVersion()!=null? qd.getVersion().getVersionID(): null;
                Integer colorId = qd.getColor()!=null? qd.getColor().getColorID(): null;
                List<Integer> vehicleIds = new ArrayList<>();
                if (versionId != null && colorId != null) {
                    vehicleIds = vehicleDAO.findAvailableVehicleIdsByVersionAndColor(versionId, colorId, lineQty);
                    if (vehicleIds.size() < lineQty) {
                        vehicleIds = vehicleDAO.findVehicleIdsByVersionAndColorAllStatuses(versionId, colorId, lineQty);
                    }
                }
                if (vehicleIds.size() < lineQty) {
                    // Auto-create purchase order for missing vehicles
                    int shortage = lineQty - vehicleIds.size();
                    try {
                        int poId = createPurchaseOrderForShortage(dealerID, staffID, versionId, colorId, shortage);
                        if (poId > 0) {
                            ra.addFlashAttribute("message", "⚠️ Không đủ xe trong kho. Hệ thống đã tự động tạo đơn hàng mua #" + poId + " cho " + shortage + " xe còn thiếu. Vui lòng chờ EVM xử lý và thử lại sau.");
                            ra.addFlashAttribute("statusType", "INFO");
                        } else {
                            ra.addFlashAttribute("error", "Không đủ xe (Version="+versionId+", Color="+colorId+") để tạo đơn. Cần " + lineQty + ", chỉ có " + vehicleIds.size() + ". Không thể tạo đơn hàng mua tự động.");
                        }
                    } catch (Exception e) {
                        ra.addFlashAttribute("error", "Không đủ xe (Version="+versionId+", Color="+colorId+") để tạo đơn. Cần " + lineQty + ", chỉ có " + vehicleIds.size() + ". Lỗi tạo đơn hàng mua: " + e.getMessage());
                    }
                    return "redirect:/quotation/detail/" + quotationID;
                }
                // Determine unit net price (after discount). Prefer qd.getFinalNetAfterAll per line, else apply base discount.
                java.math.BigDecimal unitGross = qd.getUnitPrice()!=null? qd.getUnitPrice(): java.math.BigDecimal.ZERO;
                java.math.BigDecimal unitNet;
                if (qd.getFinalNetAfterAll()!=null) {
                    // finalNetAfterAll is total for one unit after stacking (in quotation detail we stored per item net)
                    unitNet = qd.getFinalNetAfterAll();
                } else {
                    unitNet = unitGross.multiply(java.math.BigDecimal.valueOf(1 - baseDiscountPct/100.0));
                }
                for (int i=0;i<lineQty;i++) {
                    DTOSaleOrderDetail sod = new DTOSaleOrderDetail();
                    sod.setSaleOrder(order);
                    DTOVehicle veh = new DTOVehicle();
                    veh.setVehicleID(vehicleIds.get(i));
                    sod.setVehicle(veh);
                    // price stored as discounted unit price
                    sod.setPrice(unitNet);
                    sod.setQuantity(1); // each physical vehicle line qty=1
                    if (quotation.getDealer()!=null && quotation.getDealer().getPolicyID() > 0) {
                        DTODiscountPolicy policy = new DTODiscountPolicy(); policy.setPolicyID(quotation.getDealer().getPolicyID()); sod.setDiscountPolicy(policy);
                    }
                    details.add(sod);
                    computedTotalQty += 1;
                    computedTotalAmount = computedTotalAmount.add(unitNet);
                }
            }
        } else {
            // fallback single line if no quotation details
            DTOSaleOrderDetail sod = new DTOSaleOrderDetail();
            if (vehicleId != null) {
                DTOVehicle veh = new DTOVehicle(); veh.setVehicleID(vehicleId); sod.setVehicle(veh);
            }
            // quotation total already discounted, derive per-unit directly
            int qtyFallback = (quantity != null && quantity > 0) ? quantity : 1;
            java.math.BigDecimal perUnit = java.math.BigDecimal.valueOf(quotation.getTotalPrice())
                    .divide(java.math.BigDecimal.valueOf(Math.max(1, qtyFallback)), java.math.MathContext.DECIMAL64);
            sod.setPrice(perUnit);
            sod.setQuantity(1);
            if (quotation.getDealer()!=null && quotation.getDealer().getPolicyID()>0) {
                DTODiscountPolicy policy = new DTODiscountPolicy(); policy.setPolicyID(quotation.getDealer().getPolicyID()); sod.setDiscountPolicy(policy);
            }
            for (int i=0;i<qtyFallback;i++){ details.add(sod); computedTotalQty += 1; computedTotalAmount = computedTotalAmount.add(sod.getPrice()); }
        }
        order.setTotalQuantity(computedTotalQty);
        order.setTotalAmount(computedTotalAmount);
        order.setDetail(details);
        order.setStatus(SaleOrderStatus.valueOf(normalized));

        // === Gọi DAO để insert ===
        // apply delivery estimate before persisting
        dao.applyPlannedDeliveryEstimate(order);
        boolean success = dao.createSaleOrder(order);
        if (success) {
            // persist delivery info right after insertion
            dao.updateDeliveryInfo(order.getSaleOrderID(), order.getPlannedDeliveryDate(), order.getActualDeliveryDate(), order.getEtaDays());
        }

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
            RedirectAttributes ra
    ) {
        boolean success = dao.updateSaleOrderStatus(saleOrderID, String.valueOf(SaleOrderStatus.valueOf(status.toUpperCase())));
        if (success) {
            DTOSaleOrder order = dao.getSaleOrderById(saleOrderID);
            if (order != null) {
                dao.applyActualDeliveryIfEligible(order);
            }
            if (SaleOrderStatus.valueOf(status.toUpperCase()) == SaleOrderStatus.COMPLETED) {
                if (order != null && order.getDetail() != null) {
                    DAODealerInventory inventoryDAO = new DAODealerInventory();
                    for (DTOSaleOrderDetail detail : order.getDetail()) {
                        Integer vehicleId = detail.getVehicle().getVehicleID();
                        inventoryDAO.removeVehicleByID(vehicleId);
                    }
                }
            }
            ra.addFlashAttribute("message", "Cập nhật trạng thái thành công: " + status.toUpperCase());
        } else {
            ra.addFlashAttribute("error", "Không thể cập nhật trạng thái đơn hàng!");
        }
        return "redirect:/saleorder/detail/" + saleOrderID;
    }

    // ======================================================
    // 🔧 HELPER: Tạo Purchase Order cho xe thiếu
    // ======================================================
    private int createPurchaseOrderForShortage(int dealerID, int staffID, int versionId, int colorId, int shortageQty) {
        try {
            // Get model ID from version
            DAOVehicleVersionLookup versionLookup = new DAOVehicleVersionLookup();
            Integer modelId = versionLookup.getModelIdByVersionId(versionId);
            if (modelId == null) {
                System.err.println("Cannot find model for version " + versionId);
                return -1;
            }

            // Compute unit price for this dealer
            java.math.BigDecimal unitPrice = purchaseOrderDetailDAO.computeUnitPrice(versionId, dealerID);
            if (unitPrice == null) unitPrice = java.math.BigDecimal.ZERO;
            java.math.BigDecimal totalAmount = unitPrice.multiply(java.math.BigDecimal.valueOf(shortageQty));

            // Create purchase order
            DTOPurchaseOrder order = new DTOPurchaseOrder();
            DTODealer dealer = new DTODealer();
            dealer.setDealerID(dealerID);
            order.setDealer(dealer);
            
            DTODealerStaff staff = new DTODealerStaff();
            staff.setStaffID(staffID);
            order.setStaff(staff);
            
            order.setStatus(PurchaseOrderStatus.REQUESTED);
            order.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
            order.setTotalAmount(totalAmount);
            order.setEvmID(1);

            // Insert purchase order and get ID
            int poId = purchaseOrderDAO.insertPurchaseOrder(order);
            if (poId <= 0) {
                System.err.println("Failed to create purchase order");
                return -1;
            }

            // Insert detail using the correct method
            boolean detailSuccess = purchaseOrderDetailDAO.insertOrderDetail(poId, colorId, versionId, shortageQty, unitPrice);
            if (!detailSuccess) {
                System.err.println("Failed to create purchase order detail");
                return -1;
            }

            System.out.println("✅ Auto-created Purchase Order #" + poId + " for " + shortageQty + " vehicles (Version=" + versionId + ", Color=" + colorId + ")");
            return poId;

        } catch (Exception e) {
            System.err.println("Error creating purchase order for shortage: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    // ======================================================
    // ❌ XÓA SALE ORDER
    // ======================================================
    @PostMapping("/delete/{id}")
    public String deleteSaleOrder(@PathVariable int id, RedirectAttributes ra) {
        // remove contracts first to satisfy FK constraint
        DAOSaleContract contractDAO = new DAOSaleContract();
        int removed = contractDAO.deleteContractsBySaleOrderID(id);
        boolean ok = dao.deleteSaleOrder(id);
        if (ok) {
            ra.addFlashAttribute("message", "Sale order deleted ("+id+") - removed "+removed+" contract(s)");
        } else {
            ra.addFlashAttribute("error", "Failed to delete sale order ("+id+") - check contracts or dependencies");
        }
        return "redirect:/saleorder";
    }

    // ======================================================
    // 🚚 UPDATE DELIVERY INFO
    // ======================================================
    @PostMapping("/delivery/update")
    public String updateDeliveryInfo(@RequestParam int saleOrderID,
                                     @RequestParam(required=false) String plannedDate,
                                     @RequestParam(required=false) Integer etaDays,
                                     RedirectAttributes ra) {
        DTOSaleOrder order = dao.getSaleOrderById(saleOrderID);
        if (order == null) { ra.addFlashAttribute("error","Sale order not found"); return "redirect:/saleorder"; }
        java.sql.Timestamp plannedTs = order.getPlannedDeliveryDate();
        if (plannedDate != null && !plannedDate.isBlank()) {
            try { plannedTs = java.sql.Timestamp.valueOf(plannedDate + " 00:00:00"); } catch (Exception e) { ra.addFlashAttribute("error","Invalid planned date format (yyyy-MM-dd)"); return "redirect:/saleorder/detail/"+saleOrderID; }
        }
        Integer newEta = etaDays!=null? etaDays : order.getEtaDays();
        boolean ok = dao.updateDeliveryInfo(saleOrderID, plannedTs, order.getActualDeliveryDate(), newEta);
        ra.addFlashAttribute(ok?"message":"error", ok?"Updated delivery info":"Failed updating delivery info");
        return "redirect:/saleorder/detail/"+saleOrderID;
    }

}
