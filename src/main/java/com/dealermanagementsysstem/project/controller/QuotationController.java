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

    private static final Logger log = LoggerFactory.getLogger(QuotationController.class);

    // ✅ Hiển thị form báo giá
    @GetMapping("/new")
    public String showQuotationForm(
            @RequestParam("vehicleId") Integer vehicleId,
            HttpSession session,
            Model model
    ) {
    log.debug("Open quotation form VehicleID={}", vehicleId);

        // 1️⃣ Lấy thông tin xe
    log.trace("Fetching vehicle ID={}", vehicleId);
        DTOVehicle vehicle = dao.getVehicleById(vehicleId);
        if (vehicle == null) {
            log.warn("Vehicle not found ID={}", vehicleId);
            model.addAttribute("error", "Vehicle not found for ID: " + vehicleId + ". Please check and try again.");
            return "dealerPage/errorPage";
        }
        log.debug("Vehicle found ID={}", vehicle.getVehicleID());

        // 2️⃣ Lấy thông tin dealer từ session (debug)
        DTOAccount account = (DTOAccount) session.getAttribute("user");
    log.trace("Session user username={} dealerId={}", account != null ? account.getUsername() : null, account != null ? account.getDealerStaff().getStaffID() : null);

        DTODealer dealer = null;
        if (account != null && account.getDealerStaff() != null) {
            dealer = dao.getDealerByID(account.getDealerStaff().getStaffID());
            log.debug("Resolved dealer from session dealerName={}", dealer != null ? dealer.getDealerName() : null);
        } else {
            // No dealer in session: load dealer list so user can pick in the form instead of redirecting to login
            try {
                List<DTODealer> dealerList = daoDealer.getAllDealers();
                model.addAttribute("dealerList", dealerList);
                log.info("No dealer in session. Providing dealerList size={}", dealerList != null ? dealerList.size() : 0);
            } catch (Exception ex) {
                log.error("Failed to load dealer list", ex);
            }
        }

        // 3️⃣ Ngày tạo báo giá
        Timestamp createdAt = Timestamp.valueOf(LocalDateTime.now());

        // 4️⃣ Load customers for selection
        try {
            DAOCustomer customerDAO = new DAOCustomer();
            List<DTOCustomer> customerList = customerDAO.getAllCustomers();
            model.addAttribute("customerList", customerList);
            log.debug("Loaded customers count={}", customerList.size());
        } catch (Exception ex) {
            log.error("Failed loading customer list", ex);
        }

        // 5️⃣ Truyền dữ liệu sang view
        if (dealer != null) {
            model.addAttribute("dealer", dealer);
        }
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("createdAt", createdAt);

        return "dealerPage/quotationForm"; // ✅ Tên file HTML của bạn
    }

    // 🔥 CORE FLOW STEP 2: Save quotation to database
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

        DTOAccount account = (DTOAccount) session.getAttribute("user");

        try {
            // Resolve dealer ID
            Integer resolvedDealerId = null;
            if (account != null && account.getDealerStaff() != null) {
                resolvedDealerId = account.getDealerStaff().getStaffID();
                log.trace("Using dealer from session dealerId={}", resolvedDealerId);
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

            // Get customer
            DAOCustomer customerDAO = new DAOCustomer();
            DTOCustomer customer = customerDAO.getAllCustomers().stream()
                    .filter(c -> c.getCustomerID() == customerID)
                    .findFirst()
                    .orElse(null);
            if (customer == null) {
                model.addAttribute("error", "Customer not found.");
                return "dealerPage/quotationForm";
            }

            // Get vehicle
            DTOVehicle vehicle = dao.getVehicleById(vehicleId);
            if (vehicle == null) {
                model.addAttribute("error", "Vehicle not found.");
                return "dealerPage/quotationForm";
            }

            // Determine discount
            double discountPercent = 0.0;
            if (promotionID != null) {
                switch (promotionID) {
                    case 1 -> discountPercent = 10.0;
                    case 2 -> discountPercent = 15.0;
                    case 3 -> discountPercent = 5.0;
                    default -> discountPercent = 0.0;
                }
            } else if (extraDiscount != null) {
                discountPercent = Math.max(0.0, Math.min(50.0, extraDiscount));
            }

            // Pricing
            java.math.BigDecimal basePriceBD = vehicle.getBasePrice() != null ? vehicle.getBasePrice() : java.math.BigDecimal.ZERO;
            double basePrice = basePriceBD.doubleValue();
            if (basePrice <= 0) {
                log.warn("Vehicle basePrice is zero or missing vehicleId={}", vehicleId);
            }
            double unitPrice = basePrice * (1 - discountPercent / 100.0);
            int safeQuantity = Math.max(1, quantity);
            double totalAmount = unitPrice * safeQuantity;

            // Build quotation
            DTOQuotation quotation = new DTOQuotation();
            quotation.setCustomer(customer);
            quotation.setDealer(dealer);
            quotation.setQuantity(safeQuantity);
            quotation.setCreatedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
            quotation.setStatus(QuotationStatus.CREATED);
            quotation.setLevelID(dealer.getLevelID() > 0 ? dealer.getLevelID() : 1);
            quotation.setTotalPrice(totalAmount);
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
                detail.setUnitPrice(java.math.BigDecimal.valueOf(unitPrice));
                DTOVehicleVersion versionRef = new DTOVehicleVersion();
                versionRef.setVersionID(vehicle.getVersion().getVersionID());
                detail.setVersion(versionRef);
                DTOVehicleColor colorRef = new DTOVehicleColor();
                colorRef.setColorID(vehicle.getColor().getColorID());
                detail.setColor(colorRef);
                boolean detailInserted = dao.insertQuotationDetail(detail);
                log.info("Quotation detail inserted={} quotationID={}", detailInserted, quotationID);
            } else {
                log.warn("Vehicle missing version or color, skipping detail insertion vehicleId={}", vehicleId);
            }

            // Update total amount to ensure consistency with detail(s)
            dao.updateQuotationTotalAmount(quotationID, totalAmount);
            redirectAttributes.addFlashAttribute("message", "Quotation created successfully (ID: " + quotationID + ")");
            return "redirect:/quotation/list";
        } catch (Exception e) {
            log.error("Exception saving quotation vehicleId={}", vehicleId, e);
            model.addAttribute("error", "Error creating quotation: " + e.getMessage());
            return "dealerPage/quotationForm";
        }
    }

    // 🔥 CORE FLOW STEP 3: List all quotations (for dealer to review)
    @GetMapping("/list")
    public String listQuotations(Model model) {
    log.debug("Loading quotations list");

        try {
            List<DTOQuotation> quotations = dao.getAllQuotations();
            model.addAttribute("quotations", quotations);
            model.addAttribute("message", "Found " + quotations.size() + " quotations");
            
            log.info("Loaded quotations size={}", quotations.size());
            return "dealerPage/quotationList";
        } catch (Exception e) {
            log.error("Error loading quotations", e);
            model.addAttribute("error", "Failed to load quotations: " + e.getMessage());
            return "dealerPage/errorPage";
        }
    }

    // 🔥 CORE FLOW STEP 4: View quotation details
    @GetMapping("/detail/{id}")
    public String viewQuotationDetail(@PathVariable("id") int id, Model model) {
    log.debug("Viewing quotation detail id={}", id);

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
                log.trace("Calculated quotation total id={} totalPrice={}", id, totalPrice);
            } else {
                log.warn("No quotation details found id={}", id);
            }

            model.addAttribute("quotation", quotation);
            model.addAttribute("details", details);
            
            log.info("Loaded quotation details id={}", id);
            return "dealerPage/quotationDetail";
        } catch (Exception e) {
            log.error("Error loading quotation detail id={}", id, e);
            model.addAttribute("error", "Failed to load quotation details: " + e.getMessage());
            return "redirect:/quotation/list";
        }
    }

    // 🔥 CORE FLOW STEP 5: Approve quotation
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

    // 🔥 CORE FLOW STEP 6: Reject quotation
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

    // 🔥 CORE FLOW: Quotation preview with PDF export and Create Order buttons
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

    // ✅ Add QuotationDetail to existing quotation
    @PostMapping("/detail/add")
    public String addQuotationDetail(
            @RequestParam("quotationID") int quotationID,
            @RequestParam("versionID") int versionID,
            @RequestParam("colorID") int colorID,
            @RequestParam("unitPrice") double unitPrice,
            Model model
    ) {
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

    // ✅ Update QuotationDetail
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

    // ✅ Delete QuotationDetail
    @PostMapping("/detail/delete")
    public String deleteQuotationDetail(
            @RequestParam("quotationDetailID") int quotationDetailID,
            @RequestParam("quotationID") int quotationID,
            Model model
    ) {
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
}
