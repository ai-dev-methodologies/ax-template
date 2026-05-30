package com.ax.template.authblueprint.dsr;

/**
 * The five GDPR data-subject-rights request kinds (specs/data-subject-rights-l0.yaml).
 *
 * <p>The lower-case {@link #metricType()} string is the fixed-enum bounded label
 * value used by the three canonical Micrometer meters (DSR-OBSERVABILITY-001) —
 * {@code type ∈ {access, rectify, erasure, portability, restrict}}.
 */
public enum DsrRequestType {
    ACCESS("access"),
    RECTIFY("rectify"),
    ERASURE("erasure"),
    PORTABILITY("portability"),
    RESTRICT("restrict");

    private final String metricType;

    DsrRequestType(String metricType) {
        this.metricType = metricType;
    }

    /** Bounded-cardinality metric label value (DSR-OBSERVABILITY-001). */
    public String metricType() {
        return metricType;
    }
}
