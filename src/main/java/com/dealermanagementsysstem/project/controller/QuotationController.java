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

import java.sql.SQLException;
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
        System.out.println(" Dealer ID from getDealerIdFromSession(): " + (dealerId != null ? dealerId : "❌ NULL"));

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

                if (customerList.size() > 0) {
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

                if (customerList.size() > 0 && dealer == null) {
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
            ex.printStackTrace();
            log.error("Failed loading customer list", ex);
        }

        // ⃣Truyền dữ liệu sang view
        if (dealer != null) {
            model.addAttribute("dealer", dealer);
        }
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("createdAt", createdAt);

        return "dealerPage/quotationForm"; //  Tên file HTML của bạn
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
            java.math.BigDecimal basePriceBD = vehicle.getBasePrice() != null ? vehicle.getBasePrice() : java.math.BigDecimal.ZERO;
            double basePrice = basePriceBD.doubleValue();
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
                detail.setUnitPrice(basePriceBD); // store base price per unit
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
    public String listQuotations(Model model, HttpSession session) {
        log.info("========== QUOTATION LIST REQUEST ==========");

        try {
            //  Get dealer ID from logged-in user's email
            Integer dealerId = getDealerIdFromSession();

            log.info(" Resolved dealerId from session: {}", dealerId);

            List<DTOQuotation> quotations;

            // Filter by dealer if user is DEALER or DEALERSTAFF
            if (dealerId != null) {
                log.info(" Calling dao.getQuotationsByDealerId({})", dealerId);
                quotations = dao.getQuotationsByDealerId(dealerId);

                // Log each quotation's dealer ID for verification
                log.info(" Retrieved {} quotations:", quotations.size());
                for (DTOQuotation q : quotations) {
                    log.info("   - QuotationID={}, DealerID={}, Customer={}",
                        q.getQuotationID(),
                        q.getDealer() != null ? q.getDealer().getDealerID() : "NULL",
                        q.getCustomer() != null ? q.getCustomer().getFullName() : "NULL");
                }

                model.addAttribute("dealerFiltered", true);
                model.addAttribute("dealerID", dealerId);
                log.info(" Filtered quotations by dealerID={}, size={}", dealerId, quotations.size());
            } else {
                // Admin/EVM - show all quotations
                log.info(" No dealer found - showing all quotations (EVM/Admin)");
                quotations = dao.getAllQuotations();
                model.addAttribute("dealerFiltered", false);
                log.info("Loaded all quotations size={}", quotations.size());
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
            }

            // Check if any line has dealer discount applied
            boolean promotionAppliedFlag = details.stream()
                .anyMatch(d -> d.getAppliedDealerDiscountPercent() != null && d.getAppliedDealerDiscountPercent() > 0);

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
            double grossAll = details.stream().mapToDouble(d -> d.getSubtotal().doubleValue()).sum();
            double afterLine = details.stream().mapToDouble(d -> {
                double sub = d.getSubtotal().doubleValue();
                double lp = d.getAppliedDealerDiscountPercent() != null ? d.getAppliedDealerDiscountPercent() : 0.0;
                return sub * (1 - lp / 100.0);
            }).sum();
            double finalNetTotal = afterLine * (1 - baseDiscountPct / 100.0);

            // Calculate per line final net
            for (DTOQuotationDetail d : details) {
                double lp = d.getAppliedDealerDiscountPercent() != null ? d.getAppliedDealerDiscountPercent() : 0.0;
                double sub = d.getSubtotal().doubleValue();
                double lineAfterLine = sub * (1 - lp / 100.0);
                double lineFinal = lineAfterLine * (1 - baseDiscountPct / 100.0);
                d.setFinalNetAfterAll(java.math.BigDecimal.valueOf(lineFinal));
            }

            // Set model attributes for display
            model.addAttribute("lineLevelGross", grossAll);
            model.addAttribute("lineLevelNet", afterLine);
            if (promotionAppliedFlag && appliedLineDiscountPercent != null) {
                model.addAttribute("lineLevelDiscountPercent", appliedLineDiscountPercent);
            }
            model.addAttribute("promotionApplied", promotionAppliedFlag);
            model.addAttribute("finalNetTotal", finalNetTotal);
            model.addAttribute("baseDiscountPercent", baseDiscountPct);
            model.addAttribute("finalCombinedDiscountPercent", grossAll > 0 ? (1 - finalNetTotal / grossAll) * 100.0 : 0.0);

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
        java.math.BigDecimal basePrice = vehicle.getBasePrice() != null ? vehicle.getBasePrice() : java.math.BigDecimal.ZERO;
        detail.setUnitPrice(basePrice);
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
                    dao.updateQuotationDetail(d);
                }
                // Recalculate quotation total after removing discount
                dao.recalcQuotationTotal(quotationID);
                ra.addFlashAttribute("message", "Dealer discount removed successfully");
            } else {
                // Apply discount
                DTODealerPriceAdjustment discount = daoDealerPriceAdjustment.getDiscountById(discountId);
                if (discount == null || discount.getDiscountPercent() == null) {
                    ra.addFlashAttribute("error", "Invalid discount selected");
                    return "redirect:/quotation/detail/" + quotationID;
                }

                Integer promoModelId = discount.getVehicleModel() != null ?
                                     discount.getVehicleModel().getModelID() : null;

                if (promoModelId == null) {
                    ra.addFlashAttribute("error", "Discount missing model reference");
                    return "redirect:/quotation/detail/" + quotationID;
                }

                double discountPercent = discount.getDiscountPercent();
                boolean anyMatched = false;

                for (DTOQuotationDetail d : details) {
                    if (d.getVersion() != null &&
                        d.getVersion().getModel() != null &&
                        d.getVersion().getModel().getModelID() == promoModelId) {
                        d.setAppliedDealerDiscountPercent(discountPercent);
                        dao.updateQuotationDetail(d);
                        anyMatched = true;
                    } else {
                        d.setAppliedDealerDiscountPercent(null);
                        dao.updateQuotationDetail(d);
                    }
                }

                if (anyMatched) {
                    // Recalculate quotation total after applying discount
                    dao.recalcQuotationTotal(quotationID);
                    ra.addFlashAttribute("message", "Dealer discount applied: " +
                                       discount.getPromotionName() + " (" + discountPercent + "%)");
                } else {
                    ra.addFlashAttribute("error", "Promotion model does not match any line items");
                }
            }
        } catch (Exception e) {
            log.error("Error applying dealer discount", e);
            ra.addFlashAttribute("error", "Failed to apply dealer discount: " + e.getMessage());
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
    public String deleteQuotation(@PathVariable int id, RedirectAttributes ra) {
        if (dao.isQuotationLocked(id)) { ra.addFlashAttribute("error","Quotation locked by completed sale order; cannot delete."); return "redirect:/quotation/detail/"+id; }
        try {
            boolean ok = dao.deleteQuotation(id);
            ra.addFlashAttribute(ok?"message":"error", ok?"Quotation deleted successfully":"Failed to delete quotation");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Exception deleting quotation: " + e.getMessage());
        }
        return "redirect:/quotation/list";
    }

    @PostMapping("/update")
    public String batchUpdateQuotation(
            @RequestParam int quotationID,
            @RequestParam(name="discountPercent", required=false) Double discountPercent,
            @RequestParam(name="detailIds", required=false) List<Integer> detailIds,
            @RequestParam(name="unitPrices", required=false) List<Double> unitPrices,
            @RequestParam(name="quantities", required=false) List<Integer> quantities,
            RedirectAttributes ra) {
        if (dao.isQuotationLocked(quotationID)) { ra.addFlashAttribute("error","Quotation locked by completed sale order. Create a new quotation instead."); return "redirect:/quotation/detail/"+quotationID; }
        DTOQuotation q = dao.getQuotationById(quotationID);
        if (q == null) { ra.addFlashAttribute("error","Quotation not found"); return "redirect:/quotation/list"; }
        // Update detail lines
        if (detailIds != null && unitPrices != null && quantities != null) {
            int updated = 0; int len = Math.min(detailIds.size(), Math.min(unitPrices.size(), quantities.size()));
            for (int i=0;i<len;i++) {
                Integer id = detailIds.get(i); Double price = unitPrices.get(i); Integer qty = quantities.get(i);
                if (id == null) continue; java.math.BigDecimal priceBD = price!=null? java.math.BigDecimal.valueOf(Math.max(0, price)) : java.math.BigDecimal.ZERO;
                if (dao.updateQuotationDetailFields(id, priceBD, qty!=null?qty:1)) updated++;
            }
            ra.addFlashAttribute("message", "Updated " + updated + " line(s)");
        }
        // Update base discount only now
        if (discountPercent != null) {
            double clamped = Math.max(0.0, Math.min(80.0, discountPercent));
            dao.updateQuotationDiscount(quotationID, clamped);
        }
        dao.recalcQuotationTotal(quotationID);
        return "redirect:/quotation/detail/" + quotationID;
    }
}
