package com.ax.template.authblueprint.offereligibility;

import jakarta.persistence.Column;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Violation-proof tests for offer-eligibility-l0.yaml — reflection-only structural negatives plus a
 * direct (no-Spring) invocation of the pure evaluator. They prove the fail-closed applicability
 * invariant is UNREPRESENTABLE to violate: criteria are immutable, there is no public setter, no
 * stored decision flag, and the evaluator's only applied outcome is reason ELIGIBLE — every missing
 * criterion denies by default.
 */
@Tag("OFFER_ELIGIBILITY")
class OfferEligibilityViolationProofTest {

    /**
     * OFFER-FAIL-CLOSED-001 / persistence: every declared criterion is @Column(updatable=false) —
     * the offer's eligibility inputs cannot be re-written after creation, so an evaluation can never
     * drift from the declaration it summarizes.
     */
    @Test
    @Tag("OFFER-FAIL-CLOSED-001")
    void violation_criteriaColumnsImmutable() throws NoSuchFieldException {
        for (String field : new String[] {"name", "qualifierSku", "qualifierTag", "minQualifierQty",
                "targetSku", "targetTag", "discountBasisPoints", "eligibleSegment", "createdAt"}) {
            Field f = EligibilityOffer.class.getDeclaredField(field);
            Column col = f.getAnnotation(Column.class);
            assertThat(col).as("EligibilityOffer.%s MUST be a @Column", field).isNotNull();
            assertThat(col.updatable())
                .as("EligibilityOffer.%s MUST be updatable=false (immutable declared criterion)", field)
                .isFalse();
        }
    }

    /** Optimistic-locking @Version is present (concurrent-mutation guard). */
    @Test
    @Tag("OFFER-FAIL-CLOSED-001")
    void violation_versionFieldPresent() {
        boolean hasVersion = Arrays.stream(EligibilityOffer.class.getDeclaredFields())
            .anyMatch(f -> f.isAnnotationPresent(Version.class));
        assertThat(hasVersion).as("EligibilityOffer MUST carry an @Version field").isTrue();
    }

    /** No public setter on the aggregate — the criteria are fixed at creation, the decision is derived. */
    @Test
    @Tag("OFFER-FAIL-CLOSED-001")
    void violation_noPublicSettersAndNoStoredDecisionFlag() {
        long publicSetters = Arrays.stream(EligibilityOffer.class.getDeclaredMethods())
            .filter(m -> m.getName().startsWith("set") && Modifier.isPublic(m.getModifiers()))
            .count();
        assertThat(publicSetters)
            .as("EligibilityOffer MUST have zero public setters — its criteria are immutable")
            .isZero();

        // No stored applied/eligible boolean — the decision is DERIVED per evaluation, never persisted.
        for (Field f : EligibilityOffer.class.getDeclaredFields()) {
            String name = f.getName().toLowerCase();
            assertThat(name)
                .as("EligibilityOffer MUST NOT store an applied/eligible decision flag (field '%s')", f.getName())
                .doesNotContain("applied")
                .doesNotContain("eligible_decision");
            if (name.startsWith("iseligible") || name.equals("eligible") || name.equals("applied")) {
                throw new AssertionError("EligibilityOffer must not store a decision flag: " + f.getName());
            }
        }
    }

    /** The @Check makes a non-positive min quantity / negative discount unrepresentable at the DB layer. */
    @Test
    @Tag("OFFER-QUALIFIER-MINQTY-001")
    void violation_checkConstraintPresent() {
        Check chk = EligibilityOffer.class.getAnnotation(Check.class);
        assertThat(chk).as("EligibilityOffer MUST carry an @Check").isNotNull();
        assertThat(chk.constraints())
            .as("the @Check MUST bind min_qualifier_qty and discount_basis_points")
            .contains("min_qualifier_qty >= 1")
            .contains("discount_basis_points >= 0");
    }

    /** The customer-xref allow-list getter returns an immutable defensive copy — it cannot be mutated externally. */
    @Test
    @Tag("OFFER-SEGMENT-ELIGIBILITY-001")
    void violation_allowListGetterIsImmutableCopy() {
        EligibilityOffer offer = fullyEligibleOffer(UUID.randomUUID());
        Set<UUID> exposed = offer.getEligibleCustomerIds();
        assertThatThrownBy(() -> exposed.add(UUID.randomUUID()))
            .as("the exposed allow-list MUST be immutable (defensive copy)")
            .isInstanceOf(UnsupportedOperationException.class);
    }

    /**
     * OFFER-FAIL-CLOSED-001 (keystone): the pure evaluator denies by default. Invoked directly (no
     * Spring, null deps — decide() reads no injected collaborator), every missing criterion yields a
     * NOT-APPLIED reason, and the ONLY applied outcome carries reason ELIGIBLE.
     */
    @Test
    @Tag("OFFER-FAIL-CLOSED-001")
    void violation_evaluatorDeniesByDefaultAndOnlyAppliesWhenEligible() {
        OfferEligibilityService evaluator = new OfferEligibilityService(null, null, null);
        UUID customer = UUID.randomUUID();
        var qualifyingContext = new OfferEligibilityService.EvaluationContext(
            customer, Set.of(),
            List.of(new OfferEligibilityService.Line("Q", null, 2),
                    new OfferEligibilityService.Line("T", null, 1)));

        // Fully eligible → applied, reason ELIGIBLE (the only applied outcome).
        var ok = evaluator.decide(fullyEligibleOffer(customer), qualifyingContext);
        assertThat(ok.applied()).isTrue();
        assertThat(ok.reason()).isEqualTo(EligibilityReason.ELIGIBLE);

        // Missing target criteria → fail-closed NOT-APPLIED even with a fully qualifying context.
        EligibilityOffer noTarget = new EligibilityOffer(UUID.randomUUID(), "no-target",
            "Q", null, 1, null, null, 1000L, null, Set.of(customer), Instant.EPOCH);
        var denied = evaluator.decide(noTarget, qualifyingContext);
        assertThat(denied.applied()).isFalse();
        assertThat(denied.reason()).isEqualTo(EligibilityReason.MISSING_TARGET_CRITERIA);

        // Unknown customer in context → fail-closed.
        var unknown = evaluator.decide(fullyEligibleOffer(customer),
            new OfferEligibilityService.EvaluationContext(null, Set.of(), List.of()));
        assertThat(unknown.applied()).isFalse();
        assertThat(unknown.reason()).isEqualTo(EligibilityReason.UNKNOWN_CUSTOMER);

        // Below-threshold qualifier → NOT-APPLIED (not an error; just not applicable).
        var below = evaluator.decide(fullyEligibleOffer(customer),
            new OfferEligibilityService.EvaluationContext(customer, Set.of(),
                List.of(new OfferEligibilityService.Line("Q", null, 1),
                        new OfferEligibilityService.Line("T", null, 1))));
        assertThat(below.applied()).isFalse();
        assertThat(below.reason()).isEqualTo(EligibilityReason.QUALIFIER_MIN_QTY_NOT_MET);
    }

    /** ELIGIBLE is the sole applied reason — every other EligibilityReason is a NOT-APPLIED outcome. */
    @Test
    @Tag("OFFER-FAIL-CLOSED-001")
    void violation_eligibleIsTheOnlyAppliedReason() {
        OfferEligibilityService.EligibilityDecision applied =
            OfferEligibilityService.EligibilityDecision.applied(UUID.randomUUID());
        assertThat(applied.reason()).isEqualTo(EligibilityReason.ELIGIBLE);
        assertThat(applied.applied()).isTrue();
        for (EligibilityReason r : EligibilityReason.values()) {
            if (r == EligibilityReason.ELIGIBLE) continue;
            var d = OfferEligibilityService.EligibilityDecision.notApplied(UUID.randomUUID(), r);
            assertThat(d.applied())
                .as("reason %s MUST be a NOT-APPLIED outcome (only ELIGIBLE applies)", r)
                .isFalse();
        }
    }

    private static EligibilityOffer fullyEligibleOffer(UUID allowedCustomer) {
        return new EligibilityOffer(UUID.randomUUID(), "full-" + UUID.randomUUID(),
            "Q", null, 2, "T", null, 1000L, "gold", Set.of(allowedCustomer), Instant.EPOCH);
    }
}
