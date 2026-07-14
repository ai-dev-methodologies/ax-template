package com.ax.template.authblueprint.duplicatesubmission;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality counter (domain-metrics-bounded-cardinality): {@code duplicate_submission_total
 * {op, outcome}}, both fixed enums — never a subjectRef, lossType, or submission id.
 */
@Component
public class DuplicateSubmissionMetrics {

    private final MeterRegistry registry;

    public DuplicateSubmissionMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** @param op defineChannel | submit | withdraw | reject ;
     *  @param outcome ok | accepted | flagged | duplicate | invalid */
    public void record(String op, String outcome) {
        registry.counter("duplicate_submission_total", "op", op, "outcome", outcome).increment();
    }
}
