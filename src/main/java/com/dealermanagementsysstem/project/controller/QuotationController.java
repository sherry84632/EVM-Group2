package com.dealermanagementsysstem.project.controller;

import com.dealermanagementsysstem.project.Model.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/quotation")
public class QuotationController {

    @Autowired
    private DAOQuotation dao; // use Spring managed bean instead of manual instantiation
    @Autowired
    private DAODealer daoDealer;
    @Autowired
    private DAOAccount daoAccount;
    @Autowired
    private DAODealerPriceAdjustment daoDealerPriceAdjustment;
    @Autowired
    private DAOVehicle vehicleDAO; // new injection for multi-select

    private static final Logger log = LoggerFactory.getLogger(QuotationController.class);

    /**
     * Helper method to get dealer ID from current logged-in user
     * Returns null if user is not associated with a dealer (EVM/Admin)
     */
    private Integer getDealerIdFromSession() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null) {
                log.warn(" No authentication found");
                return null;
            }

            String email = auth.getName();
            if (email == null) {
                log.warn(" No email found in authentication");
                return null;
            }

            Integer dealerId = daoAccount.getDealerIdByEmail(email);
            if (dealerId == null) {
                log.info("️ No dealer found for email: {} (likely EVM/Admin user)", email);
            } else {
                log.debug(" Found dealerId={} for email={}", dealerId, email);
            }
            return dealerId;
        } catch (Exception e) {
            log.error(" Error getting dealer ID from session", e);
            return null;
        }
    }

    //  Hiển thị form báo giá
    @GetMapping("/new")
    public String showQuotationForm(
            @RequestParam("vehicleId") Integer vehicleId,
            HttpSession session,
            Model model
    ) {
    log.debug("Open quotation form VehicleID={}", vehicleId);

        //  Lấy thông tin xe
    log.trace("Fetching vehicle ID={}", vehicleId);
        DTOVehicle vehicle = dao.getVehicleById(vehicleId);
        if (vehicle == null) {
            log.warn("Vehicle not found ID={}", vehicleId);
            model.addAttribute("error", "Vehicle not found for ID: " + vehicleId + ". Please check and try again.");
            return "dealerPage/errorPage";
        }
        log.debug("Vehicle found ID={}", vehicle.getVehicleID());

        //  Lấy thông tin dealer từ session - USE RELIABLE METHOD
        System.out.println("\n========== DEALER LOADING DEBUG ==========");
        System.out.println(" Method: showQuotationForm() for VehicleID=" + vehicleId);

        // Get authentication info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = (auth != null && auth.isAuthenticated()) ? auth.getName() : "NOT_AUTHENTICATED";
        System.out.println(" Current User Email: " + currentUserEmail);

        Integer dealerId = getDealerIdFromSession();
        System.out.println(" Dealer ID from getDealerIdFromSession(): " + (dealerId != null ? dealerId : "NULL"));

        DTODealer dealer = null;
        if (dealerId != null && dealerId > 0) {
            dealer = dao.getDealerByID(dealerId);
            if (dealer != null) {
                System.out.println(" SUCCESS: Loaded dealer from DB");
                System.out.println("   ├─ Dealer ID: " + dealer.getDealerID());
                System.out.println("   ├─ Dealer Name: " + dealer.getDealerName());
                System.out.println("   └─ Will filter customers by this dealer");
                // load active discounts for the dealer if page needs them later
                model.addAttribute("activeDiscounts", daoDealerPriceAdjustment.getActiveDiscountsByDealer(dealer.getDealerID()));
            } else {
                System.out.println(" ERROR: Dealer not found in DB for ID=" + dealerId);
                System.out.println("   └─ This should NOT happen! Check Dealer table.");
            }
        } else {
            System.out.println(" WARNING: No dealer ID found in session");
            System.out.println("   ├─ User email: " + currentUserEmail);
            System.out.println("   ├─ Likely Admin/EVM user (no dealer assigned)");
            System.out.println("   └─ Will load ALL customers (no filter)");
            // No dealer in session: load dealer list so user can pick in the form instead of redirecting to login
            try {
                List<DTODealer> dealerList = daoDealer.getAllDealers();
                model.addAttribute("dealerList", dealerList);
                log.info("No dealer in session. Providing dealerList size={}", dealerList != null ? dealerList.size() : 0);
            } catch (Exception ex) {
                log.error("Failed to load dealer list", ex);
            }
        }
        System.out.println("==========================================\n");

        // Ngày tạo báo giá
        Timestamp createdAt = Timestamp.valueOf(LocalDateTime.now());

        //  Load customers for selection - FILTERED BY DEALER
        try {
            DAOCustomer customerDAO = new DAOCustomer();
            List<DTOCustomer> customerList;

            //  DEBUG: Log dealer information
            System.out.println("\n========== CUSTOMER LOADING DEBUG ==========");
            System.out.println(" Loading customers for quotation form");
            System.out.println(" Dealer object status: " + (dealer != null ? " EXISTS" : " NULL"));

            if (dealer != null) {
                System.out.println("   ├─ Dealer ID: " + dealer.getDealerID());
                System.out.println("   └─ Dealer Name: " + dealer.getDealerName());
            }

            //  Filter customers by dealer if dealer is logged in
            if (dealer != null && dealer.getDealerID() > 0) {
                int targetDealerId = dealer.getDealerID();
                System.out.println("\n SQL Query: SELECT * FROM Customer WHERE DealerID = " + targetDealerId);

                customerList = customerDAO.getCustomersByDealerId(targetDealerId);

                System.out.println(" FILTERED RESULT:");
                System.out.println("   ├─ Found " + customerList.size() + " customers for DealerID=" + targetDealerId);

                if (!customerList.isEmpty()) {
                    System.out.println("   ├─ Sample customers:");
                    int sampleCount = Math.min(3, customerList.size());
                    for (int i = 0; i < sampleCount; i++) {
                        DTOCustomer c = customerList.get(i);
                        System.out.println("   │  " + (i+1) + ". " + c.getFullName() + " (ID=" + c.getCustomerID() + ")");
                    }
                    if (customerList.size() > 3) {
                        System.out.println("   │  ... and " + (customerList.size() - 3) + " more");
                    }
                } else {
                    System.out.println("   └─  NO CUSTOMERS FOUND for this dealer!");
                    System.out.println("      → Check if Customer table has records with DealerID=" + targetDealerId);
                }

                log.debug("Loaded customers for DealerID={}, count={}", targetDealerId, customerList.size());
            } else {
                // Admin/EVM: show all customers
                System.out.println(" SQL Query: SELECT * FROM Customer (no WHERE clause)");

                customerList = customerDAO.getAllCustomers();

                System.out.println("️ NO FILTER APPLIED:");
                System.out.println("   ├─ Loaded ALL " + customerList.size() + " customers from database");
                System.out.println("   ├─ Reason: dealer is " + (dealer == null ? "NULL" : "ID=" + dealer.getDealerID()));
                System.out.println("   └─ This is normal for Admin/EVM users");

                if (!customerList.isEmpty() && dealer == null) {
                    System.out.println(" IMPORTANT: If you are a DEALER user, this is WRONG!");
                    System.out.println("   → Your account may not be linked to a dealer");
                    System.out.println("   → Check database: Account → DealerStaff → Dealer relationship");
                }

                log.debug("Loaded all customers (no dealer filter), count={}", customerList.size());
            }

            System.out.println("========== END CUSTOMER DEBUG ==========\n");
            model.addAttribute("customerList", customerList);
        } catch (Exception ex) {
            System.err.println(" EXCEPTION while loading customer list:");
            System.err.println("   Error: " + ex.getMessage());
            log.error("Failed loading customer list", ex);
        }

        // ⃣Truyền dữ liệu sang view
        if (dealer != null) {
            model.addAttribute("dealer", dealer);
        }
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("createdAt", createdAt);

        return "dealerPage/quotationForm"; // view name FIXME ensure template exists
    }

    //  CORE FLOW STEP 2: Save quotation to database
    @PostMapping("/save")
    public String saveQuotation(
            @RequestParam("customerID") int customerID,
            @RequestParam("vehicleId") int vehicleId,
            @RequestParam(value = "quantity", defaultValue = "1") int quantity,
            @RequestParam(value = "extraDiscount", required = false) Double extraDiscount,
            @RequestParam(value = "promotionID", required = false) Integer promotionID,
            @RequestParam(value = "dealerID", required = false) Integer dealerIDParam,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        log.debug("Saving quotation customerID={} vehicleId={} quantity={} dealerIDParam={} promotionID={} extraDiscount={}", customerID, vehicleId, quantity, dealerIDParam, promotionID, extraDiscount);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        DAOAccount daoAccount = new DAOAccount();
        DTOAccount account = daoAccount.findAccountByEmail(email);

        try {
            // Resolve dealer ID
            Integer resolvedDealerId = null;
            if (account != null && account.getDealerStaff() != null && account.getDealerStaff().getDealer()!=null) {
                resolvedDealerId = account.getDealerStaff().getDealer().getDealerID();
            } else if (dealerIDParam != null) {
                resolvedDealerId = dealerIDParam;
                log.trace("Using dealer from param dealerId={}", resolvedDealerId);
            }
            if (resolvedDealerId == null) {
                model.addAttribute("error", "Please select a dealer to create quotation.");
                return "dealerPage/quotationForm";
            }
            DTODealer dealer = dao.getDealerByID(resolvedDealerId);
            if (dealer == null) {
                model.addAttribute("error", "Dealer not found.");
                return "dealerPage/quotationForm";
            }

            // Get customer - FILTERED BY DEALER for security
            DAOCustomer customerDAO = new DAOCustomer();
            DTOCustomer customer = customerDAO.getCustomerById(customerID);

            //  Validate customer belongs to this dealer (security check)
            if (customer == null) {
                model.addAttribute("error", "Customer not found.");
                return "dealerPage/quotationForm";
            }
            if (customer.getDealer() != null && customer.getDealer().getDealerID() != resolvedDealerId) {
                log.warn(" Security: Dealer {} attempted to create quotation for customer {} belonging to dealer {}",
                         resolvedDealerId, customerID, customer.getDealer().getDealerID());
                model.addAttribute("error", "Customer does not belong to your dealership.");
                return "dealerPage/quotationForm";
            }

            // Get vehicle
            DTOVehicle vehicle = dao.getVehicleById(vehicleId);
            if (vehicle == null) {
                model.addAttribute("error", "Vehicle not found.");
                return "dealerPage/quotationForm";
            }
            applyDealerPriceToVehicle(vehicle, resolvedDealerId);

            // Determine discount
            double discountPercentValue = 0.0;
            if (promotionID != null) {
                switch (promotionID) {
                    case 1 -> discountPercentValue = 10.0;
                    case 2 -> discountPercentValue = 15.0;
                    case 3 -> discountPercentValue = 5.0;
                    default -> discountPercentValue = 0.0;
                }
            } else if (extraDiscount != null) {
                discountPercentValue = Math.max(0.0, Math.min(80.0, extraDiscount));
            }

            // Pricing
            java.math.BigDecimal unitPriceBD = vehicle.getDealerSellingPriceResolved() != null ? vehicle.getDealerSellingPriceResolved() : java.math.BigDecimal.ZERO;
            double basePrice = unitPriceBD.doubleValue();
            int safeQuantity = Math.max(1, quantity);
            double gross = basePrice * safeQuantity;
            double net = gross * (1 - discountPercentValue/100.0);

            // Build quotation
            DTOQuotation quotation = new DTOQuotation();
            quotation.setCustomer(customer);
            quotation.setDealer(dealer);
            quotation.setQuantity(safeQuantity);
            quotation.setCreatedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
            quotation.setStatus(QuotationStatus.CREATED);
            quotation.setLevelID(dealer.getLevelID() > 0 ? dealer.getLevelID() : 1);
            quotation.setDiscountPercent(discountPercentValue == 0.0 ? null : discountPercentValue);
            quotation.setTotalPrice(net);
            if (account != null && account.getDealerStaff() != null) {
                DTODealerStaff staff = new DTODealerStaff();
                staff.setStaffID(account.getDealerStaff().getStaffID());
                quotation.setStaff(staff);
            }

            int quotationID = dao.insertQuotation(quotation);
            if (quotationID <= 0) {
                model.addAttribute("error", "Failed to create quotation. Please try again.");
                return "dealerPage/quotationForm";
            }
            log.info("Quotation created quotationID={}", quotationID);

            // Insert a single quotation detail representing this vehicle (if version & color present)
            if (vehicle.getVersion() != null && vehicle.getColor() != null) {
                DTOQuotationDetail detail = new DTOQuotationDetail();
                DTOQuotation qRef = new DTOQuotation();
                qRef.setQuotationID(quotationID);
                detail.setQuotation(qRef);
                detail.setUnitPrice(unitPriceBD); // store dealer resolved price per unit
                DTOVehicleVersion versionRef = new DTOVehicleVersion();
                versionRef.setVersionID(vehicle.getVersion().getVersionID());
                detail.setVersion(versionRef);
                DTOVehicleColor colorRef = new DTOVehicleColor();
                colorRef.setColorID(vehicle.getColor().getColorID());
                detail.setColor(colorRef);
                detail.setQuantity(safeQuantity);
                boolean detailInserted = dao.insertQuotationDetail(detail);
                log.info("Quotation detail inserted={} quotationID={}", detailInserted, quotationID);
            } else {
                log.warn("Vehicle missing version or color, skipping detail insertion vehicleId={}", vehicleId);
            }

            dao.recalcQuotationTotal(quotationID); // ensures net stored
            redirectAttributes.addFlashAttribute("message", "Quotation created successfully (ID: " + quotationID + ")");
            return "redirect:/quotation/list";
        } catch (Exception e) {
            log.error("Exception saving quotation vehicleId={}", vehicleId, e);
            model.addAttribute("error", "Error creating quotation: " + e.getMessage());
            return "dealerPage/quotationForm";
        }
    }

    //  CORE FLOW STEP 3: List all quotations (for dealer to review) - FILTERED BY DEALER
    @GetMapping("/list")
    public String listQuotations(Model model) {
        log.info("========== QUOTATION LIST REQUEST ==========");
        try {
            Integer dealerId = getDealerIdFromSession();
            List<DTOQuotation> quotations;
            if (dealerId != null) {
                quotations = dao.getQuotationsByDealerId(dealerId);
                model.addAttribute("dealerFiltered", true);
                model.addAttribute("dealerID", dealerId);
            } else {
                quotations = dao.getAllQuotations();
                model.addAttribute("dealerFiltered", false);
            }
            // Tính toán gross/net cho từng quotation để hiển thị đồng nhất với trang chi tiết
            for (DTOQuotation q : quotations) {
                List<DTOQuotationDetail> details = dao.getQuotationDetails(q.getQuotationID());
                q.setQuotationDetails(details);
                double baseDiscountPct = q.getDiscountPercent() != null ? q.getDiscountPercent() : 0.0;
                double gross = 0.0; double net = 0.0;
                if (details != null) {
                    for (DTOQuotationDetail d : details) {
                        // Set base discount percent lên line để các hàm tính nằm trong DTOQuotationDetail dùng đúng
                        d.setBaseQuotationDiscountPercent(baseDiscountPct);
                        // Gross = subtotal (đã nhân quantity)
                        if (d.getSubtotal() != null) gross += d.getSubtotal().doubleValue();
                        // Net full stack (dealer + manufacturer + base)
                        java.math.BigDecimal lineNet = d.getNetAfterFullStack();
                        net += lineNet.doubleValue();
                    }
                }
                q.setGrossTotal(gross);
                q.setNetTotal(net);
                double effPct = gross > 0 ? (1 - net / gross) * 100.0 : 0.0;
                q.setEffectiveDiscountPercent(effPct);
                // Giữ totalPrice cũ để tránh ảnh hưởng phần khác nhưng nếu chưa đúng thì đồng bộ với gross
                if (gross > 0) { q.setTotalPrice(gross); }
            }
            model.addAttribute("quotations", quotations);
            model.addAttribute("message", "Found " + quotations.size() + " quotations");
            return "dealerPage/quotationList";
        } catch (Exception e) {
            log.error("Error loading quotations", e);
            model.addAttribute("error", "Failed to load quotations: " + e.getMessage());
            return "dealerPage/errorPage";
        }
    }

    //  CORE FLOW STEP 4: View quotation details
    @GetMapping("/detail/{id}")
    public String viewQuotationDetail(@PathVariable("id") int id, Model model) {
        log.debug("Viewing quotation detail id={}", id);

        try {
            DTOQuotation quotation = dao.getQuotationById(id);
            if (quotation == null) {
                model.addAttribute("error", "Quotation not found!");
                return "redirect:/quotation/list";
            }
            if (quotation.getDealer()!=null) {
                dao.normalizeQuotationDealerPrices(id, quotation.getDealer().getDealerID());
                quotation = dao.getQuotationById(id); // reload after normalization
            }

            List<DTOQuotationDetail> details = dao.getQuotationDetails(id);
            quotation.setQuotationDetails(details);

            if (details != null && !details.isEmpty()) {
                double totalPrice = details.stream().mapToDouble(d -> d.getUnitPrice().doubleValue()).sum();
                quotation.setTotalPrice(totalPrice);
            }

            model.addAttribute("quotation", quotation);
            model.addAttribute("details", details);

            // Load active discounts for dealer
            if (quotation.getDealer() != null) {
                var active = daoDealerPriceAdjustment.getActiveDiscountsByDealer(quotation.getDealer().getDealerID());
                log.debug("Active promotions for dealer {} count={}", quotation.getDealer().getDealerID(), active.size());
                model.addAttribute("activeDiscounts", active);

                // Check if any detail has applied discount and find which discount it is
                DTODealerPriceAdjustment appliedDiscount = null;
                for (DTOQuotationDetail d : details) {
                    if (d.getAppliedDealerDiscountPercent() != null && d.getAppliedDealerDiscountPercent() > 0) {
                        // Find matching discount from active discounts
                        for (DTODealerPriceAdjustment disc : active) {
                            if (disc.getDiscountPercent() != null &&
                                Math.abs(disc.getDiscountPercent() - d.getAppliedDealerDiscountPercent()) < 0.01) {
                                // Check if model matches
                                Integer promoModelId = disc.getVehicleModel() != null ? disc.getVehicleModel().getModelID() : null;
                                Integer lineModelId = d.getVersion() != null && d.getVersion().getModel() != null ?
                                                    d.getVersion().getModel().getModelID() : null;
                                if (promoModelId != null && promoModelId.equals(lineModelId)) {
                                    appliedDiscount = disc;
                                    break;
                                }
                            }
                        }
                        break;
                    }
                }

                if (appliedDiscount != null) {
                    model.addAttribute("appliedDealerDiscount", appliedDiscount);
                }

                // Load manufacturer discount policies for this dealer
                DAODiscountPolicy daoPolicy = new DAODiscountPolicy();
                java.util.List<DTODiscountPolicy> allPolicies = daoPolicy.getPoliciesByDealerId(quotation.getDealer().getDealerID());
                java.time.LocalDate today = java.time.LocalDate.now();
                java.util.List<DTODiscountPolicy> manufacturerPolicies = allPolicies.stream()
                        .filter(p -> p.getStatus() == null || p.getStatus() == DiscountPolicyStatus.ACTIVE)
                        .filter(p -> (p.getStartDate() == null || !p.getStartDate().isAfter(today)) &&
                                     (p.getEndDate() == null || !p.getEndDate().isBefore(today)))
                        .toList();
                log.debug("Filtered ACTIVE manufacturer policies dealer={} count={}", quotation.getDealer().getDealerID(), manufacturerPolicies.size());
                model.addAttribute("manufacturerPolicies", manufacturerPolicies);

                // Check if any detail has applied manufacturer policy
                DTODiscountPolicy appliedManufacturerPolicy = null;
                for (DTOQuotationDetail d : details) {
                    if (d.getPromoPolicy() != null) {
                        appliedManufacturerPolicy = d.getPromoPolicy();
                        break;
                    }
                }

                if (appliedManufacturerPolicy != null) {
                    model.addAttribute("appliedManufacturerPolicy", appliedManufacturerPolicy);
                }
            }

            // Check if any line has dealer discount applied
            boolean promotionAppliedFlag = (details != null) && details.stream()
                .anyMatch(d -> d.getAppliedDealerDiscountPercent() != null && d.getAppliedDealerDiscountPercent() > 0);
            // Manufacturer promo flag
            boolean manufacturerPromoApplied = (details != null) && details.stream()
                .anyMatch(d -> d.getPromoDiscountPercent() != null && d.getPromoDiscountPercent() > 0);

            Double appliedLineDiscountPercent = null;
            if (promotionAppliedFlag) {
                appliedLineDiscountPercent = details.stream()
                    .filter(d -> d.getAppliedDealerDiscountPercent() != null && d.getAppliedDealerDiscountPercent() > 0)
                    .map(DTOQuotationDetail::getAppliedDealerDiscountPercent)
                    .findFirst()
                    .orElse(null);
            }

            // Calculate final net: apply line-level then base discount stacking
            double baseDiscountPct = quotation.getDiscountPercent() != null ? quotation.getDiscountPercent() : 0.0;
            double grossAll = details != null ? details.stream().mapToDouble(d -> d.getSubtotal().doubleValue()).sum() : 0.0;
            double afterLine = 0.0;
            if (details != null) {
                for (DTOQuotationDetail d : details) {
                    d.setBaseQuotationDiscountPercent(baseDiscountPct);
                    java.math.BigDecimal lineAfterManufacturer = d.getAfterManufacturer();
                    afterLine += lineAfterManufacturer.doubleValue();
                }
            }
            double finalNetTotal = 0.0;
            if (details != null) {
                for (DTOQuotationDetail d : details) {
                    java.math.BigDecimal net = d.getNetAfterFullStack();
                    d.setFinalNetAfterAll(net); // now includes base discount
                    finalNetTotal += net.doubleValue();
                }
            }
            // Aggregate discount component totals for template (BigDecimal for precision)
            java.math.BigDecimal dealerDiscountSum = java.math.BigDecimal.ZERO;
            java.math.BigDecimal manufacturerDiscountSum = java.math.BigDecimal.ZERO;
            java.math.BigDecimal baseQuotationDiscountSum = java.math.BigDecimal.ZERO;
            if (details != null) {
                for (DTOQuotationDetail d : details) {
                    if (d.getDealerDiscountTotal() != null) dealerDiscountSum = dealerDiscountSum.add(d.getDealerDiscountTotal());
                    if (d.getManufacturerDiscountTotal() != null) manufacturerDiscountSum = manufacturerDiscountSum.add(d.getManufacturerDiscountTotal());
                    if (d.getBaseQuotationDiscountTotal() != null) baseQuotationDiscountSum = baseQuotationDiscountSum.add(d.getBaseQuotationDiscountTotal());
                }
            }
            model.addAttribute("dealerDiscountSum", dealerDiscountSum);
            model.addAttribute("manufacturerDiscountSum", manufacturerDiscountSum);
            model.addAttribute("baseQuotationDiscountSum", baseQuotationDiscountSum);
            // Add previously expected attributes for template (were missing -> null errors)
            model.addAttribute("promotionApplied", promotionAppliedFlag);
            model.addAttribute("manufacturerPromoApplied", manufacturerPromoApplied);
            model.addAttribute("lineLevelGross", grossAll);
            model.addAttribute("lineLevelNet", afterLine);
            model.addAttribute("lineLevelDiscountPercent", appliedLineDiscountPercent);
            model.addAttribute("finalNetTotal", finalNetTotal);
            model.addAttribute("baseDiscountPercent", baseDiscountPct);
            double effectiveCombinedPercent = grossAll > 0 ? (1 - finalNetTotal / grossAll) * 100.0 : 0.0;
            model.addAttribute("finalCombinedDiscountPercent", effectiveCombinedPercent);

            // Add quotation locked status
            boolean quotationLocked = dao.isQuotationLocked(id);
            model.addAttribute("quotationLocked", quotationLocked);
            Integer completedSaleOrderId = dao.getCompletedSaleOrderId(id);
            if (completedSaleOrderId != null) {
                model.addAttribute("completedSaleOrderId", completedSaleOrderId);
            }

            return "dealerPage/quotationDetail";
        } catch (Exception e) {
            log.error("Error loading quotation detail id={}", id, e);
            model.addAttribute("error", "Failed to load quotation details: " + e.getMessage());
            return "redirect:/quotation/list";
        }
    }

    //  CORE FLOW STEP 5: Approve quotation
    @PostMapping("/approve/{id}")
    public String approveQuotation(@PathVariable("id") int id, Model model) {
    log.debug("Approving quotation id={}", id);

        try {
            // First, check if quotation exists and get current status
            DTOQuotation quotation = dao.getQuotationById(id);
            if (quotation == null) {
                log.warn("Quotation not found id={} for approve", id);
                model.addAttribute("error", "Quotation not found!");
                return "redirect:/quotation/list";
            }
            log.trace("Current status={} will update to Accepted", quotation.getStatus());
            
            boolean success = dao.updateQuotationStatus(id, QuotationStatus.APPROVED);
            if (success) {
                log.info("Quotation approved id={}", id);
                model.addAttribute("message", "Quotation approved successfully!");
            } else {
                log.warn("Failed to approve quotation id={}", id);
                model.addAttribute("error", "Failed to approve quotation!");
            }
        } catch (Exception e) {
            log.error("Error approving quotation id={}", id, e);
            model.addAttribute("error", "An error occurred while approving quotation: " + e.getMessage());
        }

        return "redirect:/quotation/list";
    }

    //  CORE FLOW STEP 6: Reject quotation
    @PostMapping("/reject/{id}")
    public String rejectQuotation(@PathVariable("id") int id, Model model) {
    log.debug("Rejecting quotation id={}", id);

        try {
            boolean success = dao.updateQuotationStatus(id, QuotationStatus.REJECTED);
            if (success) {
                log.info("Quotation rejected id={}", id);
                model.addAttribute("message", "Quotation rejected successfully!");
            } else {
                log.warn("Failed to reject quotation id={}", id);
                model.addAttribute("error", "Failed to reject quotation!");
            }
        } catch (Exception e) {
            log.error("Error rejecting quotation id={}", id, e);
            model.addAttribute("error", "An error occurred while rejecting quotation: " + e.getMessage());
        }

        return "redirect:/quotation/list";
    }

    // CORE FLOW: Quotation preview with PDF export and Create Order buttons
    @GetMapping("/preview/{id}")
    public String previewQuotation(@PathVariable("id") int id, Model model) {
    log.debug("Preview quotation id={}", id);

        try {
            DTOQuotation quotation = dao.getQuotationById(id);
            if (quotation == null) {
                model.addAttribute("error", "Quotation not found!");
                return "redirect:/quotation/list";
            }

            // Get quotation details (price information)
            List<DTOQuotationDetail> details = dao.getQuotationDetails(id);
            quotation.setQuotationDetails(details);

            // Calculate total price from details
            if (details != null && !details.isEmpty()) {
                double totalPrice = details.stream()
                    .mapToDouble(detail -> detail.getUnitPrice().doubleValue())
                    .sum();
                quotation.setTotalPrice(totalPrice);
            }

            model.addAttribute("quotation", quotation);
            model.addAttribute("details", details);
            
                log.info("Loaded quotation preview id={}", id);
            return "dealerPage/quotationPreview";
        } catch (Exception e) {
            log.error("Error loading quotation preview id={}", id, e);
            model.addAttribute("error", "Failed to load quotation preview: " + e.getMessage());
            return "redirect:/quotation/list";
        }
    }

    @GetMapping("/quotation/create")
    public String createQuotationForm(Model model) { // removed throws SQLException (not thrown)
        List<DTODealer> dealerList = daoDealer.getAllDealers();
        model.addAttribute("dealerList", dealerList);
        model.addAttribute("quotation", new DTOQuotation());
        return "evmPage/quotation-create";
    }

    //  Add QuotationDetail to existing quotation
    @PostMapping("/detail/add")
    public String addQuotationDetail(
            @RequestParam("quotationID") int quotationID,
            @RequestParam("versionID") int versionID,
            @RequestParam("colorID") int colorID,
            @RequestParam("unitPrice") double unitPrice,
            Model model
    ) {
        if (dao.isQuotationLocked(quotationID)) { model.addAttribute("error","Quotation locked; cannot add line."); return "redirect:/quotation/detail/"+quotationID; }

        log.debug("Adding quotation detail quotationID={} versionID={} colorID={} unitPrice={}",
                 quotationID, versionID, colorID, unitPrice);

        try {
            // Create quotation detail object
            DTOQuotationDetail detail = new DTOQuotationDetail();
            
            // Set quotation relationship
            DTOQuotation quotation = new DTOQuotation();
            quotation.setQuotationID(quotationID);
            detail.setQuotation(quotation);

            // Set version relationship
            DTOVehicleVersion version = new DTOVehicleVersion();
            version.setVersionID(versionID);
            detail.setVersion(version);

            // Set color relationship
            DTOVehicleColor color = new DTOVehicleColor();
            color.setColorID(colorID);
            detail.setColor(color);

            // Set unit price
            detail.setUnitPrice(java.math.BigDecimal.valueOf(unitPrice));

            // Insert quotation detail
            boolean success = dao.insertQuotationDetail(detail);
            
            if (success) {
                log.info("QuotationDetail added quotationID={}", quotationID);
                model.addAttribute("message", "Quotation detail added successfully!");
                
                // Update quotation total amount
                DTOQuotation quotationObj = dao.getQuotationById(quotationID);
                if (quotationObj != null) {
                    List<DTOQuotationDetail> details = dao.getQuotationDetails(quotationID);
                    double totalPrice = details.stream()
                        .mapToDouble(d -> d.getUnitPrice().doubleValue())
                        .sum();
                    dao.updateQuotationTotalAmount(quotationID, totalPrice);
                }
            } else {
                log.warn("Failed to add quotation detail quotationID={}", quotationID);
                model.addAttribute("error", "Failed to add quotation detail!");
            }

        } catch (Exception e) {
            log.error("Error adding quotation detail quotationID={}", quotationID, e);
            model.addAttribute("error", "An error occurred while adding quotation detail: " + e.getMessage());
        }

        return "redirect:/quotation/detail/" + quotationID;
    }

    //  Update QuotationDetail
    @PostMapping("/detail/update")
    public String updateQuotationDetail(
            @RequestParam("quotationDetailID") int quotationDetailID,
            @RequestParam("versionID") int versionID,
            @RequestParam("colorID") int colorID,
            @RequestParam("unitPrice") double unitPrice,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        log.debug("Updating quotation detail id={} versionID={} colorID={} unitPrice={}", 
                 quotationDetailID, versionID, colorID, unitPrice);

        try {
            // Create quotation detail object
            DTOQuotationDetail detail = new DTOQuotationDetail();
            detail.setQuotationDetailID(quotationDetailID);

            // Set version relationship
            DTOVehicleVersion version = new DTOVehicleVersion();
            version.setVersionID(versionID);
            detail.setVersion(version);

            // Set color relationship
            DTOVehicleColor color = new DTOVehicleColor();
            color.setColorID(colorID);
            detail.setColor(color);

            // Set unit price
            detail.setUnitPrice(java.math.BigDecimal.valueOf(unitPrice));

            // Update quotation detail
            boolean success = dao.updateQuotationDetail(detail);
            
            if (success) {
                log.info("QuotationDetail updated id={}", quotationDetailID);
                redirectAttributes.addFlashAttribute("message", "Quotation detail updated successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Failed to update quotation detail!");
            }

        } catch (Exception e) {
            log.error("Error updating quotation detail id={}", quotationDetailID, e);
            model.addAttribute("error", "An error occurred while updating quotation detail: " + e.getMessage());
        }

        return "redirect:/quotation/list";
    }

    //  Delete QuotationDetail
    @PostMapping("/detail/delete")
    public String deleteQuotationDetail(
            @RequestParam("quotationDetailID") int quotationDetailID,
            @RequestParam("quotationID") int quotationID,
            Model model
    ) {
        if (dao.isQuotationLocked(quotationID)) { model.addAttribute("error","Quotation locked; cannot delete line."); return "redirect:/quotation/detail/"+quotationID; }

        log.debug("Deleting quotation detail id={} quotationID={}", quotationDetailID, quotationID);

        try {
            boolean success = dao.deleteQuotationDetail(quotationDetailID);
            
            if (success) {
                log.info("QuotationDetail deleted id={}", quotationDetailID);
                model.addAttribute("message", "Quotation detail deleted successfully!");
                
                // Update quotation total amount
                DTOQuotation quotation = dao.getQuotationById(quotationID);
                if (quotation != null) {
                    List<DTOQuotationDetail> details = dao.getQuotationDetails(quotationID);
                    double totalPrice = details.stream()
                        .mapToDouble(d -> d.getUnitPrice().doubleValue())
                        .sum();
                    dao.updateQuotationTotalAmount(quotationID, totalPrice);
                }
            } else {
                log.warn("Failed to delete quotation detail id={}", quotationDetailID);
                model.addAttribute("error", "Failed to delete quotation detail!");
            }

        } catch (Exception e) {
            log.error("Error deleting quotation detail id={}", quotationDetailID, e);
            model.addAttribute("error", "An error occurred while deleting quotation detail: " + e.getMessage());
        }

        return "redirect:/quotation/detail/" + quotationID;
    }

    //  CORE FLOW STEP 7: Add vehicle to existing quotation (AJAX)
    @PostMapping("/addVehicle")
    public String addVehicleToQuotation(
            @RequestParam int vehicleId,
            @RequestParam int customerID,
            @RequestParam(defaultValue = "1") int quantity,
            HttpSession session,
            RedirectAttributes ra
    ) {
        DTOAccount account = resolveSessionAccount(session);
        if (account == null || account.getDealerStaff() == null || account.getDealerStaff().getDealer() == null) {
            ra.addFlashAttribute("error", "Session expired or dealer info missing. Please login again.");
            return "redirect:/getVehicleListToCreateQuotation";
        }
        int dealerID = account.getDealerStaff().getDealer().getDealerID();
        int staffID = account.getDealerStaff().getStaffID();
        DTOVehicle vehicle = dao.getVehicleById(vehicleId);
        if (vehicle == null) {
            ra.addFlashAttribute("error", "Vehicle not found.");
            return "redirect:/vehicleList";
        }
        applyDealerPriceToVehicle(vehicle, dealerID);
        int levelID = 1;
        DTODealer dealer = dao.getDealerByID(dealerID);
        if (dealer != null && dealer.getLevelID() > 0) levelID = dealer.getLevelID();
        int quotationID = dao.createQuotationIfNotExists(dealerID, customerID, staffID, levelID);
        if (quotationID <= 0) { ra.addFlashAttribute("error", "Cannot create quotation."); return "redirect:/vehicleList"; }
        // No per-line discount here: discount only at quotation total level.
        if (vehicle.getVersion() == null || vehicle.getColor() == null) {
            ra.addFlashAttribute("error", "Vehicle missing version or color data.");
            return "redirect:/vehicleList";
        }
        // Skip duplicate
        if (dao.existsQuotationDetail(quotationID, vehicle.getVersion().getVersionID(), vehicle.getColor().getColorID())) {
            ra.addFlashAttribute("message", "Vehicle already in quotation. Quantity unchanged.");
            return "redirect:/quotation/detail/" + quotationID;
        }
        DTOQuotationDetail detail = new DTOQuotationDetail();
        DTOQuotation qRef = new DTOQuotation(); qRef.setQuotationID(quotationID); detail.setQuotation(qRef);
        DTOVehicleVersion vRef = new DTOVehicleVersion(); vRef.setVersionID(vehicle.getVersion().getVersionID()); detail.setVersion(vRef);
        DTOVehicleColor cRef = new DTOVehicleColor(); cRef.setColorID(vehicle.getColor().getColorID()); detail.setColor(cRef);
        java.math.BigDecimal unitPrice = vehicle.getDealerSellingPriceResolved() != null ? vehicle.getDealerSellingPriceResolved() : java.math.BigDecimal.ZERO;
        detail.setUnitPrice(unitPrice);
        detail.setQuantity(Math.max(1, quantity));
        boolean ok = dao.insertQuotationDetail(detail);
        if (ok) { dao.recalcQuotationTotal(quotationID); ra.addFlashAttribute("message", "Added vehicle to quotation #" + quotationID); }
        else { ra.addFlashAttribute("error", "Failed to add vehicle to quotation."); }
        return "redirect:/quotation/detail/" + quotationID;
    }

    @PostMapping("/addVehicles")
    public String addMultipleVehicles(
            @RequestParam List<Integer> vehicleIds,
            @RequestParam int customerID,
            @RequestParam(defaultValue = "1") int quantity,
            HttpSession session,
            RedirectAttributes ra
    ) {
        DTOAccount account = resolveSessionAccount(session);
        if (account == null || account.getDealerStaff() == null || account.getDealerStaff().getDealer() == null) {
            ra.addFlashAttribute("error", "Session expired or dealer info missing.");
            return "redirect:/getVehicleListToCreateQuotation";
        }
        int dealerID = account.getDealerStaff().getDealer().getDealerID();
        int staffID = account.getDealerStaff().getStaffID();
        int levelID = 1;
        DTODealer dealer = dao.getDealerByID(dealerID);
        if (dealer != null && dealer.getLevelID() > 0) levelID = dealer.getLevelID();
        int quotationID = dao.createQuotationIfNotExists(dealerID, customerID, staffID, levelID);
        if (quotationID <= 0) { ra.addFlashAttribute("error", "Cannot create quotation."); return "redirect:/vehicleList"; }
        List<DTOVehicle> vehicles = new java.util.ArrayList<>();
        for (Integer id : vehicleIds) {
            DTOVehicle v = dao.getVehicleById(id);
            if (v != null) vehicles.add(v);
        }
        int added = dao.addMultipleDetails(quotationID, vehicles, quantity);
        if (added > 0) ra.addFlashAttribute("message", "Added " + added + " vehicle(s) to quotation #" + quotationID);
        else ra.addFlashAttribute("error", "No new vehicles added (duplicates or missing data)." );
        return "redirect:/quotation/detail/" + quotationID;
    }

    @PostMapping("/addVehiclesWithQty")
    public String addMultipleVehiclesWithQuantities(
            @RequestParam(name = "vehicleIds") List<Integer> vehicleIds,
            @RequestParam(name = "quantities") List<Integer> quantities,
            @RequestParam(name = "customerID") int customerID,
            HttpSession session,
            RedirectAttributes ra
    ) {
        DTOAccount account = resolveSessionAccount(session);
        if (account == null || account.getDealerStaff() == null || account.getDealerStaff().getDealer() == null) {
            ra.addFlashAttribute("error", "Session expired or dealer info missing.");
            return "redirect:/getVehicleListToCreateQuotation";
        }
        if (vehicleIds == null || vehicleIds.isEmpty()) {
            ra.addFlashAttribute("error", "No vehicles selected.");
            return "redirect:/getVehicleListToCreateQuotation";
        }
        if (quantities == null) quantities = java.util.Collections.emptyList();
        if (quantities.size() != vehicleIds.size()) {
            log.warn("Quantities size {} differs from vehicleIds size {}. Will normalize.", quantities.size(), vehicleIds.size());
        }
        java.util.List<Integer> normalizedQty = new java.util.ArrayList<>();
        for (int i = 0; i < vehicleIds.size(); i++) {
            int q = 1;
            if (i < quantities.size() && quantities.get(i) != null && quantities.get(i) > 0) q = quantities.get(i);
            normalizedQty.add(q);
        }
        int dealerID = account.getDealerStaff().getDealer().getDealerID();
        int staffID = account.getDealerStaff().getStaffID();
        int levelID = 1;
        DTODealer dealer = dao.getDealerByID(dealerID);
        if (dealer != null && dealer.getLevelID() > 0) levelID = dealer.getLevelID();
        int quotationID = dao.createQuotationIfNotExists(dealerID, customerID, staffID, levelID);
        if (quotationID <= 0) {
            ra.addFlashAttribute("error", "Cannot create quotation.");
            return "redirect:/getVehicleListToCreateQuotation";
        }
        java.util.List<DTOVehicle> vehicles = new java.util.ArrayList<>();
        for (Integer id : vehicleIds) {
            DTOVehicle v = dao.getVehicleById(id);
            if (v != null) vehicles.add(v); else log.warn("Vehicle {} not found, skipped", id);
        }
        int added = dao.addMultipleDetailsWithQuantities(quotationID, vehicles, normalizedQty);
        if (added > 0) {
            ra.addFlashAttribute("message", "Added " + added + " vehicle(s) to quotation #" + quotationID);
        } else {
            ra.addFlashAttribute("error", "No new vehicles added (duplicates or missing data).");
        }
        return "redirect:/quotation/detail/" + quotationID;
    }

    //  Update discount percent for a quotation
    @PostMapping("/discount/update")
    public String updateQuotationDiscount(@RequestParam int quotationID,
                                          @RequestParam double discountPercent,
                                          RedirectAttributes ra) {
        if (dao.isQuotationLocked(quotationID)) { ra.addFlashAttribute("error","Quotation locked; cannot change discount."); return "redirect:/quotation/detail/"+quotationID; }
        double clamped = Math.max(0.0, Math.min(80.0, discountPercent));
        boolean ok = dao.updateQuotationDiscount(quotationID, clamped);
        ra.addFlashAttribute(ok ? "message" : "error", ok ? "Updated discount to " + clamped + "%" : "Failed to update discount");
        return "redirect:/quotation/detail/" + quotationID;
    }

    /**
     * Apply dealer discount to quotation line items and SAVE to database
     */
    @PostMapping("/dealer-discount/apply")
    public String applyDealerDiscount(@RequestParam int quotationID,
                                      @RequestParam int discountId,
                                      RedirectAttributes ra) {
        if (dao.isQuotationLocked(quotationID)) {
            ra.addFlashAttribute("error", "Quotation locked; cannot change discount.");
            return "redirect:/quotation/detail/" + quotationID;
        }

        try {
            List<DTOQuotationDetail> details = dao.getQuotationDetails(quotationID);

            if (discountId == 0) {
                // Remove discount
                for (DTOQuotationDetail d : details) {
                    d.setAppliedDealerDiscountPercent(null);
                    d.setAppliedDealerDiscountAmount(null);
                    dao.updateQuotationDetail(d);
                }
                dao.recalcQuotationTotal(quotationID);
                ra.addFlashAttribute("message", "Dealer discount removed successfully");
            } else {
                DTODealerPriceAdjustment discount = daoDealerPriceAdjustment.getDiscountById(discountId);
                if (discount == null) {
                    ra.addFlashAttribute("error", "Invalid discount selected");
                    return "redirect:/quotation/detail/" + quotationID;
                }
                Double percent = discount.getDiscountPercent();
                Double amountDouble = discount.getDiscountAmount();
                java.math.BigDecimal amountBD = amountDouble != null ? java.math.BigDecimal.valueOf(amountDouble) : null;
                boolean hasPercent = percent != null && percent > 0;
                boolean hasAmount = !hasPercent && amountBD != null && amountBD.compareTo(java.math.BigDecimal.ZERO) > 0;
                if (!hasPercent && !hasAmount) {
                    ra.addFlashAttribute("error", "Discount must have either positive percent or amount");
                    return "redirect:/quotation/detail/" + quotationID;
                }
                // Determine scope
                boolean appliesToAll = (discount.getVehicleModel() == null) &&
                        (discount.getApplicableModelIDs() == null || discount.getApplicableModelIDs().isBlank());
                java.util.Set<Integer> applicableModelSet = new java.util.HashSet<>();
                if (!appliesToAll) {
                    if (discount.getVehicleModel() != null && discount.getVehicleModel().getModelID() > 0) {
                        applicableModelSet.add(discount.getVehicleModel().getModelID());
                    }
                    String csv = discount.getApplicableModelIDs();
                    if (csv != null && !csv.isBlank()) {
                        for (String token : csv.split(",")) {
                            token = token.trim();
                            if (!token.isEmpty()) {
                                try { applicableModelSet.add(Integer.parseInt(token)); } catch (NumberFormatException ignored) {}
                            }
                        }
                    }
                }
                boolean anyMatched = false;
                for (DTOQuotationDetail d : details) {
                    Integer lineModelId = null;
                    if (d.getVersion() != null && d.getVersion().getModel() != null) {
                        lineModelId = d.getVersion().getModel().getModelID();
                    }
                    boolean match = appliesToAll || (lineModelId != null && applicableModelSet.contains(lineModelId));
                    if (match) {
                        if (hasPercent) {
                            d.setAppliedDealerDiscountPercent(percent);
                            d.setAppliedDealerDiscountAmount(null);
                        } else { // amount only
                            d.setAppliedDealerDiscountPercent(null);
                            d.setAppliedDealerDiscountAmount(amountBD);
                        }
                        anyMatched = true;
                    } else {
                        d.setAppliedDealerDiscountPercent(null);
                        d.setAppliedDealerDiscountAmount(null);
                    }
                    dao.updateQuotationDetail(d);
                }
                if (anyMatched) {
                    dao.recalcQuotationTotal(quotationID);
                    String scopeLabel = appliesToAll ? "All Models" : ("Models: " + applicableModelSet);
                    String valLabel = hasPercent ? (percent + "%") : ("$" + amountBD);
                    ra.addFlashAttribute("message", "Dealer discount applied: " + discount.getPromotionName() + " (" + valLabel + ") " + scopeLabel);
                } else {
                    ra.addFlashAttribute("error", "Dealer discount does not match any line items");
                }
            }
        } catch (Exception e) {
            log.error("Error applying dealer discount", e);
            ra.addFlashAttribute("error", "Failed to apply dealer discount: " + e.getMessage());
        }

        return "redirect:/quotation/detail/" + quotationID;
    }

    /**
     * Apply manufacturer discount policy to quotation line items and SAVE to database
     */
    @PostMapping("/manufacturer-policy/apply")
    public String applyManufacturerPolicy(@RequestParam int quotationID,
                                         @RequestParam int policyId,
                                         RedirectAttributes ra) {
        if (dao.isQuotationLocked(quotationID)) {
            ra.addFlashAttribute("error", "Quotation locked; cannot change manufacturer policy.");
            return "redirect:/quotation/detail/" + quotationID;
        }

        try {
            List<DTOQuotationDetail> details = dao.getQuotationDetails(quotationID);

            if (policyId == 0) {
                // Remove manufacturer policy
                for (DTOQuotationDetail d : details) {
                    d.setPromoCode(null);
                    d.setPromoDiscountPercent(null);
                    d.setPromoDiscountAmount(null);
                    d.setPromoPolicy(null);
                    dao.updateQuotationDetail(d);
                }
                dao.recalcQuotationTotal(quotationID);
                ra.addFlashAttribute("message", "Manufacturer policy removed successfully");
            } else {
                // Apply manufacturer policy
                DAODiscountPolicy daoPolicy = new DAODiscountPolicy();
                DTODiscountPolicy policy = daoPolicy.getPolicyById(policyId);

                if (policy == null || policy.getDiscountPercent() == null) {
                    ra.addFlashAttribute("error", "Invalid manufacturer policy selected");
                    return "redirect:/quotation/detail/" + quotationID;
                }

                // Check if policy applies to all models or specific models
                String applicableModels = policy.getApplicableToModels();
                boolean applyToAll = (applicableModels == null || applicableModels.trim().isEmpty());
                java.util.List<Integer> modelIds = new java.util.ArrayList<>();

                if (!applyToAll) {
                    // Parse comma-separated model IDs
                    String[] ids = applicableModels.split(",");
                    for (String id : ids) {
                        try {
                            modelIds.add(Integer.parseInt(id.trim()));
                        } catch (NumberFormatException e) {
                            // Skip invalid IDs
                        }
                    }
                }

                double discountPercent = policy.getDiscountPercent() != null ? policy.getDiscountPercent().doubleValue() : 0.0;
                boolean anyMatched = false;

                for (DTOQuotationDetail d : details) {
                    boolean shouldApply = false;

                    if (applyToAll) {
                        shouldApply = true;
                    } else if (d.getVersion() != null && d.getVersion().getModel() != null) {
                        int lineModelId = d.getVersion().getModel().getModelID();
                        shouldApply = modelIds.contains(lineModelId);
                    }

                    if (shouldApply) {
                        d.setPromoCode(policy.getPolicyName());
                        d.setPromoDiscountPercent(discountPercent);
                        d.setPromoDiscountAmount(null); // Using percent, not fixed amount
                        d.setPromoPolicy(policy);
                        dao.updateQuotationDetail(d);
                        anyMatched = true;
                    } else {
                        // Clear manufacturer policy from this line
                        d.setPromoCode(null);
                        d.setPromoDiscountPercent(null);
                        d.setPromoDiscountAmount(null);
                        d.setPromoPolicy(null);
                        dao.updateQuotationDetail(d);
                    }
                }

                if (anyMatched) {
                    dao.recalcQuotationTotal(quotationID);
                    ra.addFlashAttribute("message", "Manufacturer policy applied: " +
                                       policy.getPolicyName() + " (" + discountPercent + "%)");
                } else {
                    ra.addFlashAttribute("error", "Policy does not match any line items");
                }
            }
        } catch (Exception e) {
            log.error("Error applying manufacturer policy", e);
            ra.addFlashAttribute("error", "Failed to apply manufacturer policy: " + e.getMessage());
        }

        return "redirect:/quotation/detail/" + quotationID;
    }

    @PostMapping("/detail/quantity")
    public String updateDetailQuantity(@RequestParam int quotationDetailID,
                                       @RequestParam int quotationID,
                                       @RequestParam int quantity,
                                       RedirectAttributes ra) {
        if (dao.isQuotationLocked(quotationID)) { ra.addFlashAttribute("error","Quotation locked; cannot change quantity."); return "redirect:/quotation/detail/"+quotationID; }
        boolean ok = dao.updateQuotationDetailQuantity(quotationDetailID, quantity);
        if (ok) {
            dao.recalcQuotationTotal(quotationID);
        }
        ra.addFlashAttribute(ok ? "message" : "error", ok ? "Updated line quantity" : "Failed to update line quantity");
        return "redirect:/quotation/detail/" + quotationID;
    }

    @PostMapping("/discount/clear")
    public String clearQuotationDiscount(@RequestParam int quotationID, RedirectAttributes ra) {
        boolean ok = dao.updateQuotationDiscount(quotationID, 0.0); // sets percent to 0
        ra.addFlashAttribute(ok?"message":"error", ok?"Cleared base discount":"Failed to clear discount");
        return "redirect:/quotation/detail/" + quotationID;
    }

    private DTOAccount resolveSessionAccount(HttpSession session) {
        //  FIX: Use correct session attribute name 'loggedInAccount' (not 'user')
        DTOAccount acc = (DTOAccount) session.getAttribute("loggedInAccount");
        if (acc == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                DTOAccount dbAcc = daoAccount.findAccountByEmail(auth.getName());
                if (dbAcc != null) {
                    session.setAttribute("loggedInAccount", dbAcc); //  Fixed attribute name
                    acc = dbAcc;
                    log.debug("Hydrated session user from auth principal={} accountId={}", auth.getName(), dbAcc.getAccountId());
                }
            }
        }
        return acc;
    }

    @GetMapping("/detail/line/{detailId}")
    public String viewSingleQuotationLine(@PathVariable int detailId, Model model) {
        log.debug("Viewing single quotation detail line id={}", detailId);
        DTOQuotationDetail detail = dao.getQuotationDetailById(detailId);
        if (detail == null) {
            model.addAttribute("error", "Quotation line not found");
            return "dealerPage/quotationLineDetail"; // will show message
        }
        // compute subtotal and effective discount
        double unit = detail.getUnitPrice()!=null?detail.getUnitPrice().doubleValue():0.0;
        int qty = Math.max(1, detail.getQuantity());
        double gross = unit * qty;
        Double discPct = detail.getQuotation()!=null?detail.getQuotation().getDiscountPercent():null;
        double discountPercent = discPct!=null?discPct:0.0;
        double net = gross * (1 - discountPercent/100.0);
        model.addAttribute("detail", detail);
        model.addAttribute("gross", gross);
        model.addAttribute("net", net);
        model.addAttribute("discountPercent", discountPercent);
        return "dealerPage/quotationLineDetail";
    }

    //  NEW: Multi-select vehicle quotation creation form
    @GetMapping("/multi")
    public String showMultiQuotationForm(HttpSession session, Model model) {
        DTOAccount account = resolveSessionAccount(session);
        DTODealer dealer = null;
        if (account != null && account.getDealerStaff() != null && account.getDealerStaff().getDealer() != null) {
            dealer = dao.getDealerByID(account.getDealerStaff().getDealer().getDealerID());
            model.addAttribute("activeDiscounts", daoDealerPriceAdjustment.getActiveDiscountsByDealer(dealer.getDealerID()));
        } else {
            try { model.addAttribute("dealerList", daoDealer.getAllDealers()); } catch (Exception ex) { }
        }
        // Vehicles list for selection
        java.util.List<DTOVehicle> vehicles = vehicleDAO.getVehicles();
        model.addAttribute("vehicles", vehicles);

        // Customers list - FILTERED BY DEALER
        try {
            DAOCustomer cDao = new DAOCustomer();
            java.util.List<DTOCustomer> customerList;
            if (dealer != null && dealer.getDealerID() > 0) {
                customerList = cDao.getCustomersByDealerId(dealer.getDealerID());
                log.debug("Loaded customers for multi-quotation, DealerID={}, count={}", dealer.getDealerID(), customerList.size());
            } else {
                customerList = cDao.getAllCustomers();
                log.debug("Loaded all customers for multi-quotation (Admin/EVM), count={}", customerList.size());
            }
            model.addAttribute("customerList", customerList);
        } catch (Exception ex) {
            log.error("Failed loading customer list for multi-quotation", ex);
        }

        if (dealer != null) model.addAttribute("dealer", dealer);
        model.addAttribute("nowTs", java.time.LocalDateTime.now());
        return "dealerPage/quotationCreateMulti";
    }

    @PostMapping("/delete/{id}")
    public String deleteQuotation(@PathVariable int id,
                                   @RequestParam(name="force", required=false, defaultValue="false") boolean force,
                                   RedirectAttributes ra) {
        if (dao.isQuotationLocked(id)) {
            if (force) {
                ra.addFlashAttribute("error","Quotation has COMPLETED sale orders; cannot force delete.");
            } else {
                ra.addFlashAttribute("error","Quotation locked by completed sale order; cannot delete.");
            }
            return "redirect:/quotation/detail/"+id;
        }
        try {
            boolean ok;
            if (force) {
                ok = dao.forceDeleteQuotation(id);
            } else {
                ok = dao.deleteQuotation(id);
            }
            if (ok) {
                ra.addFlashAttribute("message", force?"Force deleted quotation":"Quotation deleted successfully");
            } else {
                // gather refs for diagnostics
                java.util.List<DTOSaleOrder> refs = dao.getSaleOrderRefs(id);
                if (!refs.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Cannot delete. Referenced by sale orders: ");
                    for (DTOSaleOrder so : refs) {
                        sb.append('#').append(so.getSaleOrderID()).append('(').append(so.getStatus()).append(") ");
                    }
                    if (force) sb.append(" | Completed orders block force delete.");
                    ra.addFlashAttribute("error", sb.toString());
                } else {
                    ra.addFlashAttribute("error", "Failed to delete quotation (unknown reason)");
                }
            }
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("sale order")) {
                ra.addFlashAttribute("error", "Cannot delete quotation: " + errorMsg + (force?" (force ignored)":""));
            } else {
                ra.addFlashAttribute("error", "Exception deleting quotation: " + errorMsg);
            }
        }
        return "redirect:/quotation/list";
    }

    @PostMapping("/update")
    public String bulkUpdateQuotation(@RequestParam int quotationID,
                                      @RequestParam(value = "detailIds", required = false) List<Integer> detailIds,
                                      @RequestParam(value = "quantities", required = false) List<Integer> quantities,
                                      @RequestParam(value = "discountPercent", required = false) Double discountPercent,
                                      RedirectAttributes ra) {
        try {
            // Guard: locked quotation
            if (dao.isQuotationLocked(quotationID)) {
                ra.addFlashAttribute("error", "Quotation is locked; cannot update.");
                return "redirect:/quotation/detail/" + quotationID;
            }
            boolean anyQtyChanged = false;
            if (detailIds != null && quantities != null) {
                if (detailIds.size() != quantities.size()) {
                    ra.addFlashAttribute("error", "Mismatched detailIds and quantities length.");
                    return "redirect:/quotation/detail/" + quotationID;
                }
                for (int i = 0; i < detailIds.size(); i++) {
                    int dId = detailIds.get(i);
                    int qty = quantities.get(i) != null ? quantities.get(i) : 1;
                    if (qty < 1) qty = 1;
                    boolean ok = dao.updateQuotationDetailQuantity(dId, qty);
                    if (ok) anyQtyChanged = true; else ra.addFlashAttribute("error", "Failed updating line id=" + dId);
                }
            }
            boolean discountChanged = false;
            if (discountPercent != null) {
                // Validate range 0-80 (business rule from form attributes)
                if (discountPercent < 0 || discountPercent > 80) {
                    ra.addFlashAttribute("error", "Discount percent out of allowed range (0-80). Not applied.");
                } else {
                    // Update discount; method already triggers recalc
                    discountChanged = dao.updateQuotationDiscount(quotationID, discountPercent);
                    if (!discountChanged) ra.addFlashAttribute("error", "Failed updating base discount percent.");
                }
            }
            // Recalc totals once if quantity changed but discount not handled (avoid double recalc when discountChanged)
            if (anyQtyChanged && !discountChanged) {
                dao.recalcQuotationTotal(quotationID);
            }
            if (!anyQtyChanged && !discountChanged) {
                ra.addFlashAttribute("message", "No changes detected.");
            } else {
                String msg = (anyQtyChanged ? "Updated quantities. " : "") + (discountChanged ? "Updated base discount." : "");
                ra.addFlashAttribute("message", msg.trim());
            }
        } catch (Exception ex) {
            log.error("bulkUpdateQuotation failed quotationID={}", quotationID, ex);
            ra.addFlashAttribute("error", "Bulk update failed: " + ex.getMessage());
        }
        return "redirect:/quotation/detail/" + quotationID;
    }

    private void applyDealerPriceToVehicle(DTOVehicle vehicle, Integer dealerId) {
        if (vehicle == null || dealerId == null || dealerId <= 0) return;
        try {
            DAODealerModelPrice dmp = new DAODealerModelPrice();
            Integer modelId = vehicle.getModelID();
            if (modelId != null) {
                java.math.BigDecimal price = dmp.getPrice(dealerId, modelId);
                if (price != null) {
                    vehicle.setDealerSellingPrice(price);
                    // also push into model for templates resolving
                    if (vehicle.getVersion()!=null && vehicle.getVersion().getModel()!=null) {
                        vehicle.getVersion().getModel().setDealerSellingPrice(price);
                    }
                }
            }
        } catch (Exception ignored) { }
    }
}
