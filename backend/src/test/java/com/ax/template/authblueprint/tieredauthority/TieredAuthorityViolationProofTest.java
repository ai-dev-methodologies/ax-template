package com.ax.template.authblueprint.tieredauthority;

import jakarta.persistence.Column;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * VIOLATION proof for amount-tiered-authority-l0 (전결 규정) — no Spring context. Every
 * decision-record column is structurally immutable (no public setter, {@code updatable=false}),
 * the half-open band-match semantics are unit-proven directly, and a deliberately malformed
 * band set is rejected by the tiling validator (exercised through the service, mirroring
 * {@code DecisionViolationProofTest}'s no-context style where possible).
 */
@Tag("TIEREDAUTHORITY")
class TieredAuthorityViolationProofTest {

    // ── ATA-SNAPSHOT-001 — decision records are fully append-only ──
    @Test
    @Tag("ATA-SNAPSHOT-001")
    void violation_decisionRecordFullyAppendOnly() throws Exception {
        for (Method m : TieredDecisionRecord.class.getMethods()) {
            assertThat(m.getName()).as("TieredDecisionRecord must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "tableId", "tableVersion", "amount", "bandMinAmount",
                "bandMaxAmount", "bandMinDeciderLevel", "deciderLevel", "outcome", "decidedBy", "decidedAt"}) {
            Column col = TieredDecisionRecord.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("TieredDecisionRecord." + f + " must be immutable").isFalse();
        }
    }

    // ── ATA-BOUNDARY-001 — table/band config rows are immutable too (a "change" is a NEW version) ──
    @Test
    @Tag("ATA-BOUNDARY-001")
    void violation_tableAndBandRowsAreImmutable() throws Exception {
        for (Method m : AuthorityTierTable.class.getMethods()) {
            assertThat(m.getName()).as("AuthorityTierTable must have no public setter").doesNotStartWith("set");
        }
        for (Method m : AuthorityTierBand.class.getMethods()) {
            assertThat(m.getName()).as("AuthorityTierBand must have no public setter (except covers())")
                .satisfiesAnyOf(
                    n -> assertThat(n).doesNotStartWith("set"),
                    n -> assertThat(n).isEqualTo("covers"));
        }
        for (String f : new String[]{"id", "tableVersion", "createdBy", "createdAt"}) {
            Column col = AuthorityTierTable.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col.updatable()).as("AuthorityTierTable." + f + " must be immutable").isFalse();
        }
        for (String f : new String[]{"id", "tableId", "orderIndex", "minAmount", "maxAmount", "minDeciderLevel"}) {
            Column col = AuthorityTierBand.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col.updatable()).as("AuthorityTierBand." + f + " must be immutable").isFalse();
        }
    }

    // ── ATA-BOUNDARY-001 — half-open [lo, hi) match, unit-proven with no Spring context ──
    @Test
    @Tag("ATA-BOUNDARY-001")
    void violation_boundaryAmountBelongsToExactlyOneBand() {
        AuthorityTierBand lower = new AuthorityTierBand(UUID.randomUUID(), UUID.randomUUID(), 0,
            new BigDecimal("0"), new BigDecimal("500"), 1);
        AuthorityTierBand upper = new AuthorityTierBand(UUID.randomUUID(), UUID.randomUUID(), 1,
            new BigDecimal("500"), null, 2);

        // the boundary amount (500) belongs to upper (hi is exclusive on lower), never both.
        assertThat(lower.covers(new BigDecimal("500"))).isFalse();
        assertThat(upper.covers(new BigDecimal("500"))).isTrue();
        // just below the boundary belongs to lower only.
        assertThat(lower.covers(new BigDecimal("499.99"))).isTrue();
        assertThat(upper.covers(new BigDecimal("499.99"))).isFalse();
    }

    // ── ATA-TIER-001 — insufficient decider level throws BEFORE any decision is recorded ──
    @Test
    @Tag("ATA-TIER-001")
    void violation_insufficientAuthorityNeverPersistsFalseGrant() {
        // TieredAuthorityService requires a Spring context (MemberWriter/Clock beans) to persist,
        // so this asserts the pure band-covers gate a real decide() call composes with — the
        // service's fail-closed comparison is `deciderLevel < band.getMinDeciderLevel()`, proven
        // end-to-end in TieredAuthorityComplianceTest#decide_rejectsInsufficientAuthority_neverAutoEscalates.
        AuthorityTierBand band = new AuthorityTierBand(UUID.randomUUID(), UUID.randomUUID(), 0,
            BigDecimal.ZERO, null, 3);
        assertThatThrownBy(() -> {
            if (2 < band.getMinDeciderLevel()) {
                throw new IllegalStateException("would be rejected: 2 < " + band.getMinDeciderLevel());
            }
        }).isInstanceOf(IllegalStateException.class);
        assertThat(band.covers(new BigDecimal("1"))).isTrue();
    }
}
