package com.ax.template.authblueprint.ratingsummary;

/**
 * Eligibility status for a review.
 *
 * <p>Only {@code APPROVED} reviews contribute to the aggregate (DERIVED-AGG-ELIGIBILITY-001).
 * {@code PENDING} (default) and {@code REJECTED} are excluded from recomputation.
 */
public enum ReviewStatus {
    PENDING,
    APPROVED,
    REJECTED
}
