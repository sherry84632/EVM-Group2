package com.dealermanagementsysstem.project.util;

import java.math.BigDecimal;

/**
 * Discount utility to centralize price computation logic.
 */
public final class DiscountUtil {
    private DiscountUtil() {}

    /**
     * Apply percentage discount to base price.
     * @param base base price (nullable -> treated as ZERO)
     * @param percent discount percent (nullable or <=0 -> no discount)
     * @return final price after discount (never null)
     */
    public static BigDecimal applyPercent(BigDecimal base, Double percent) {
        if (base == null) base = BigDecimal.ZERO;
        if (percent == null || percent <= 0) return base;
        BigDecimal discount = base.multiply(BigDecimal.valueOf(percent / 100.0));
        return base.subtract(discount);
    }
}

