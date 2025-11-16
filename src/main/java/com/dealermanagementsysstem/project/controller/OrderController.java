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
    private final DAODealerInventory inventoryDAO = new DAODealerInventory(); // Thêm DAO Inventory

    // ======================================================
    //  DANH SÁCH TẤT CẢ SALE ORDER
    // ======================================================
    @GetMapping
    public String listSaleOrders(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "from", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate from,
            @RequestParam(value = "to", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate to,
            Model model) {
        List<DTOSaleOrder> all = dao.getAllSaleOrders();
        List<DTOSaleOrder> orders = new java.util.ArrayList<>();

        // Determine logged-in account & dealer (restrict results if dealer role)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth != null ? auth.getName() : null;
        Integer dealerFilterId = null;
        Role role = null;
        if (email != null && !email.isBlank()) {
            DAOAccount daoAccount = new DAOAccount();
            DTOAccount acc = daoAccount.findAccountByEmail(email);
            if (acc != null) {
                role = acc.getRole();
                if (role == Role.DEALER || role == Role.DEALERSTAFF) {
                    // Only allow viewing own dealer's orders
                    if (acc.getDealerStaff() != null && acc.getDealerStaff().getDealer() != null) {
                        dealerFilterId = acc.getDealerStaff().getDealer().getDealerID();
                    }
                }
            }
        }

        for (DTOSaleOrder o : all) {
            boolean ok = true;
            // Dealer scope restriction
            if (dealerFilterId != null) {
                ok = (o.getDealer() != null && o.getDealer().getDealerID() == dealerFilterId);
            }
            // keyword by customer name
            if (ok && keyword != null && !keyword.trim().isEmpty()) {
                String name = (o.getCustomer()!=null && o.getCustomer().getFullName()!=null) ? o.getCustomer().getFullName() : "";
                ok = name.toLowerCase().contains(keyword.trim().toLowerCase());
            }
            // status filter
            if (ok && status != null && !status.isBlank()) {
                ok = (o.getStatus()!=null && o.getStatus().name().equalsIgnoreCase(status));
            }
            // date range filter
            if (ok && (from != null || to != null)) {
                java.time.LocalDate created = o.getCreatedAt()!=null ? o.getCreatedAt().toLocalDateTime().toLocalDate() : null;
                if (created == null) ok = false;
                if (ok && from != null && created.isBefore(from)) ok = false;
                if (ok && to != null && created.isAfter(to)) ok = false;
            }
            if (ok) orders.add(o);
        }

        model.addAttribute("orders", orders);
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("status", status != null ? status : "");
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("dealerScope", dealerFilterId); // optional for UI to show scope
        return "dealerPage/dealerCustomerOrderList";
    }

    // ======================================================
    // ⃣ FORM TẠO SALE ORDER MỚI
    // ======================================================
    @GetMapping("/new")
    public String showCreateForm(Model model, HttpSession session) { // session kept for future enhancements

        //  Lấy thông tin người dùng đăng nhập
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        DAOAccount daoAccount = new DAOAccount();
        DTOAccount account = daoAccount.findAccountByEmail(email);

        if (account == null || account.getDealerStaff() == null) {
            model.addAttribute("error", "Bạn cần đăng nhập bằng tài khoản dealer!");
            return "redirect:/login";
        }

        //  Lấy danh sách quotation đã duyệt cho dealer này
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
    //   XỬ LÝ SUBMIT FORM TẠO SALE ORDER
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
        //  Lấy thông tin tài khoản hiện tại
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        DAOAccount daoAccount = new DAOAccount();
        DTOAccount account = daoAccount.findAccountByEmail(email);

        if (account == null || account.getDealerStaff()== null) {
            model.addAttribute("error", "Tài khoản hiện tại không hợp lệ hoặc chưa đăng nhập!");
            return "redirect:/login";
        }

        int dealerID = account.getDealerStaff().getDealer().getDealerID();

        //  Lấy quotation được chọn
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
        List<ShortageInfo> shortages = new ArrayList<>();

        if (quotation.getQuotationDetails() != null && !quotation.getQuotationDetails().isEmpty()) {
            for (DTOQuotationDetail qd : quotation.getQuotationDetails()) {
                int lineQty = qd.getQuantity();
                Integer versionId = qd.getVersion()!=null? qd.getVersion().getVersionID(): null;
                Integer colorId = qd.getColor()!=null? qd.getColor().getColorID(): null;
                List<Integer> vehicleIds = new ArrayList<>();
                if (versionId != null && colorId != null) {
                    vehicleIds = inventoryDAO.getAvailableVehicleIdsFromInventory(dealerID, versionId, colorId, lineQty);
                    if (vehicleIds.size() < lineQty) {
                        int shortage = lineQty - vehicleIds.size();
                        shortages.add(new ShortageInfo(versionId, colorId, shortage,
                                      qd.getVersion()!=null ? qd.getVersion().getVersionName() : "N/A",
                                      qd.getColor()!=null ? qd.getColor().getColorName() : "N/A"));
                        continue;
                    }
                }
                java.math.BigDecimal unitNet = qd.getFinalNetAfterAll();
                java.math.BigDecimal grossUnit = qd.getUnitPrice()!=null? qd.getUnitPrice(): java.math.BigDecimal.ZERO;
                if (unitNet == null) {
                    // Recompute stacking if not present (dealer + quotation + promo)
                    java.math.BigDecimal afterDealer = grossUnit.multiply(java.math.BigDecimal.valueOf(1 - qd.getAppliedDealerDiscountPercent()/100.0));
                    java.math.BigDecimal afterQuotation = afterDealer.multiply(java.math.BigDecimal.valueOf(1 - quotation.getDiscountPercent()/100.0));
                    unitNet = afterQuotation; // promo fields not persisted yet here
                }
                for (int i=0;i<lineQty;i++) {
                    DTOSaleOrderDetail sod = new DTOSaleOrderDetail();
                    sod.setSaleOrder(order);
                    DTOVehicle veh = new DTOVehicle();
                    veh.setVehicleID(vehicleIds.get(i));
                    sod.setVehicle(veh);
                    sod.setPrice(unitNet);
                    sod.setGrossUnitPrice(grossUnit);
                    sod.setQuantity(1);
                    // Map dealer discount & promo from quotation detail
                    sod.setDealerDiscountPercent(qd.getAppliedDealerDiscountPercent());
                    sod.setPromoCode(qd.getPromoCode());
                    sod.setPromoDiscountPercent(qd.getPromoDiscountPercent());
                    sod.setPromoDiscountAmount(qd.getPromoDiscountAmount());
                    if (qd.getPromoPolicy()!=null) {
                        sod.setPromoPolicyID(qd.getPromoPolicy().getPolicyID());
                        sod.setDiscountPolicy(qd.getPromoPolicy());
                    }
                    details.add(sod);
                    computedTotalQty += 1;
                    computedTotalAmount = computedTotalAmount.add(unitNet);
                }
            }
        } else {
            // Fallback single line
            DTOSaleOrderDetail sod = new DTOSaleOrderDetail();
            if (vehicleId != null) {
                DTOVehicle veh = new DTOVehicle(); veh.setVehicleID(vehicleId); sod.setVehicle(veh);
            }
            int qtyFallback = (quantity != null && quantity > 0) ? quantity : 1;
            java.math.BigDecimal perUnit = java.math.BigDecimal.valueOf(quotation.getTotalPrice())
                    .divide(java.math.BigDecimal.valueOf(Math.max(1, qtyFallback)), java.math.MathContext.DECIMAL64);
            sod.setPrice(perUnit);
            sod.setGrossUnitPrice(perUnit);
            sod.setQuantity(1);
            sod.setDealerDiscountPercent(quotation.getDiscountPercent()); // approximate
            details.add(sod);
            for (int i=0;i<qtyFallback;i++){ computedTotalQty += 1; computedTotalAmount = computedTotalAmount.add(perUnit); }
        }

        // 🔹 XỬ LÝ SHORTAGES - Tạo 1 PurchaseOrder duy nhất cho tất cả xe thiếu
        if (!shortages.isEmpty()) {
            try {
                int poId = createPurchaseOrderForMultipleShortages(dealerID, staffID, shortages);
                if (poId > 0) {
                    StringBuilder msg = new StringBuilder(" Không đủ xe trong kho. Hệ thống đã tự động tạo đơn hàng mua #" + poId + " cho:\n");
                    for (ShortageInfo s : shortages) {
                        msg.append("  • ").append(s.qty).append(" xe ")
                           .append(s.versionName).append(" (").append(s.colorName).append(")\n");
                    }
                    msg.append("Vui lòng chờ EVM xử lý và thử lại sau.");
                    ra.addFlashAttribute("message", msg.toString());
                    ra.addFlashAttribute("statusType", "INFO");
                } else {
                    ra.addFlashAttribute("error", "Không đủ xe trong kho và không thể tạo đơn hàng mua tự động.");
                }
            } catch (Exception e) {
                ra.addFlashAttribute("error", "Không đủ xe trong kho. Lỗi tạo đơn hàng mua: " + e.getMessage());
            }
            return "redirect:/quotation/detail/" + quotationID;
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

            //  RESERVE CÁC XE TRONG INVENTORY
            for (DTOSaleOrderDetail detail : details) {
                if (detail.getVehicle() != null && detail.getVehicle().getVehicleID() != null) {
                    boolean reserved = inventoryDAO.reserveVehicle(detail.getVehicle().getVehicleID());
                    if (!reserved) {
                        System.err.println(" Failed to reserve vehicle ID=" + detail.getVehicle().getVehicleID());
                    }
                }
            }
        }

        if (success) {
            ra.addFlashAttribute("message", " Tạo đơn hàng thành công! Các xe đã được reserve trong kho.");
            return "redirect:/saleorder";
        } else {
            model.addAttribute("error", "Không thể tạo đơn hàng, vui lòng thử lại.");
            return "dealerPage/createSaleOrder";
        }
    }

    // ======================================================
    //   XEM CHI TIẾT SALE ORDER
    // ======================================================
    @GetMapping("/detail/{id}")
    public String viewOrderDetail(@PathVariable("id") int id, Model model) {
        DTOSaleOrder order = dao.getSaleOrderById(id);
        if (order == null) {
            model.addAttribute("error", "Không tìm thấy đơn hàng!");
            return "redirect:/saleorder";
        }

        // Check if current user is EVM role (read-only mode)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isReadOnly = false;
        System.out.println("=== SALE ORDER DETAIL DEBUG ===");
        System.out.println("Order ID: " + id);
        if (auth != null && auth.getName() != null) {
            System.out.println("User email: " + auth.getName());
            DAOAccount daoAccount = new DAOAccount();
            DTOAccount acc = daoAccount.findAccountByEmail(auth.getName());
            if (acc != null) {
                System.out.println("User role: " + acc.getRole());
                if (acc.getRole() == Role.ADMIN || acc.getRole() == Role.EVMSTAFF) {
                    isReadOnly = true;
                    System.out.println("✓ Setting isReadOnly = TRUE (EVM user)");
                } else {
                    System.out.println("✓ Setting isReadOnly = FALSE (Dealer user)");
                }
            } else {
                System.out.println("⚠ Account not found for email: " + auth.getName());
            }
        } else {
            System.out.println("⚠ No authentication found");
        }
        System.out.println("Final isReadOnly value: " + isReadOnly);
        System.out.println("================================");

        // Compute financial breakdown server-side to avoid complex SpEL lambdas
        java.math.BigDecimal grossTotal = java.math.BigDecimal.ZERO;
        java.math.BigDecimal dealerDiscountTotal = java.math.BigDecimal.ZERO;
        java.math.BigDecimal promoDiscountTotal = java.math.BigDecimal.ZERO;
        if (order.getDetail() != null) {
            for (DTOSaleOrderDetail d : order.getDetail()) {
                int qty = d.getQuantity() != null ? d.getQuantity() : 1;
                java.math.BigDecimal grossUnit = d.getGrossUnitPrice() != null ? d.getGrossUnitPrice() : (d.getPrice() != null ? d.getPrice() : java.math.BigDecimal.ZERO);
                grossTotal = grossTotal.add(grossUnit.multiply(java.math.BigDecimal.valueOf(qty)));
                dealerDiscountTotal = dealerDiscountTotal.add(d.getDealerDiscountAmountPerUnit().multiply(java.math.BigDecimal.valueOf(qty)));
                promoDiscountTotal = promoDiscountTotal.add(d.getPromoDiscountAmountPerUnit().multiply(java.math.BigDecimal.valueOf(qty)));
            }
        }
        model.addAttribute("grossTotal", grossTotal);
        model.addAttribute("dealerDiscountTotal", dealerDiscountTotal);
        model.addAttribute("promoDiscountTotal", promoDiscountTotal);
        model.addAttribute("order", order);
        model.addAttribute("isReadOnly", isReadOnly);
        return "dealerPage/dealerCustomerOrderDetail";
    }

    // ======================================================
    //   LẤY CHI TIẾT 1 SALE ORDER DETAIL (DỰA VÀO VehicleID)
    // ======================================================
    @GetMapping("/detail/item/{detailId}")
    @ResponseBody
    public DTOSaleOrderDetail getDetailItem(@PathVariable("detailId") int detailId) {
        // Lấy chi tiết đơn hàng qua DAO
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
        // Check if user has permission to update status (DEALER/DEALERSTAFF only)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            DAOAccount daoAccount = new DAOAccount();
            DTOAccount acc = daoAccount.findAccountByEmail(auth.getName());
            if (acc != null && (acc.getRole() == Role.ADMIN || acc.getRole() == Role.EVMSTAFF)) {
                ra.addFlashAttribute("error", "You do not have permission to update order status. This action is restricted to dealers.");
                return "redirect:/saleorder/detail/" + saleOrderID;
            }
        }

        SaleOrderStatus newStatus = SaleOrderStatus.valueOf(status.toUpperCase());
        DTOSaleOrder order = dao.getSaleOrderById(saleOrderID);

        if (order == null) {
            ra.addFlashAttribute("error", "Không tìm thấy đơn hàng!");
            return "redirect:/saleorder";
        }

        //  XỬ LÝ KHI CANCEL - HOÀN TRẢ XE VỀ INVENTORY
        if (newStatus == SaleOrderStatus.CANCELLED) {
            if (order.getDetail() != null) {
                for (DTOSaleOrderDetail detail : order.getDetail()) {
                    Integer vehicleId = detail.getVehicle() != null ? detail.getVehicle().getVehicleID() : null;
                    if (vehicleId != null) {
                        boolean returned = inventoryDAO.returnVehicleToInventory(vehicleId);
                        if (returned) {
                            System.out.println(" Returned vehicle ID=" + vehicleId + " to inventory (status=AVAILABLE)");
                        } else {
                            System.err.println(" Failed to return vehicle ID=" + vehicleId + " to inventory");
                        }
                    }
                }
            }
        }

        // XỬ LÝ KHI COMPLETED - CHỈ ĐÁNH DẤU XE LÀ SOLD (KHÔNG XÓA ĐỂ GIỮ VIN)
        if (newStatus == SaleOrderStatus.COMPLETED) {
            if (order.getDetail() != null) {
                for (DTOSaleOrderDetail detail : order.getDetail()) {
                    Integer vehicleId = detail.getVehicle() != null ? detail.getVehicle().getVehicleID() : null;
                    if (vehicleId != null) {
                        // CHỈ đánh dấu SOLD, KHÔNG xóa khỏi inventory để giữ VIN cho sale order detail
                        inventoryDAO.markVehicleAsSold(vehicleId);
                        // NOTE: Không gọi removeVehicleByID() vì sẽ mất VIN trong sale order detail
                    }
                }
            }
        }

        boolean success = dao.updateSaleOrderStatus(saleOrderID, newStatus.toString());

        if (success) {
            dao.applyActualDeliveryIfEligible(order);

            String message = switch (newStatus) {
                case CANCELLED -> " Đơn hàng đã bị hủy. Các xe đã được hoàn trả vào kho.";
                case COMPLETED -> " Đơn hàng đã hoàn thành. Các xe đã được đánh dấu là SOLD.";
                default -> " Cập nhật trạng thái thành công: " + status.toUpperCase();
            };

            ra.addFlashAttribute("message", message);
        } else {
            ra.addFlashAttribute("error", "Không thể cập nhật trạng thái đơn hàng!");
        }

        return "redirect:/saleorder/detail/" + saleOrderID;
    }

    // ======================================================
    // 🔧 HELPER: Tạo Purchase Order cho NHIỀU loại xe thiếu (1 PO với nhiều details)
    // ======================================================
    private int createPurchaseOrderForMultipleShortages(int dealerID, int staffID, List<ShortageInfo> shortages) {
        try {
            if (shortages == null || shortages.isEmpty()) {
                return -1;
            }

            // Calculate total amount for all shortages
            java.math.BigDecimal totalAmount = java.math.BigDecimal.ZERO;
            for (ShortageInfo shortage : shortages) {
                java.math.BigDecimal unitPrice = purchaseOrderDetailDAO.computeUnitPrice(shortage.versionId, dealerID);
                if (unitPrice == null) unitPrice = java.math.BigDecimal.ZERO;
                totalAmount = totalAmount.add(unitPrice.multiply(java.math.BigDecimal.valueOf(shortage.qty)));
            }

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
                System.err.println(" Failed to create purchase order for shortages");
                return -1;
            }

            // Insert ALL details
            int successCount = 0;
            for (ShortageInfo shortage : shortages) {
                java.math.BigDecimal unitPrice = purchaseOrderDetailDAO.computeUnitPrice(shortage.versionId, dealerID);
                if (unitPrice == null) unitPrice = java.math.BigDecimal.ZERO;

                boolean detailSuccess = purchaseOrderDetailDAO.insertOrderDetail(
                    poId, shortage.colorId, shortage.versionId, shortage.qty, unitPrice
                );

                if (detailSuccess) {
                    successCount++;
                    System.out.println("   Added detail: " + shortage.qty + " xe " +
                                     shortage.versionName + " (" + shortage.colorName + ")");
                } else {
                    System.err.println("   Failed to add detail for version=" + shortage.versionId);
                }
            }

            System.out.println(" Auto-created Purchase Order #" + poId + " with " +
                             successCount + "/" + shortages.size() + " details");
            return poId;

        } catch (Exception e) {
            System.err.println(" Error creating purchase order for multiple shortages: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    // ======================================================
    // 🔧 HELPER: Tạo Purchase Order cho xe thiếu (DEPRECATED - dùng createPurchaseOrderForMultipleShortages)
    // ======================================================
    @Deprecated
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

            System.out.println(" Auto-created Purchase Order #" + poId + " for " + shortageQty + " vehicles (Version=" + versionId + ", Color=" + colorId + ")");
            return poId;

        } catch (Exception e) {
            System.err.println("Error creating purchase order for shortage: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    // ======================================================
    // XÓA SALE ORDER
    // ======================================================
    @PostMapping("/delete/{id}")
    public String deleteSaleOrder(@PathVariable int id, RedirectAttributes ra) {
        // Check if user has permission to delete (DEALER/DEALERSTAFF only)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            DAOAccount daoAccount = new DAOAccount();
            DTOAccount acc = daoAccount.findAccountByEmail(auth.getName());
            if (acc != null && (acc.getRole() == Role.ADMIN || acc.getRole() == Role.EVMSTAFF)) {
                ra.addFlashAttribute("error", "You do not have permission to delete orders. This action is restricted to dealers.");
                return "redirect:/saleorder";
            }
        }

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
    //  UPDATE DELIVERY INFO
    // ======================================================
    @PostMapping("/delivery/update")
    public String updateDeliveryInfo(@RequestParam int saleOrderID,
                                     @RequestParam(required=false) String plannedDate,
                                     @RequestParam(required=false) Integer etaDays,
                                     RedirectAttributes ra) {
        // Check if user has permission to update delivery (DEALER/DEALERSTAFF only)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            DAOAccount daoAccount = new DAOAccount();
            DTOAccount acc = daoAccount.findAccountByEmail(auth.getName());
            if (acc != null && (acc.getRole() == Role.ADMIN || acc.getRole() == Role.EVMSTAFF)) {
                ra.addFlashAttribute("error", "You do not have permission to update delivery information. This action is restricted to dealers.");
                return "redirect:/saleorder/detail/" + saleOrderID;
            }
        }

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

    // ======================================================
    //  HELPER CLASS: Thông tin về xe thiếu
    // ======================================================
    private static class ShortageInfo {
        int versionId;
        int colorId;
        int qty;
        String versionName;
        String colorName;

        ShortageInfo(int versionId, int colorId, int qty, String versionName, String colorName) {
            this.versionId = versionId;
            this.colorId = colorId;
            this.qty = qty;
            this.versionName = versionName;
            this.colorName = colorName;
        }
    }
}
