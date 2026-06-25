package com.ax.template.authblueprint.commercepromotion;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality metrics for the promotion engine. Tags use only categorical values
 * (never offer ids or order refs) to prevent cardinality explosion.
 */
@Component
public class PromotionMetrics {

    private final MeterRegistry registry;

    public PromotionMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordApply(String result) {
        Counter.builder("promo_apply_total")
            .tag("result", result)
            .register(registry)
            .increment();
    }

    public void recordRedeem(String result) {
        Counter.builder("promo_redeem_total")
            .tag("result", result)
            .register(registry)
            .increment();
    }
}
