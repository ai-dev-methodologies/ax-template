package com.ax.template.authblueprint.consumerproof;

import java.math.BigDecimal;

// VIOLATING — money_boundary_seam_guard
// Raw single-arg BigDecimal.valueOf on a long-minor money getter. The value
// stays in MINOR units while the payment edge reads a BigDecimal as MAJOR units
// — a silent 100x over-charge for 2-decimal currencies.
public class InvoiceCalculator {

    public BigDecimal toMajor(Order order) {
        return BigDecimal.valueOf(order.getTotalAmount());
    }
}
