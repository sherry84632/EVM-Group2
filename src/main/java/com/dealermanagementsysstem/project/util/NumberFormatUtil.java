package com.dealermanagementsysstem.project.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utility class for formatting numbers without decimal places
 */
@Component("numberFormat")
public class NumberFormatUtil {

    /**
     * Format currency as integer (no decimal places)
     * Example: 1234567.89 -> $1,234,568 (rounded)
     */
    public String formatCurrency(Number value) {
        if (value == null) {
            return "$0";
        }

        // Round to nearest integer
        long rounded = Math.round(value.doubleValue());

        // Format with comma separators
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);
        formatter.setGroupingUsed(true);
        formatter.setMaximumFractionDigits(0);
        formatter.setMinimumFractionDigits(0);

        return "$" + formatter.format(rounded);
    }

    /**
     * Format decimal as integer (no decimal places)
     * Example: 1234567.89 -> 1,234,568 (rounded)
     */
    public String formatNumber(Number value) {
        if (value == null) {
            return "0";
        }

        // Round to nearest integer
        long rounded = Math.round(value.doubleValue());

        // Format with comma separators
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);
        formatter.setGroupingUsed(true);
        formatter.setMaximumFractionDigits(0);
        formatter.setMinimumFractionDigits(0);

        return formatter.format(rounded);
    }

    /**
     * Format BigDecimal as integer currency
     */
    public String formatCurrency(BigDecimal value) {
        if (value == null) {
            return "$0";
        }
        return formatCurrency(value.doubleValue());
    }

    /**
     * Format BigDecimal as integer
     */
    public String formatNumber(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return formatNumber(value.doubleValue());
    }
}

