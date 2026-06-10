package com.ax.template.authblueprint.decisiongov;

import jakarta.persistence.Column;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for decision-governance-l0. Structural assertions a deliberate break cannot
 * pass silently: version rows are fully append-only, the four-eyes/reason gates are declared
 * as the entity @Check AND in the migration, the chain is uniquely keyed, the scope's
 * version pointer is package-sealed, and both re-determination paths go through the
 * PESSIMISTIC_WRITE finder — no Spring context.
 */
@Tag("DECISIONGOV")
class DecisionViolationProofTest {

    // ── DG-RECOMPUTE-001 — version rows fully append-only (every column immutable, no setter) ──
    @Test @Tag("DG-RECOMPUTE-001") @Tag("DG-BASIS-001")
    void violation_versionRowFullyAppendOnly() throws Exception {
        for (Method m : DecisionVersion.class.getMethods()) {
            assertThat(m.getName()).as("DecisionVersion must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "scopeId", "versionNo", "kind", "basisJson",
                "outcome", "reason", "decidedBy", "approvedBy", "decidedAt"}) {
            Column col = DecisionVersion.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("DecisionVersion." + f + " must be immutable").isFalse();
        }
    }

    // ── DG-OVERRIDE-001 — the four-eyes + reason gates are declared on the entity @Check ──
    @Test @Tag("DG-OVERRIDE-001")
    void violation_entityCarriesFourEyesAndReasonChecks() {
        Check check = DecisionVersion.class.getAnnotation(Check.class);
        assertThat(check).as("DecisionVersion must carry @Check").isNotNull();
        String c = check.constraints().replaceAll("\\s+", " ");
        assertThat(c).contains("kind <> 'OVERRIDE' OR (approved_by IS NOT NULL AND approved_by <> decided_by)");
        assertThat(c).contains("kind = 'COMPUTED' OR LENGTH(TRIM(reason)) > 0");
    }

    // ── DG-CHAIN-001 — duplicate (scope, version) unrepresentable; pointer package-sealed ──
    @Test @Tag("DG-CHAIN-001")
    void violation_chainUniquelyKeyed_pointerSealed() throws Exception {
        jakarta.persistence.Table table = DecisionVersion.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(table.uniqueConstraints()).as("uq(scope_id, version_no) must be declared").isNotEmpty();
        assertThat(table.uniqueConstraints()[0].columnNames()).containsExactly("scope_id", "version_no");

        for (Method m : DecisionScope.class.getMethods()) {
            assertThat(m.getName()).as("DecisionScope must expose no public setter").doesNotStartWith("set");
        }
        Method advance = DecisionScope.class.getDeclaredMethod("advanceVersion", int.class);
        assertThat(Modifier.isPublic(advance.getModifiers()))
            .as("DecisionScope.advanceVersion must be package-private (service is the sole mutator)")
            .isFalse();
        assertThat(DecisionScope.class.getDeclaredField("version").isAnnotationPresent(Version.class))
            .as("DecisionScope.version must carry @Version").isTrue();
    }

    // ── DG-CONCURRENT-001 — both re-determination paths go through the locked finder ──
    @Test @Tag("DG-CONCURRENT-001")
    void violation_reDeterminationsUseTheLockedFinder() throws Exception {
        Method locked = DecisionScopeRepository.class.getMethod("findByScopeKeyForUpdate", String.class);
        org.springframework.data.jpa.repository.Lock lock =
            locked.getAnnotation(org.springframework.data.jpa.repository.Lock.class);
        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);

        String src = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "decisiongov", "DecisionService.java"));
        for (String method : new String[]{"public DecisionVersion recompute(", "public DecisionVersion override("}) {
            int start = src.indexOf(method);
            assertThat(start).as(method + " must exist").isPositive();
            String body = src.substring(start, src.indexOf("\n    }", start));
            assertThat(body)
                .as(method + " must read the scope via the PESSIMISTIC_WRITE finder")
                .contains("findByScopeKeyForUpdate");
        }
    }

    // ── DG-OVERRIDE-001 / DG-CHAIN-001 — the migration carries the same backstops ──
    @Test @Tag("DG-OVERRIDE-001") @Tag("DG-CHAIN-001")
    void violation_migrationCarriesTheSameBackstops() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V043__create_decision_governance.sql")) {
            assertThat(in).as("V043__create_decision_governance.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("approved_by IS NOT NULL AND approved_by <> decided_by");
            assertThat(sql).contains("kind = 'COMPUTED' OR LENGTH(TRIM(reason)) > 0");
            assertThat(sql).contains("UNIQUE INDEX uq_decision_scope_version");
        }
    }
}
