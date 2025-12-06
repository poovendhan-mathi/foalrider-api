package com.foalrider.shared.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility class for price calculations.
 */
public final class PriceUtils {

    private static final int DECIMAL_PLACES = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private PriceUtils() {
        // Prevent instantiation
    }

    /**
     * Round a price to 2 decimal places.
     */
    public static BigDecimal round(BigDecimal price) {
        if (price == null) {
            return BigDecimal.ZERO;
        }
        return price.setScale(DECIMAL_PLACES, ROUNDING_MODE);
    }

    /**
     * Calculate percentage discount.
     * 
     * @param originalPrice The original price
     * @param discountPercent The discount percentage (e.g., 20 for 20%)
     * @return The discount amount
     */
    public static BigDecimal calculatePercentageDiscount(BigDecimal originalPrice, BigDecimal discountPercent) {
        if (originalPrice == null || discountPercent == null) {
            return BigDecimal.ZERO;
        }
        return round(originalPrice.multiply(discountPercent).divide(new BigDecimal("100"), DECIMAL_PLACES, ROUNDING_MODE));
    }

    /**
     * Calculate the price after percentage discount.
     */
    public static BigDecimal applyPercentageDiscount(BigDecimal originalPrice, BigDecimal discountPercent) {
        BigDecimal discount = calculatePercentageDiscount(originalPrice, discountPercent);
        return round(originalPrice.subtract(discount));
    }

    /**
     * Calculate the price after fixed discount.
     */
    public static BigDecimal applyFixedDiscount(BigDecimal originalPrice, BigDecimal discountAmount) {
        if (originalPrice == null || discountAmount == null) {
            return originalPrice;
        }
        BigDecimal result = originalPrice.subtract(discountAmount);
        return result.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : round(result);
    }

    /**
     * Calculate total for quantity.
     */
    public static BigDecimal calculateTotal(BigDecimal unitPrice, int quantity) {
        if (unitPrice == null || quantity <= 0) {
            return BigDecimal.ZERO;
        }
        return round(unitPrice.multiply(new BigDecimal(quantity)));
    }

    /**
     * Calculate discount percentage between two prices.
     */
    public static BigDecimal calculateDiscountPercentage(BigDecimal originalPrice, BigDecimal salePrice) {
        if (originalPrice == null || salePrice == null || originalPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal discount = originalPrice.subtract(salePrice);
        return round(discount.multiply(new BigDecimal("100")).divide(originalPrice, DECIMAL_PLACES, ROUNDING_MODE));
    }

    /**
     * Format price to cents (for Stripe).
     */
    public static long toCents(BigDecimal price) {
        if (price == null) {
            return 0;
        }
        return price.multiply(new BigDecimal("100")).longValue();
    }

    /**
     * Convert cents to price (from Stripe).
     */
    public static BigDecimal fromCents(long cents) {
        return new BigDecimal(cents).divide(new BigDecimal("100"), DECIMAL_PLACES, ROUNDING_MODE);
    }

    /**
     * Check if price is valid (positive).
     */
    public static boolean isValidPrice(BigDecimal price) {
        return price != null && price.compareTo(BigDecimal.ZERO) >= 0;
    }
}
