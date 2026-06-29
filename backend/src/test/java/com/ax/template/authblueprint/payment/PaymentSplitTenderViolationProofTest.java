package com.ax.template.authblueprint.payment;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Violation proof tests for PAYMENT-SPLIT-001 (split-tender coverage) — external-reference absorption G005.
 *
 * <p>Plain reflection — no Spring context required. Where {@code PaymentSplitTenderTest} proves the
 * coverage gate BEHAVIORALLY (under-funded → 422, fully-covered → 200), this proves the same
 * invariant is enforced BY CONSTRUCTION, so the behavioral path cannot be sidestepped:
 * <ol>
 *   <li>the coverage sum counts EXACTLY the two successfully-authorized states {AUTHORIZED, CAPTURED}
 *       — a PENDING / DECLINED / VOIDED / REFUNDED tender structurally cannot inflate coverage;</li>
 *   <li>the tender event ledger (amount + type) is append-only ({@code updatable=false}) — a recorded
 *       tender's amount or type cannot be mutated post-hoc to game the coverage sum.</li>
 * </ol>
 *
 * <p>This is the methodology-mandated ViolationProofTest for the absorbed payment vertical; its
 * absence (caught by the 2026-06-26 completeness audit) is what motivates the
 * {@code violation_proof} field now required by the absorption parity guard.
 */
@Tag("PAYMENT")
class PaymentSplitTenderViolationProofTest {

    @Test
    @Tag("PAYMENT-SPLIT-001")
    void violation_coverageCountsExactlyTheTwoAuthorizedStates() throws Exception {
        Query q = PaymentRepository.class
                .getMethod("sumActiveAuthorizedByOrderId", String.class, String.class)
                .getAnnotation(Query.class);
        assertThat(q)
                .as("coverage sum must be a single pinned @Query, not a hand-assembled/overridable path")
                .isNotNull();
        String jpql = q.value();
        assertThat(jpql).contains("PaymentState.AUTHORIZED");
        assertThat(jpql).contains("PaymentState.CAPTURED");
        // Exactly two PaymentState values are coverage-eligible. If a future edit added a third state
        // (e.g. PENDING) to the IN-clause, this count flips to 3 and the proof fails — so coverage can
        // never be silently widened to count a not-yet-authorized tender.
        int stateRefs = jpql.split("PaymentState\\.", -1).length - 1;
        assertThat(stateRefs)
                .as("exactly 2 PaymentState values may count toward split-tender coverage")
                .isEqualTo(2);
    }

    @Test
    @Tag("PAYMENT-SPLIT-001")
    void violation_tenderLedgerAmountAndTypeAreAppendOnly() throws Exception {
        // The coverage sum is derived from recorded tender events; their amount and type must be
        // immutable, or a settled tender could be edited to fabricate coverage after the fact.
        for (String fieldName : new String[] {"amountNumeric", "type"}) {
            Field f = PaymentEvent.class.getDeclaredField(fieldName);
            jakarta.persistence.Column col = f.getAnnotation(jakarta.persistence.Column.class);
            assertThat(col).as("PaymentEvent." + fieldName + " must carry @Column").isNotNull();
            assertThat(col.updatable())
                    .as("PaymentEvent." + fieldName + " must be updatable=false (append-only tender ledger)")
                    .isFalse();
        }
    }
}
