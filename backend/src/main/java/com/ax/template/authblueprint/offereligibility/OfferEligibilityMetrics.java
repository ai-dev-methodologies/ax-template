package com.ax.template.authblueprint.offereligibility;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality metrics for offer-eligibility. The applied/not-applied decision and its
 * reason ARE recorded here as categorical labels (enum names only — never offer ids or customer
 * ids), so the decision is observable without unbounded cardinality.
 */
@Component
public class OfferEligibilityMetrics {

    private final MeterRegistry registry;

    public OfferEligibilityMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param outcome {@code applied} | {@code not_applied}; @param reason an {@link EligibilityReason} name. */
    public void recordEvaluation(String outcome, String reason) {
        Counter.builder("offer_eligibility_evaluation_total")
            .tag("outcome", outcome)
            .tag("reason", reason)
            .register(registry)
            .increment();
    }

    public void recordOfferCreated() {
        Counter.builder("offer_eligibility_offer_created_total")
            .register(registry)
            .increment();
    }
}
