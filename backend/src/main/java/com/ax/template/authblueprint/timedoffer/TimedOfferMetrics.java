package com.ax.template.authblueprint.timedoffer;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code timed_offer_total
 * {op, outcome}}, both fixed enums — never a subject id, candidate, offer id, or actor.
 */
@Component
public class TimedOfferMetrics {

    private final MeterRegistry registry;

    public TimedOfferMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param op extend | accept | decline | reoffer | expire ;
     *  @param outcome ok | accepted | declined | reoffered | expired | not_open | expired_deadline
     *                 | subject_taken | not_reofferable | skipped | invalid */
    public void record(String op, String outcome) {
        registry.counter("timed_offer_total", "op", op, "outcome", outcome).increment();
    }
}
