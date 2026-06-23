package com.ax.template.authblueprint.sensitiveaccess;

import jakarta.persistence.Column;
import jakarta.persistence.Version;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for sensitive-read-audit-l0. Structural assertions a deliberate break cannot pass
 * silently: the access-log rows are append-only + fully immutable + setter-free; the raw value is
 * tagged @SensitiveField and immutable; the default projection masks (never equals raw); the service
 * RECORDS the access BEFORE returning the raw value in one reveal method; NO delete path exists; the
 * access-log query is ROLE_ADMIN-gated; the record carries @Version; the migration carries the same shape.
 */
@Tag("SENSITIVEACCESS")
class SensitiveAccessViolationProofTest {

    private static final Path SRC = Path.of(System.getProperty("user.dir"), "src", "main", "java",
        "com", "ax", "template", "authblueprint", "sensitiveaccess");

    // ── SENSITIVE-READ-001 — access-log rows append-only, fully immutable, no public setter ──
    @Test @Tag("SENSITIVE-READ-001")
    void violation_accessLogAppendOnly_immutable_noSetter() throws Exception {
        for (Method m : SensitiveAccessLog.class.getMethods()) {
            assertThat(m.getName()).as("SensitiveAccessLog must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "recordId", "recordRef", "fieldName", "accessor", "purpose", "occurredAt"}) {
            Column col = SensitiveAccessLog.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("SensitiveAccessLog." + f + " must be immutable").isFalse();
        }
    }

    // ── SENSITIVE-READ-001 — the raw value is @SensitiveField-tagged and immutable; @Version present ──
    @Test @Tag("SENSITIVE-READ-001")
    void violation_rawValueIsSensitiveFieldTagged_immutable_versioned() throws Exception {
        assertThat(SensitiveRecord.class.getDeclaredField("rawValue").isAnnotationPresent(SensitiveField.class))
            .as("the raw value field MUST carry @SensitiveField (the generic audit-on-read marker)").isTrue();
        assertThat(SensitiveRecord.class.getMethod("getRawValue").isAnnotationPresent(SensitiveField.class))
            .as("the raw value getter MUST carry @SensitiveField").isTrue();
        for (String f : new String[]{"id", "recordRef", "fieldName", "rawValue", "owner", "createdAt"}) {
            Column col = SensitiveRecord.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col.updatable()).as("SensitiveRecord." + f + " must be immutable").isFalse();
        }
        assertThat(SensitiveRecord.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();
    }

    // ── SENSITIVE-MASK-001 — the masked projection never equals the raw value ──
    @Test @Tag("SENSITIVE-MASK-001")
    void violation_maskedProjection_neverEqualsRaw() {
        SensitiveRecord r = new SensitiveRecord(UUID.randomUUID(), "REF", "field",
            "1234567890123456", "owner", java.time.Instant.now());
        assertThat(r.maskedValue()).isEqualTo("****3456");
        assertThat(r.maskedValue()).isNotEqualTo(r.getRawValue());
        // a short value is fully masked
        SensitiveRecord shortV = new SensitiveRecord(UUID.randomUUID(), "REF", "field",
            "abc", "owner", java.time.Instant.now());
        assertThat(shortV.maskedValue()).isEqualTo("****");
        assertThat(shortV.maskedValue()).isNotEqualTo("abc");
    }

    // ── SENSITIVE-READ-001 — the service RECORDS the access BEFORE returning the raw value ──
    @Test @Tag("SENSITIVE-READ-001") @Tag("SENSITIVE-PURPOSE-001")
    void violation_revealRecordsBeforeReturn_andPurposeGate() throws Exception {
        String svc = Files.readString(SRC.resolve("SensitiveAccessService.java"));
        int start = svc.indexOf("public String reveal(");
        assertThat(start).as("reveal method must exist").isPositive();
        String body = svc.substring(start, svc.indexOf("\n    }", start));

        int persist = body.indexOf("persistAndFlush");
        int ret = body.indexOf("return r.getRawValue()");
        assertThat(persist).as("reveal must write the access-log row").isPositive();
        assertThat(ret).as("reveal must return the raw value").isPositive();
        assertThat(persist).as("the access row MUST be recorded BEFORE the raw value is returned")
            .isLessThan(ret);

        // the purpose gate precedes the record fetch (no value path without a purpose)
        assertThat(body).as("reveal gates on a non-blank purpose")
            .contains("purpose.isBlank()").contains("purposeRequired()");
        int blankCheck = body.indexOf("purpose.isBlank()");
        assertThat(blankCheck).as("the blank-purpose 422 gate precedes the row write").isLessThan(persist);

        // the reveal is @Transactional (write + read share one unit → reveal-without-record rolls back)
        String beforeReveal = svc.substring(0, start);
        assertThat(beforeReveal.lastIndexOf("@Transactional"))
            .as("reveal must be @Transactional").isGreaterThan(beforeReveal.lastIndexOf("@Transactional(readOnly = true)"));
    }

    // ── SENSITIVE-MASK-001 — the default projection (get) writes NO access-log row ──
    @Test @Tag("SENSITIVE-MASK-001")
    void violation_defaultGet_writesNoAccessRow() throws Exception {
        String svc = Files.readString(SRC.resolve("SensitiveAccessService.java"));
        int start = svc.indexOf("public SensitiveRecord get(");
        assertThat(start).as("get method must exist").isPositive();
        String body = svc.substring(start, svc.indexOf("\n    }", start));
        assertThat(body).as("the masked get path must NOT write an access-log row")
            .doesNotContain("persistAndFlush").doesNotContain("members.persist");
    }

    // ── SENSITIVE-QUERY-001 — no delete path anywhere; the access-log query is ROLE_ADMIN-gated ──
    @Test @Tag("SENSITIVE-QUERY-001")
    void violation_noDeletePath_accessLogIsAdminOnly() throws Exception {
        for (Method m : SensitiveRecordRepository.class.getDeclaredMethods()) {
            assertThat(m.getName()).as("no delete method on the repository").doesNotContain("delete");
        }
        for (String src : new String[]{"SensitiveAccessService", "SensitiveAccessController"}) {
            String text = Files.readString(SRC.resolve(src + ".java"));
            assertThat(text).as(src + " must contain no delete call — records are closed, never removed")
                .doesNotContain(".delete(").doesNotContain("deleteBy");
        }
        String ctrl = Files.readString(SRC.resolve("SensitiveAccessController.java"));
        int logMethod = ctrl.indexOf("public List<AccessLogDto> accessLog(");
        assertThat(logMethod).as("accessLog endpoint must exist").isPositive();
        String preceding = ctrl.substring(ctrl.lastIndexOf("@GetMapping", logMethod), logMethod);
        assertThat(preceding).as("the access-log query MUST be ROLE_ADMIN-gated")
            .contains("@PreAuthorize").contains("ROLE_ADMIN");
    }

    // ── the migration carries the same shape (immutable trail, no UNIQUE that would block re-reveal) ──
    @Test @Tag("SENSITIVE-READ-001") @Tag("SENSITIVE-QUERY-001")
    void violation_migrationCarriesTheSameShape() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V058__create_sensitiveaccess.sql")) {
            assertThat(in).as("V058__create_sensitiveaccess.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            String norm = sql.replaceAll("\\s+", " ");
            assertThat(norm).contains("CREATE TABLE sensitive_records");
            assertThat(norm).contains("CREATE TABLE sensitive_access_logs");
            assertThat(norm).contains("purpose").contains("accessor").contains("occurred_at");
            // the access trail allows many rows per record — NO unique constraint on the log
            assertThat(norm.toUpperCase()).as("the access log must NOT be uniquely constrained (re-reveal is legal)")
                .doesNotContain("UNIQUE INDEX UQ_SENSITIVE").doesNotContain("UNIQUE (RECORD_ID");
        }
    }
}
