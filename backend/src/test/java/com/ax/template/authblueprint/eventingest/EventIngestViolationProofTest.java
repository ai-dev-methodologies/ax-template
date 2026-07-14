package com.ax.template.authblueprint.eventingest;

import jakarta.persistence.Column;
import jakarta.persistence.LockModeType;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for monotonic-event-ingest-l0. Structural assertions a deliberate break
 * cannot pass silently: the watermark row's identity columns are immutable and its watermark
 * mutator is package-private, the dedup ledger is fully append-only with a unique constraint,
 * the request DTO structurally cannot carry a client-supplied recorded_at, the row-lock read is
 * PESSIMISTIC_WRITE, NO delete path exists anywhere in the domain, and the migration carries the
 * same unique backstops.
 */
class EventIngestViolationProofTest {

    // ── INGEST-WATERMARK-001 — identity columns immutable; the watermark mutator is sealed ──
    @Test @Tag("EVENTINGEST") @Tag("INGEST-WATERMARK-001")
    void violation_identityColumnsImmutable_watermarkMutatorSealed() throws Exception {
        for (String f : new String[]{"id", "stream", "subjectId", "createdAt"}) {
            Column col = IngestState.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("IngestState." + f + " must be immutable").isFalse();
        }
        Method apply = Arrays.stream(IngestState.class.getDeclaredMethods())
            .filter(m -> m.getName().equals("apply")).findFirst().orElseThrow();
        assertThat(Modifier.isPublic(apply.getModifiers()))
            .as("IngestState.apply must be package-private (sole mutator)").isFalse();
        assertThat(IngestState.class.getDeclaredField("version")
            .isAnnotationPresent(jakarta.persistence.Version.class)).isTrue();
        jakarta.persistence.Table table = IngestState.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(table.uniqueConstraints()[0].columnNames()).containsExactly("stream", "subject_id");
    }

    // ── INGEST-IDEMPOTENT-APPLY-001 — the dedup ledger is fully append-only ──
    @Test @Tag("EVENTINGEST") @Tag("INGEST-IDEMPOTENT-APPLY-001")
    void violation_dedupLedgerFullyImmutable_uniqueOnStreamEventId() throws Exception {
        for (Method m : ProcessedEvent.class.getMethods()) {
            assertThat(m.getName()).as("ProcessedEvent must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"ingestStateId", "stream", "eventId", "appliedAt"}) {
            Column col = ProcessedEvent.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("ProcessedEvent." + f + " must be immutable").isFalse();
        }
        jakarta.persistence.Table table = ProcessedEvent.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(table.uniqueConstraints()[0].columnNames()).containsExactly("stream", "event_id");
    }

    // ── INGEST-CAPTURE-001 — the request shape structurally cannot carry a client recorded_at ──
    @Test @Tag("EVENTINGEST") @Tag("INGEST-CAPTURE-001")
    void violation_requestRecordHasNoRecordedAtComponent() {
        var components = EventIngestController.ApplyEventReq.class.getRecordComponents();
        assertThat(Arrays.stream(components).map(c -> c.getName()))
            .as("ApplyEventReq must not expose a client-settable recordedAt")
            .doesNotContain("recordedAt");
    }

    // ── INGEST-REJECT-STALE-001 — the row-lock finder is PESSIMISTIC_WRITE ──
    @Test @Tag("EVENTINGEST") @Tag("INGEST-REJECT-STALE-001")
    void violation_lockedFinderIsPessimisticWrite() throws Exception {
        Method locked = IngestStateRepository.class.getMethod("findByStreamAndSubjectIdForUpdate",
            String.class, String.class);
        Lock lock = locked.getAnnotation(Lock.class);
        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    // ── no delete path anywhere — the row is a projection, never erased ──
    @Test @Tag("EVENTINGEST") @Tag("INGEST-WATERMARK-001")
    void violation_noDeletePath() throws Exception {
        for (Method m : IngestStateRepository.class.getDeclaredMethods()) {
            assertThat(m.getName()).doesNotContain("delete");
        }
        for (String src : new String[]{"EventIngestService", "EventIngestController"}) {
            String text = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
                "com", "ax", "template", "authblueprint", "eventingest", src + ".java"));
            assertThat(text).as(src + " must contain no delete call")
                .doesNotContain(".delete(").doesNotContain("deleteBy");
        }
    }

    // ── the migration carries the same unique backstops ──
    @Test @Tag("EVENTINGEST") @Tag("INGEST-IDEMPOTENT-APPLY-001")
    void violation_migrationCarriesTheSameBackstops() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V098__create_event_ingest.sql")) {
            assertThat(in).as("V098__create_event_ingest.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("UNIQUE INDEX uq_ingest_state_stream_subject");
            assertThat(sql).contains("UNIQUE INDEX uq_processed_event_stream_eventid");
        }
    }
}
