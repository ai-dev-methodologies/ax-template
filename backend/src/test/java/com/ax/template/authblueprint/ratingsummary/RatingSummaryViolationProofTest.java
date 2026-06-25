package com.ax.template.authblueprint.ratingsummary;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Violation proof tests for derived-aggregate-consistency-l0.yaml (DERIVED-AGG-CONSISTENCY-001).
 *
 * <p>Plain reflection — no Spring context required. Proves that the aggregate fields
 * {@code average} and {@code reviewCount} on {@link RatingSummary} have no public setters,
 * making hand-editing the derived aggregate structurally impossible.
 */
@Tag("RATING_SUMMARY")
class RatingSummaryViolationProofTest {

    @Test
    @Tag("DERIVED-AGG-CONSISTENCY-001")
    void violation_noPublicSetterForAverage() {
        for (Method m : RatingSummary.class.getDeclaredMethods()) {
            if ("setAverage".equals(m.getName())) {
                assertThat(Modifier.isPublic(m.getModifiers()))
                    .as("RatingSummary.setAverage MUST NOT be public — the aggregate is DERIVED only; "
                      + "a public setter would allow callers to bypass the sole-mutator service "
                      + "(DERIVED-AGG-CONSISTENCY-001)")
                    .isFalse();
            }
        }
        // Also assert no public method starts with 'setAverage' at all.
        long publicSetAverageCount = java.util.Arrays.stream(RatingSummary.class.getDeclaredMethods())
            .filter(m -> m.getName().startsWith("setAverage") && Modifier.isPublic(m.getModifiers()))
            .count();
        assertThat(publicSetAverageCount)
            .as("RatingSummary must have zero public setAverage* methods")
            .isZero();
    }

    @Test
    @Tag("DERIVED-AGG-CONSISTENCY-001")
    void violation_noPublicSetterForReviewCount() {
        for (Method m : RatingSummary.class.getDeclaredMethods()) {
            if ("setReviewCount".equals(m.getName())) {
                assertThat(Modifier.isPublic(m.getModifiers()))
                    .as("RatingSummary.setReviewCount MUST NOT be public — the aggregate is DERIVED only; "
                      + "a public setter would allow callers to bypass the sole-mutator service "
                      + "(DERIVED-AGG-CONSISTENCY-001)")
                    .isFalse();
            }
        }
        long publicSetCountCount = java.util.Arrays.stream(RatingSummary.class.getDeclaredMethods())
            .filter(m -> m.getName().startsWith("setReviewCount") && Modifier.isPublic(m.getModifiers()))
            .count();
        assertThat(publicSetCountCount)
            .as("RatingSummary must have zero public setReviewCount* methods")
            .isZero();
    }
}
