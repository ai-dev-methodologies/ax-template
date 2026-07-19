package com.ax.template.authblueprint.consumerproof;

import java.math.BigDecimal;

// CLEAN — routes the minor->major conversion through the canonical seam
// (common/Money.toMajorUnits), which places the decimal point at the currency's
// minor-unit scale. No raw money-getter BigDecimal.valueOf.
public class InvoiceCalculator {

    public BigDecimal toMajor(Order order) {
        return Money.toMajorUnits(order.getTotalAmount(), order.getCurrency());
    }
}
