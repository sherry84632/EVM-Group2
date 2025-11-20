package com.dealermanagementsysstem.project.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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

    /**
     * Compact currency formatting for very large numbers to prevent UI overflow.
     * Examples: 1,234 -> $1,234 | 45,600,000 -> $45.6M | 9,000,000,000 -> $9B
     */
    public String formatCompactCurrency(Number value) {
        if (value == null) return "$0";
        double v = value.doubleValue();
        boolean neg = v < 0;
        double abs = Math.abs(v);
        String suffix;
        double display;
        if (abs >= 1_000_000_000) { // Billions
            display = abs / 1_000_000_000d;
            suffix = "B";
        } else if (abs >= 1_000_000) { // Millions
            display = abs / 1_000_000d;
            suffix = "M";
        } else if (abs >= 1_000) { // Thousands normal formatting
            return (neg ? "-" : "") + formatCurrency(abs);
        } else {
            return (neg ? "-" : "") + formatCurrency(abs);
        }
        // One decimal (if <10B show one decimal, else integer). Use long threshold literal safely.
        long bigThreshold = 10_000_000_000L; // 10B
        String pattern = abs >= bigThreshold ? "%.0f" : "%.1f";
        String formatted = String.format(Locale.US, pattern, display);
        if (formatted.endsWith(".0")) formatted = formatted.substring(0, formatted.length() - 2);
        return (neg ? "-$" : "$") + formatted + suffix;
    }
}
