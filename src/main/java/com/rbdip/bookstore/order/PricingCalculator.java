package com.rbdip.bookstore.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class PricingCalculator {
    private static final int BULK_QUANTITY = 10;
    private static final BigDecimal BULK_RATE = new BigDecimal("0.95");
    private static final BigDecimal VIP_RATE = new BigDecimal("0.9");
    private static final BigDecimal WHOLESALE_RATE = new BigDecimal("0.85");
    private static final BigDecimal COUPON_RATE = new BigDecimal("0.8");
    private static final BigDecimal LARGE_THRESHOLD = new BigDecimal("1000");
    private static final BigDecimal LARGE_RATE = new BigDecimal("0.98");

    public record LineItem(BigDecimal price, int quantity) {
    }

    public BigDecimal calculateOrderTotal(List<LineItem> items, String customerType, String couponCode) {
        BigDecimal total = BigDecimal.ZERO;

        for (LineItem item : items) {
            BigDecimal linePrice = item.price().multiply(BigDecimal.valueOf(item.quantity()));
            if (item.quantity() > BULK_QUANTITY) {
                linePrice = linePrice.multiply(BULK_RATE);
            }
            total = total.add(linePrice);
        }

        if ("vip".equals(customerType)) {
            total = total.multiply(VIP_RATE);
        } else if ("wholesale".equals(customerType)) {
            total = total.multiply(WHOLESALE_RATE);
        }

        if ("SAVE10".equals(couponCode)) {
            total = total.subtract(BigDecimal.TEN);
        } else if ("SAVE20PERCENT".equals(couponCode)) {
            total = total.multiply(COUPON_RATE);
        }

        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }

        if (total.compareTo(LARGE_THRESHOLD) > 0) {
            total = total.multiply(LARGE_RATE);
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }
}
