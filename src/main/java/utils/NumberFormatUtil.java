package utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public class NumberFormatUtil {
    private static final DecimalFormat USD_DECIMAL = new DecimalFormat("#,##0"); // no decimals
    static {
        USD_DECIMAL.setRoundingMode(RoundingMode.HALF_UP);
    }
    public static String formatCurrency(BigDecimal value) {
        if (value == null) return "$0";
        BigDecimal rounded = value.setScale(0, RoundingMode.HALF_UP); // round to whole
        return "$" + USD_DECIMAL.format(rounded);
    }
}
