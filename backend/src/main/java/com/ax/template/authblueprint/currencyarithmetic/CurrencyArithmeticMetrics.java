package com.ax.template.authblueprint.currencyarithmetic;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality metrics for currency-arithmetic. The operation outcome is a categorical label
 * from a fixed small set (never a ledger id, currency, or amount), so the arithmetic is observable
 * without unbounded cardinality.
 */
@Component
public class CurrencyArithmeticMetrics {

    private final MeterRegistry registry;

    public CurrencyArithmeticMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param outcome {@code added} | {@code subtracted} | {@code converted}. */
    public void recordOperation(String outcome) {
        Counter.builder("currency_arithmetic_operation_total")
            .tag("outcome", outcome)
            .register(registry)
            .increment();
    }
}
