package com.ax.template.authblueprint.appealindependence;

import jakarta.persistence.Column;
import jakarta.persistence.Table;

import org.hibernate.annotations.Check;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for appeal-decider-independence-l0 — structural assertions a deliberate
 * break cannot pass silently: every row is fully append-only (no public setter, all columns
 * immutable), the decider-independence + one-appeal-per-level gates are declared as DB
 * backstops AND carried in the migration, mirroring {@code decisiongov.DecisionViolationProofTest}.
 */
@Tag("APPEALINDEPENDENCE")
class AppealViolationProofTest {

    // ── APPEAL-OUTCOME-001 — every row fully append-only ──
    @Test
    @Tag("APPEAL-OUTCOME-001")
    void violation_rowFullyAppendOnly() throws Exception {
        for (Method m : AppealDecision.class.getMethods()) {
            assertThat(m.getName()).as("AppealDecision must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "parentDecisionId", "chainRootId", "level", "kind",
                "decidedBy", "appealedDeciderBy", "outcome", "decidedAt"}) {
            Column col = AppealDecision.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("AppealDecision." + f + " must be immutable").isFalse();
        }
    }

    // ── APPEAL-DISTINCT-001 — the independence gate is declared on the entity @Check ──
    @Test
    @Tag("APPEAL-DISTINCT-001")
    void violation_entityCarriesIndependenceCheck() {
        Check check = AppealDecision.class.getAnnotation(Check.class);
        assertThat(check).as("AppealDecision must carry @Check").isNotNull();
        String c = check.constraints().replaceAll("\\s+", " ");
        assertThat(c).contains("kind = 'ORIGINAL'");
        assertThat(c).contains("appealed_decider_by IS NOT NULL AND decided_by <> appealed_decider_by");
    }

    // ── APPEAL-CHAIN-001 — one appeal per level is a UNIQUE constraint, not just app logic ──
    @Test
    @Tag("APPEAL-CHAIN-001")
    void violation_oneAppealPerLevelIsUniquelyKeyed() {
        Table table = AppealDecision.class.getAnnotation(Table.class);
        assertThat(table.uniqueConstraints()).as("uq(parent_decision_id) must be declared").isNotEmpty();
        assertThat(table.uniqueConstraints()[0].columnNames()).containsExactly("parent_decision_id");
    }

    // ── APPEAL-DISTINCT-001 / APPEAL-CHAIN-001 — the migration carries the same backstops ──
    @Test
    @Tag("APPEAL-DISTINCT-001")
    void violation_migrationCarriesTheSameBackstops() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V093__create_appeal_independence.sql")) {
            assertThat(in).as("V093__create_appeal_independence.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("appealed_decider_by IS NOT NULL AND decided_by <> appealed_decider_by");
            assertThat(sql).contains("UNIQUE (parent_decision_id)");
        }
    }
}
