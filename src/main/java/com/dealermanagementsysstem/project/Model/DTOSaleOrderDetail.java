package com.dealermanagementsysstem.project.Model;

import com.dealermanagementsysstem.project.service.DiscountCalculationService;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "SaleOrderDetail")
public class DTOSaleOrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SODetailID")
    private int soDetailID;

    @ManyToOne
    @JoinColumn(name = "SaleOrderID", referencedColumnName = "SaleOrderID")
    private DTOSaleOrder saleOrder;

    @ManyToOne
    @JoinColumn(name = "VehicleID", referencedColumnName = "VehicleID")
    private DTOVehicle vehicle;

    @Column(name = "Price")
    private BigDecimal price; // final net unit price after all discounts (dealer + promo + base quotation)

    // Manufacturer discount policy applied (promo)
    @ManyToOne
    @JoinColumn(name = "PolicyID", referencedColumnName = "PolicyID")
    private DTODiscountPolicy discountPolicy; // Optional: manufacturer promo policy

    // Dealer discount percent applied at quotation (line-level)
    @Column(name = "DealerDiscountPercent")
    private Double dealerDiscountPercent;

    // Manufacturer promo code snapshot
    @Column(name = "PromoCode")
    private String promoCode;

    @Column(name = "PromoDiscountPercent")
    private Double promoDiscountPercent;

    @Column(name = "PromoDiscountAmount")
    private BigDecimal promoDiscountAmount;

    @Column(name = "PromoPolicyID")
    private Integer promoPolicyID; // store in addition to PolicyID for clarity (optional redundancy)

    @Column(name = "Quantity")
    private Integer quantity; // defaults handled in getter

    private transient String vin; // Transient VIN loaded from inventory join

    @Column(name = "GrossUnitPrice")
    private BigDecimal grossUnitPrice; // original price before dealer & promo discounts (and before base quotation discount)

    // ================= NEW TRANSIENT FIELD =================
    // Base quotation discount percent (quotation-level stacking after dealer + manufacturer)
    @Transient
    private Double baseQuotationDiscountPercent; // not persisted to DB (avoid schema change)

    // === CONSTRUCTORS ===
    public DTOSaleOrderDetail() {}
    public DTOSaleOrderDetail(int soDetailID, DTOSaleOrder saleOrder, DTOVehicle vehicle,
                              BigDecimal price, DTODiscountPolicy discountPolicy) {
        this.soDetailID = soDetailID;
        this.saleOrder = saleOrder;
        this.vehicle = vehicle;
        this.price = price;
        this.discountPolicy = discountPolicy;
    }

    // === GETTERS / SETTERS ===
    public DTOSaleOrder getSaleOrder() { return saleOrder; }
    public void setSaleOrder(DTOSaleOrder saleOrder) { this.saleOrder = saleOrder; }

    public int getSoDetailID() { return soDetailID; }
    public void setSoDetailID(int soDetailID) { this.soDetailID = soDetailID; }

    public DTODiscountPolicy getDiscountPolicy() { return discountPolicy; }
    public void setDiscountPolicy(DTODiscountPolicy discountPolicy) { this.discountPolicy = discountPolicy; }

    public DTOVehicle getVehicle() { return vehicle; }
    public void setVehicle(DTOVehicle vehicle) { this.vehicle = vehicle; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getQuantity() { return quantity != null ? quantity : 1; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }

    public Double getDealerDiscountPercent() { return dealerDiscountPercent; }
    public void setDealerDiscountPercent(Double dealerDiscountPercent) { this.dealerDiscountPercent = dealerDiscountPercent; }

    public String getPromoCode() { return promoCode; }
    public void setPromoCode(String promoCode) { this.promoCode = promoCode; }

    public Double getPromoDiscountPercent() { return promoDiscountPercent; }
    public void setPromoDiscountPercent(Double promoDiscountPercent) { this.promoDiscountPercent = promoDiscountPercent; }

    public BigDecimal getPromoDiscountAmount() { return promoDiscountAmount; }
    public void setPromoDiscountAmount(BigDecimal promoDiscountAmount) { this.promoDiscountAmount = promoDiscountAmount; }

    public Integer getPromoPolicyID() { return promoPolicyID; }
    public void setPromoPolicyID(Integer promoPolicyID) { this.promoPolicyID = promoPolicyID; }

    public BigDecimal getGrossUnitPrice() { return grossUnitPrice != null ? grossUnitPrice : price; }
    public void setGrossUnitPrice(BigDecimal g) { this.grossUnitPrice = g; }

    public Double getBaseQuotationDiscountPercent() { return baseQuotationDiscountPercent; }
    public void setBaseQuotationDiscountPercent(Double baseQuotationDiscountPercent) { this.baseQuotationDiscountPercent = baseQuotationDiscountPercent; }

    // ================== DISCOUNT CALCULATION HELPERS ==================
    @Transient
    private DiscountCalculationService.DiscountBreakdown cachedBreakdown;

    private DiscountCalculationService ensureService(){ return new DiscountCalculationService(); }

    private double safePct(Double v){ return (v!=null && v>0)? v : 0.0; }

    @Transient
    private DiscountCalculationService.DiscountBreakdown computeBreakdown(){
        if (cachedBreakdown != null) return cachedBreakdown;
        BigDecimal grossUnit = getGrossUnitPrice()!=null? getGrossUnitPrice(): BigDecimal.ZERO;
        double dealerPct = safePct(dealerDiscountPercent);
        // Fallback: if explicit promo percent missing, try discountPolicy percent
        double manufPct = safePct(promoDiscountPercent);
        if (manufPct == 0.0 && discountPolicy != null && discountPolicy.getDiscountPercent() != null) {
            manufPct = safePct(discountPolicy.getDiscountPercent().doubleValue());
        }
        // Fallback fixed amount from policy if line promo amount absent
        BigDecimal manufFixed = promoDiscountAmount;
        if ((manufFixed == null || manufFixed.compareTo(BigDecimal.ZERO) <= 0) && discountPolicy != null && discountPolicy.getDiscountAmount() != null) {
            manufFixed = discountPolicy.getDiscountAmount();
        }
        double basePct = safePct(baseQuotationDiscountPercent);
        cachedBreakdown = ensureService().calculate(grossUnit, dealerPct, manufPct, manufFixed, basePct);
        return cachedBreakdown;
    }

    // Per-unit amounts via service
    @Transient public BigDecimal getDealerDiscountAmountPerUnit(){ return computeBreakdown().dealerAmount(); }
    @Transient public BigDecimal getPromoDiscountAmountPerUnit(){ return computeBreakdown().manufacturerAmount(); }
    @Transient public BigDecimal getBaseQuotationDiscountAmountPerUnit(){ return computeBreakdown().baseAmount(); }
    @Transient public BigDecimal getNetUnitPrice(){ return computeBreakdown().finalNet(); }

    // Convenience formatted helpers (use NumberFormatUtil)
    @Transient public String getFormattedDealerDiscountPerUnit(){ return utils.NumberFormatUtil.formatCurrency(getDealerDiscountAmountPerUnit()); }
    @Transient public String getFormattedManufacturerDiscountPerUnit(){ return utils.NumberFormatUtil.formatCurrency(getPromoDiscountAmountPerUnit()); }
    @Transient public String getFormattedBaseQuotationDiscountPerUnit(){ return utils.NumberFormatUtil.formatCurrency(getBaseQuotationDiscountAmountPerUnit()); }
    @Transient public String getFormattedDealerDiscountTotal(){ return utils.NumberFormatUtil.formatCurrency(getDealerDiscountTotal()); }
    @Transient public String getFormattedManufacturerDiscountTotal(){ return utils.NumberFormatUtil.formatCurrency(getPromoDiscountTotal()); }
    @Transient public String getFormattedBaseQuotationDiscountTotal(){ return utils.NumberFormatUtil.formatCurrency(getBaseQuotationDiscountTotal()); }
    @Transient public String getFormattedSavedPerUnit(){ return utils.NumberFormatUtil.formatCurrency(getDealerDiscountAmountPerUnit().add(getPromoDiscountAmountPerUnit()).add(getBaseQuotationDiscountAmountPerUnit())); }
    @Transient public String getFormattedSavedTotal(){ return utils.NumberFormatUtil.formatCurrency(getDealerDiscountTotal().add(getPromoDiscountTotal()).add(getBaseQuotationDiscountTotal())); }

    // Totals (unit * quantity)
    @Transient public BigDecimal getDealerDiscountTotal(){ return getDealerDiscountAmountPerUnit().multiply(BigDecimal.valueOf(getQuantity())); }
    @Transient public BigDecimal getPromoDiscountTotal(){ return getPromoDiscountAmountPerUnit().multiply(BigDecimal.valueOf(getQuantity())); }
    @Transient public BigDecimal getBaseQuotationDiscountTotal(){ return getBaseQuotationDiscountAmountPerUnit().multiply(BigDecimal.valueOf(getQuantity())); }
    @Transient public BigDecimal getLineTotal(){ return getNetUnitPrice().multiply(BigDecimal.valueOf(getQuantity())); }

    public String getFormattedUnitPrice() { return utils.NumberFormatUtil.formatCurrency(getNetUnitPrice()); }
    public String getFormattedLineTotal() { return utils.NumberFormatUtil.formatCurrency(getLineTotal()); }
}
