package com.ax.template.authblueprint.divisibility;

import jakarta.persistence.Column;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for material-divisibility-constraint-l0. Structural assertions a deliberate break
 * cannot pass silently: the integrality/scale test is EXACT (stripTrailingZeros, format-independent),
 * the policy is versioned + immutable with the @Check backstop, the check records are immutable +
 * append-only, NO delete path exists, the re-declaration uses the PESSIMISTIC_WRITE finder, the
 * service REJECTS (throws) rather than rounding, and the migration carries the same backstops.
 */
@Tag("DIVISIBILITY")
class DivisibilityViolationProofTest {

    // ── DIV-DETERMINISM-001 — integrality/scale via exact stripTrailingZeros, format-independent ──
    @Test @Tag("DIV-DETERMINISM-001")
    void violation_integralityIsExact_andFormatIndependent() {
        // 5, 5.0, 5.00 are all integral; 5.5 is not — independent of literal format
        assertThat(DivisibilityArithmetic.isIntegral(new BigDecimal("5"))).isTrue();
        assertThat(DivisibilityArithmetic.isIntegral(new BigDecimal("5.0"))).isTrue();
        assertThat(DivisibilityArithmetic.isIntegral(new BigDecimal("5.00"))).isTrue();
        assertThat(DivisibilityArithmetic.isIntegral(new BigDecimal("5.000000"))).isTrue();
        assertThat(DivisibilityArithmetic.isIntegral(new BigDecimal("5.5"))).isFalse();
        assertThat(DivisibilityArithmetic.isIntegral(new BigDecimal("0.001"))).isFalse();
        // effective scale is measured AFTER stripping trailing zeros
        assertThat(DivisibilityArithmetic.effectiveScale(new BigDecimal("5"))).isEqualTo(0);
        assertThat(DivisibilityArithmetic.effectiveScale(new BigDecimal("1.250"))).isEqualTo(2);
        assertThat(DivisibilityArithmetic.effectiveScale(new BigDecimal("1.2345"))).isEqualTo(4);
        assertThat(DivisibilityArithmetic.effectiveScale(new BigDecimal("100"))).isEqualTo(0);
    }

    // ── DIV-POLICY-001 — policy versioned, immutable, @Version, @Check backstop, uq(material,version) ──
    @Test @Tag("DIV-POLICY-001")
    void violation_policyImmutable_versioned_checkBackstop() throws Exception {
        for (Method m : MaterialDivisibilityPolicy.class.getMethods()) {
            assertThat(m.getName()).as("MaterialDivisibilityPolicy must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "materialRef", "policyVersion", "policyKind", "maxScale", "declaredAt"}) {
            Column col = MaterialDivisibilityPolicy.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("MaterialDivisibilityPolicy." + f + " must be immutable").isFalse();
        }
        assertThat(MaterialDivisibilityPolicy.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();

        Check check = MaterialDivisibilityPolicy.class.getAnnotation(Check.class);
        String c = check.constraints().replaceAll("\\s+", " ");
        assertThat(c).contains("policy_version >= 1 AND max_scale >= 0");
        assertThat(c).contains("policy_kind = 'FRACTIONAL' OR max_scale = 0");

        jakarta.persistence.Table table = MaterialDivisibilityPolicy.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(table.uniqueConstraints()[0].columnNames()).containsExactly("material_ref", "policy_version");

        // columns avoid reserved words (never value / limit / order)
        for (String banned : new String[]{"value", "order"}) {
            assertThat(java.util.Arrays.stream(MaterialDivisibilityPolicy.class.getDeclaredFields())
                .map(fld -> {
                    Column col = fld.getAnnotation(Column.class);
                    return col == null ? "" : col.name();
                }))
                .as("no column named '" + banned + "' (reserved word)").doesNotContain(banned);
        }
    }

    // ── DIV-RECORD-001 — check records immutable, append-only, no public setter ──
    @Test @Tag("DIV-RECORD-001")
    void violation_checkRecordsImmutable_appendOnly() throws Exception {
        for (Method m : DivisibilityCheck.class.getMethods()) {
            assertThat(m.getName()).as("DivisibilityCheck must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "materialRef", "submittedQuantity", "verdict", "policyVersion", "checkedAt"}) {
            Column col = DivisibilityCheck.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("DivisibilityCheck." + f + " must be immutable").isFalse();
        }
    }

    // ── DIV-INTEGRAL/PRECISION-001 — NO delete path; the service REJECTS (throws) rather than rounding ──
    @Test @Tag("DIV-INTEGRAL-001") @Tag("DIV-PRECISION-001")
    void violation_noDeletePath_serviceRejectsNeverRounds() throws Exception {
        for (Method m : MaterialDivisibilityPolicyRepository.class.getDeclaredMethods()) {
            assertThat(m.getName()).doesNotContain("delete");
        }
        for (String src : new String[]{"DivisibilityService", "DivisibilityController"}) {
            String text = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
                "com", "ax", "template", "authblueprint", "divisibility", src + ".java"));
            assertThat(text).as(src + " must contain no delete call — policies/checks are appended, never removed")
                .doesNotContain(".delete(").doesNotContain("deleteBy");
        }
        String svc = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "divisibility", "DivisibilityService.java"));
        // the gate THROWS on a forbidden quantity — it must NOT round/quantize it
        assertThat(svc).as("the service rejects, never rounds")
            .contains("DivisibilityException.nonIntegral")
            .contains("DivisibilityException.excessPrecision")
            .doesNotContain("Math.round").doesNotContain("setScale");
        // a rejection is RECORDED (noRollbackFor) — the immutable check survives the 422
        assertThat(svc).as("a recorded rejection must survive the 422")
            .contains("noRollbackFor = DivisibilityException.class");
        // integrality is the exact stripTrailingZeros test, not a Double parse
        String arith = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "divisibility", "DivisibilityArithmetic.java"));
        assertThat(arith).contains("stripTrailingZeros").doesNotContain("doubleValue").doesNotContain("Double.parse");
    }

    // ── DIV-POLICY-001 — the re-declaration write path uses the PESSIMISTIC_WRITE finder ──
    @Test @Tag("DIV-POLICY-001")
    void violation_lockedFinder_serializesReDeclaration() throws Exception {
        Method locked = MaterialDivisibilityPolicyRepository.class.getMethod("findCurrentForUpdate", String.class);
        org.springframework.data.jpa.repository.Lock lock =
            locked.getAnnotation(org.springframework.data.jpa.repository.Lock.class);
        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);

        String svc = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "divisibility", "DivisibilityService.java"));
        int start = svc.indexOf("public MaterialDivisibilityPolicy declare(");
        assertThat(start).as("declare must exist").isPositive();
        String body = svc.substring(start, svc.indexOf("\n    }", start));
        assertThat(body).as("declare must take the material row lock").contains("findCurrentForUpdate");
    }

    // ── the migration carries the same backstops ──
    @Test @Tag("DIV-POLICY-001") @Tag("DIV-RECORD-001")
    void violation_migrationCarriesTheSameBackstops() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V068__create_divisibility.sql")) {
            assertThat(in).as("V068__create_divisibility.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("policy_version >= 1 AND max_scale >= 0");
            assertThat(sql).contains("policy_kind = 'FRACTIONAL' OR max_scale = 0");
            assertThat(sql).contains("UNIQUE INDEX uq_divisibility_material_version");
            assertThat(sql).contains("(material_ref, policy_version)");
            assertThat(sql).contains("submitted_quantity");
        }
    }
}
