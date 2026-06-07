package com.ax.template.authblueprint.governedrecord;

import jakarta.persistence.Column;
import jakarta.persistence.Version;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * VIOLATION proof for attested-change-record-l0. Structural immutability + pure ReasonVocabulary +
 * migration backstops — no Spring context.
 */
@Tag("GOVERNEDRECORD")
class GovernedRecordViolationProofTest {

    // ── ACR-APPEND-ONLY-001 — change record fully immutable; governed field has no public setter ──
    @Test @Tag("ACR-APPEND-ONLY-001")
    void violation_changeRecordImmutable_governedFieldNoPublicSetter() throws Exception {
        // ChangeRecord: every column @Column(updatable=false), no public setter
        for (Method m : ChangeRecord.class.getMethods()) {
            assertThat(m.getName()).as("ChangeRecord must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "datumId", "fieldName", "sequenceNo", "oldValue", "newValue",
                "reason", "reasonVocabVersion", "actor", "occurredAt"}) {
            Column col = ChangeRecord.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("ChangeRecord." + f + " must be immutable").isFalse();
        }
        // GovernedDatum: value mutated only via package-private setValueInternal (NOT in getMethods)
        for (Method m : GovernedDatum.class.getMethods()) {
            assertThat(m.getName())
                .as("GovernedDatum must expose no public setter (value changes only through the change service)")
                .doesNotStartWith("set");
        }
        Field v = GovernedDatum.class.getDeclaredField("version");
        assertThat(v.isAnnotationPresent(Version.class)).as("GovernedDatum.version must carry @Version").isTrue();
        for (String f : new String[]{"id", "name", "createdBy", "createdAt"}) {
            assertThat(GovernedDatum.class.getDeclaredField(f).getAnnotation(Column.class).updatable())
                .as("GovernedDatum." + f + " immutable").isFalse();
        }
    }

    // ── ACR-VOCAB-001 — closed controlled vocabulary (empty => free-text; non-empty => members only) ──
    @Test @Tag("ACR-VOCAB-001")
    void violation_reasonVocabularyIsClosedWhenConfigured() {
        assertThat(ReasonVocabulary.isAllowed("anything", Set.of())).as("empty vocab => free-text").isTrue();
        Set<String> vocab = Set.of("transcription-error", "source-correction", "query-resolution", "unit-conversion");
        assertThat(ReasonVocabulary.isAllowed("transcription-error", vocab)).isTrue();
        assertThat(ReasonVocabulary.isAllowed("misc", vocab)).as("unknown code rejected (no misc bucket)").isFalse();
        assertThat(ReasonVocabulary.isAllowed("", vocab)).isFalse();
    }

    // ── ACR-VOCAB-001 — a configured vocabulary WITHOUT a pinned version fails fast (a present-but-empty
    //    version pin would defeat the reproducibility the spec requires) ──
    @Test @Tag("ACR-VOCAB-001")
    void violation_configuredVocabularyWithoutVersion_failsFast() {
        // vocab configured, version blank -> startup failure (never silently pins "")
        assertThatThrownBy(() -> new GovernedRecordService(null, null, null, Clock.systemUTC(),
                "transcription-error,source-correction", "  "))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("reason-vocabulary-version");
        // vocab configured WITH a version is fine; an empty vocab needs no version (free-text mode)
        assertThat(new GovernedRecordService(null, null, null, Clock.systemUTC(),
                "transcription-error", "v2026.1")).isNotNull();
        assertThat(new GovernedRecordService(null, null, null, Clock.systemUTC(), "", "")).isNotNull();
    }

    // ── ACR-ENVELOPE-001 — migration declares the non-blank-reason CHECK + monotonic-sequence uniqueness ──
    @Test @Tag("ACR-ENVELOPE-001")
    void violation_migrationDeclaresReasonCheckAndSequenceUnique() throws Exception {
        String sql;
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("db/migration/V037__create_governed_records.sql")) {
            assertThat(in).as("V037 migration must be on the classpath").isNotNull();
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertThat(sql).contains("chk_governed_change_reason");
        assertThat(sql).contains("LENGTH(TRIM(reason)) > 0");
        assertThat(sql).contains("uq_governed_change_seq");
    }
}
