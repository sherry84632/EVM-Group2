package com.dealermanagementsysstem.project.service;

import java.math.BigDecimal;

/**
 * Central service for stacking discounts in consistent order:
 * 1. Dealer line percent
 * 2. Manufacturer promo (percent OR fixed amount per unit)
 * 3. Base quotation percent
 *
 * All inputs are per-unit amounts where applicable. Fixed manufacturer amount is clamped so result never < 0.
 */
public class DiscountCalculationService {

    public record DiscountBreakdown(
            BigDecimal grossUnit,
            BigDecimal dealerAmount,
            BigDecimal afterDealer,
            BigDecimal manufacturerAmount,
            BigDecimal afterManufacturer,
            BigDecimal baseAmount,
            BigDecimal finalNet
    ) {}

    private double safePct(Double v){ return (v!=null && v>0)? v : 0.0; }

    public DiscountBreakdown calculate(BigDecimal grossUnit,
                                       Double dealerPercent,
                                       Double manufacturerPercent,
                                       BigDecimal manufacturerFixedAmount,
                                       Double basePercent){
        if (grossUnit == null) grossUnit = BigDecimal.ZERO;
        double dealerPct = safePct(dealerPercent);
        double manufPct = safePct(manufacturerPercent);
        double basePct = safePct(basePercent);

        // Dealer amount
        BigDecimal dealerAmount = dealerPct>0? grossUnit.multiply(BigDecimal.valueOf(dealerPct/100.0)) : BigDecimal.ZERO;
        BigDecimal afterDealer = grossUnit.subtract(dealerAmount);
        if (afterDealer.compareTo(BigDecimal.ZERO)<0) afterDealer = BigDecimal.ZERO;

        // Manufacturer amount (choose percent if provided else fixed). If both given: prefer percent.
        BigDecimal manufacturerAmount = BigDecimal.ZERO;
        if (manufPct>0){
            manufacturerAmount = afterDealer.multiply(BigDecimal.valueOf(manufPct/100.0));
        } else if (manufacturerFixedAmount != null && manufacturerFixedAmount.compareTo(BigDecimal.ZERO) > 0){
            // treat fixed amount as per-unit discount, clamp
            manufacturerAmount = manufacturerFixedAmount.min(afterDealer);
        }
        BigDecimal afterManufacturer = afterDealer.subtract(manufacturerAmount);
        if (afterManufacturer.compareTo(BigDecimal.ZERO)<0) afterManufacturer = BigDecimal.ZERO;

        // Base quotation discount
        BigDecimal baseAmount = basePct>0? afterManufacturer.multiply(BigDecimal.valueOf(basePct/100.0)) : BigDecimal.ZERO;
        BigDecimal finalNet = afterManufacturer.subtract(baseAmount);
        if (finalNet.compareTo(BigDecimal.ZERO)<0) finalNet = BigDecimal.ZERO;

        return new DiscountBreakdown(grossUnit, dealerAmount, afterDealer, manufacturerAmount, afterManufacturer, baseAmount, finalNet);
    }
}

