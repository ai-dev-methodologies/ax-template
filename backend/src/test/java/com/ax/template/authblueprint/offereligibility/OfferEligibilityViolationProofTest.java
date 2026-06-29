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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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

    /**
     * ELIGIBLE is the sole applied reason — driven through the real {@link OfferEligibilityService#decide}
     * evaluator with DISTINCT inputs that each produce one specific reason (not via the notApplied(...)
     * factory, which would assert a hard-coded literal). Every {@link EligibilityReason} value is reached
     * by construction, and only the ELIGIBLE-producing input yields applied=true; every other yields
     * applied=false. A regression that let any non-ELIGIBLE gate return applied (or that broke an
     * ELIGIBLE evaluation) falsifies this.
     */
    @Test
    @Tag("OFFER-FAIL-CLOSED-001")
    void violation_eligibleIsTheOnlyAppliedReason() {
        OfferEligibilityService evaluator = new OfferEligibilityService(null, null, null);
        UUID customer = UUID.randomUUID();
        UUID otherCustomer = UUID.randomUUID();

        var qualifyingContext = new OfferEligibilityService.EvaluationContext(
            customer, Set.of(),
            List.of(new OfferEligibilityService.Line("Q", null, 2),
                    new OfferEligibilityService.Line("T", null, 1)));

        // Each EligibilityReason mapped to a distinct (offer, context) that produces exactly it.
        Map<EligibilityReason, OfferEligibilityService.EligibilityDecision> produced = new EnumMap<>(EligibilityReason.class);

        record Case(EligibilityOffer offer, OfferEligibilityService.EvaluationContext ctx) {}
        Map<EligibilityReason, Case> cases = new EnumMap<>(EligibilityReason.class);
        cases.put(EligibilityReason.ELIGIBLE,
            new Case(fullyEligibleOffer(customer), qualifyingContext));
        cases.put(EligibilityReason.MISSING_QUALIFIER_CRITERIA,
            new Case(new EligibilityOffer(UUID.randomUUID(), "mq", null, null, 1, "T", null, 1000L,
                "gold", Set.of(customer), Instant.EPOCH), qualifyingContext));
        cases.put(EligibilityReason.MISSING_TARGET_CRITERIA,
            new Case(new EligibilityOffer(UUID.randomUUID(), "mt", "Q", null, 1, null, null, 1000L,
                "gold", Set.of(customer), Instant.EPOCH), qualifyingContext));
        cases.put(EligibilityReason.MISSING_ELIGIBILITY_CRITERIA,
            new Case(new EligibilityOffer(UUID.randomUUID(), "me", "Q", null, 1, "T", null, 1000L,
                null, Set.of(), Instant.EPOCH), qualifyingContext));
        cases.put(EligibilityReason.UNKNOWN_CUSTOMER,
            new Case(fullyEligibleOffer(customer),
                new OfferEligibilityService.EvaluationContext(null, Set.of(), List.of())));
        cases.put(EligibilityReason.CUSTOMER_NOT_ELIGIBLE,
            new Case(fullyEligibleOffer(otherCustomer),
                new OfferEligibilityService.EvaluationContext(customer, Set.of("silver"),
                    List.of(new OfferEligibilityService.Line("Q", null, 2),
                            new OfferEligibilityService.Line("T", null, 1)))));
        cases.put(EligibilityReason.QUALIFIER_MIN_QTY_NOT_MET,
            new Case(fullyEligibleOffer(customer),
                new OfferEligibilityService.EvaluationContext(customer, Set.of(),
                    List.of(new OfferEligibilityService.Line("Q", null, 1),
                            new OfferEligibilityService.Line("T", null, 1)))));
        cases.put(EligibilityReason.NO_TARGET_LINE,
            new Case(fullyEligibleOffer(customer),
                new OfferEligibilityService.EvaluationContext(customer, Set.of(),
                    List.of(new OfferEligibilityService.Line("Q", null, 2)))));

        // Every reason must have an input that drives it (exhaustive coverage — a new reason without a
        // case fails here), and decide() must produce exactly that reason with the right applied flag.
        for (EligibilityReason expected : EligibilityReason.values()) {
            Case c = cases.get(expected);
            assertThat(c).as("missing a driving input for reason %s", expected).isNotNull();
            var decision = evaluator.decide(c.offer(), c.ctx());
            assertThat(decision.reason())
                .as("input for %s must drive decide() to that exact reason", expected)
                .isEqualTo(expected);
            assertThat(decision.applied())
                .as("only ELIGIBLE may be applied; %s must NOT be applied", expected)
                .isEqualTo(expected == EligibilityReason.ELIGIBLE);
            produced.put(expected, decision);
        }

        // Exactly one applied outcome across all distinct inputs, and it is ELIGIBLE.
        long appliedCount = produced.values().stream().filter(OfferEligibilityService.EligibilityDecision::applied).count();
        assertThat(appliedCount).as("exactly one driven input yields an applied decision").isEqualTo(1L);
        assertThat(produced.get(EligibilityReason.ELIGIBLE).applied()).isTrue();
    }

    private static EligibilityOffer fullyEligibleOffer(UUID allowedCustomer) {
        return new EligibilityOffer(UUID.randomUUID(), "full-" + UUID.randomUUID(),
            "Q", null, 2, "T", null, 1000L, "gold", Set.of(allowedCustomer), Instant.EPOCH);
    }
}
