package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PricingCalculatorTest {
    private final PricingCalculator calculator = new PricingCalculator();

    @ParameterizedTest
    @CsvSource({
        "10,10,regular,NONE,100.00", "10,11,regular,NONE,104.50",
        "10,9,regular,NONE,90.00", "100,1,vip,NONE,90.00",
        "100,1,wholesale,NONE,85.00", "100,1,unknown,NONE,100.00",
        "100,1,regular,SAVE10,90.00", "100,1,regular,SAVE20PERCENT,80.00",
        "5,1,regular,SAVE10,0.00", "10,1,regular,SAVE10,0.00",
        "1000,1,regular,NONE,1000.00", "1000.01,1,regular,NONE,980.01",
        "999.99,1,regular,NONE,999.99", "-10,1,regular,NONE,0.00",
        "10,-1,regular,NONE,0.00", "10,0,regular,NONE,0.00",
        "0.005,1,regular,NONE,0.01", "0.004,1,regular,NONE,0.00",
        "100,11,vip,SAVE20PERCENT,752.40", "100,11,wholesale,SAVE10,878.25",
        "1200,1,vip,SAVE10,1048.60"
    })
    void characterizesDiscounts(String price, int quantity, String type, String coupon, String expected) {
        assertThat(calculator.calculateOrderTotal(
                List.of(new PricingCalculator.LineItem(new BigDecimal(price), quantity)), type, coupon))
                .isEqualByComparingTo(expected);
    }

    @Test
    void aggregatesLinesAndAcceptsAbsentDiscounts() {
        assertThat(calculator.calculateOrderTotal(List.of(
                new PricingCalculator.LineItem(new BigDecimal("2.50"), 2),
                new PricingCalculator.LineItem(new BigDecimal("3"), 3)), null, null))
                .isEqualByComparingTo("14.00");
        assertThat(calculator.calculateOrderTotal(List.of(), null, null)).isEqualByComparingTo("0.00");
        assertThatThrownBy(() -> calculator.calculateOrderTotal(null, null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
