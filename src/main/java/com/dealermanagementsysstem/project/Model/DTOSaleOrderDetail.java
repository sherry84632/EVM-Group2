package com.dealermanagementsysstem.project.Model;

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
    private BigDecimal price; // final net unit price after all discounts (dealer + promo)

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

    // Transient VIN loaded from inventory join
    private transient String vin;

    @Column(name = "GrossUnitPrice")
    private BigDecimal grossUnitPrice; // original price before dealer & promo discounts

    // === CONSTRUCTORS ===

    public DTOSaleOrderDetail() {
    }

    public DTOSaleOrderDetail(int soDetailID, DTOSaleOrder saleOrder, DTOVehicle vehicle,
                              BigDecimal price, DTODiscountPolicy discountPolicy) {
        this.soDetailID = soDetailID;
        this.saleOrder = saleOrder;
        this.vehicle = vehicle;
        this.price = price;
        this.discountPolicy = discountPolicy;
    }


    // === GETTERS / SETTERS ===

    public DTOSaleOrder getSaleOrder() {
        return saleOrder;
    }

    public void setSaleOrder(DTOSaleOrder saleOrder) {
        this.saleOrder = saleOrder;
    }

    public int getSoDetailID() {
        return soDetailID;
    }

    public void setSoDetailID(int soDetailID) {
        this.soDetailID = soDetailID;
    }

    public DTODiscountPolicy getDiscountPolicy() {
        return discountPolicy;
    }

    public void setDiscountPolicy(DTODiscountPolicy discountPolicy) {
        this.discountPolicy = discountPolicy;
    }


    public DTOVehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(DTOVehicle vehicle) {
        this.vehicle = vehicle;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getQuantity() {
        return quantity != null ? quantity : 1;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public Double getDealerDiscountPercent() {
        return dealerDiscountPercent;
    }

    public void setDealerDiscountPercent(Double dealerDiscountPercent) {
        this.dealerDiscountPercent = dealerDiscountPercent;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public void setPromoCode(String promoCode) {
        this.promoCode = promoCode;
    }

    public Double getPromoDiscountPercent() {
        return promoDiscountPercent;
    }

    public void setPromoDiscountPercent(Double promoDiscountPercent) {
        this.promoDiscountPercent = promoDiscountPercent;
    }

    public BigDecimal getPromoDiscountAmount() {
        return promoDiscountAmount;
    }

    public void setPromoDiscountAmount(BigDecimal promoDiscountAmount) {
        this.promoDiscountAmount = promoDiscountAmount;
    }

    public Integer getPromoPolicyID() {
        return promoPolicyID;
    }

    public void setPromoPolicyID(Integer promoPolicyID) {
        this.promoPolicyID = promoPolicyID;
    }

    public BigDecimal getGrossUnitPrice() { return grossUnitPrice != null ? grossUnitPrice : price; }
    public void setGrossUnitPrice(BigDecimal g) { this.grossUnitPrice = g; }

    // Formatted helpers
    public String getFormattedUnitPrice() {
        return utils.NumberFormatUtil.formatCurrency(price);
    }

    public BigDecimal getLineTotal() {
        BigDecimal p = price != null ? price : BigDecimal.ZERO;
        return p.multiply(BigDecimal.valueOf(getQuantity()));
    }

    public String getFormattedLineTotal() {
        return utils.NumberFormatUtil.formatCurrency(getLineTotal());
    }

    @Transient
    public BigDecimal getDealerDiscountAmountPerUnit() {
        if (dealerDiscountPercent == null || dealerDiscountPercent <= 0) return BigDecimal.ZERO;
        BigDecimal gross = getGrossUnitPrice();
        return gross.multiply(BigDecimal.valueOf(dealerDiscountPercent/100.0));
    }
    @Transient
    public BigDecimal getPriceAfterDealerPerUnit() {
        BigDecimal gross = getGrossUnitPrice();
        return gross.subtract(getDealerDiscountAmountPerUnit());
    }
    @Transient
    public BigDecimal getPromoDiscountAmountPerUnit() {
        BigDecimal afterDealer = getPriceAfterDealerPerUnit();
        if (promoDiscountPercent != null && promoDiscountPercent > 0) {
            return afterDealer.multiply(BigDecimal.valueOf(promoDiscountPercent/100.0));
        }
        if (promoDiscountAmount != null) return promoDiscountAmount.min(afterDealer);
        return BigDecimal.ZERO;
    }
    @Transient
    public BigDecimal getNetUnitPrice() { return price != null ? price : getPriceAfterDealerPerUnit().subtract(getPromoDiscountAmountPerUnit()); }
    @Transient
    public BigDecimal getDealerDiscountTotal() { return getDealerDiscountAmountPerUnit().multiply(BigDecimal.valueOf(getQuantity())); }
    @Transient
    public BigDecimal getPromoDiscountTotal() { return getPromoDiscountAmountPerUnit().multiply(BigDecimal.valueOf(getQuantity())); }
}
