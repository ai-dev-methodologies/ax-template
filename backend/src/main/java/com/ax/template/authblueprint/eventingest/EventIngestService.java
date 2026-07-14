package com.ax.template.authblueprint.eventingest;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * monotonic-event-ingest-l0 sole orchestrator. Folds an out-of-order, at-least-once external
 * event stream into an authoritative current-state row: the watermark only ever ADVANCES
 * (INGEST-WATERMARK-001), a behind-the-watermark event is ack'd-dropped-and-counted rather than
 * treated as an error (INGEST-REJECT-STALE-001), an exact replay is deduped through the
 * ProcessedEvent ledger before any second side effect (INGEST-IDEMPOTENT-APPLY-001), and
 * {@code recorded_at} is ALWAYS this service's own clock — never a client-supplied value
 * (INGEST-CAPTURE-001). The row's PESSIMISTIC_WRITE lock is the serialization point for a
 * webhook-retry race on the same (stream, subject).
 */
@Service
public class EventIngestService {

    /** Configurable clock-skew tolerance for occurred_at <= captured_at (INGEST-CAPTURE-001). */
    static final Duration CLOCK_SKEW_TOLERANCE = Duration.ofSeconds(5);

    private final IngestStateRepository states;
    private final IngestStateCreator creator;
    private final MemberWriter members;
    private final EventIngestMetrics metrics;
    private final Clock clock;

    public EventIngestService(IngestStateRepository states, IngestStateCreator creator,
                              MemberWriter members, EventIngestMetrics metrics, Clock clock) {
        this.states = states;
        this.creator = creator;
        this.members = members;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public IngestState apply(IngestStream stream, String subjectId, String eventId,
                             Instant occurredAt, Instant capturedAt, String stateValue) {
        String streamName = stream.name();
        Instant recordedAt = Instant.now(clock);                        // server clock — never client-trusted

        // INGEST-CAPTURE-001 — occurred_at <= captured_at (tolerant) <= recorded_at (strict)
        if (occurredAt.isAfter(capturedAt.plus(CLOCK_SKEW_TOLERANCE)) || capturedAt.isAfter(recordedAt)) {
            throw EventIngestException.captureOrderInvalid();
        }

        // INGEST-IDEMPOTENT-APPLY-001 — dedup FIRST: an exact replay never re-runs a side effect,
        // whether or not it also happens to sit at/behind the current watermark.
        if (states.existsProcessedEvent(streamName, eventId)) {
            metrics.dropped(streamName, "duplicate");
            return states.findByStreamAndSubjectId(streamName, subjectId)
                .orElseThrow(EventIngestException::notFound);
        }

        IngestState row = states.findByStreamAndSubjectIdForUpdate(streamName, subjectId).orElse(null);

        // INGEST-REJECT-STALE-001 — behind-or-at the watermark is dropped-but-counted, never an error.
        if (row != null && !occurredAt.isAfter(row.getLastAppliedOccurredAt())) {
            metrics.dropped(streamName, "behind_watermark");
            recordProcessedEventIdempotently(row.getId(), streamName, eventId, recordedAt);
            return row;                                                  // ack'd, row unchanged
        }

        IngestState result;
        if (row == null) {
            // No existing row means findByStreamAndSubjectIdForUpdate had nothing to LOCK, so
            // this is the one gap the row lock cannot close: two concurrent first-ever deliveries
            // for the SAME (stream, subject) can both reach here and race to insert. The attempt
            // runs in IngestStateCreator's OWN REQUIRES_NEW transaction — see its javadoc for why
            // a plain try/flush/catch in THIS transaction is not enough (a caught flush failure
            // still leaves the doomed entity enlisted in THIS session, corrupting the very
            // fallback read below).
            try {
                result = creator.tryCreate(new IngestState(UUID.randomUUID(), streamName, subjectId,
                    stateValue, eventId, occurredAt, capturedAt, recordedAt, Instant.now(clock)));
            } catch (DataIntegrityViolationException e) {
                // INGEST-IDEMPOTENT-APPLY-001 — a concurrent racer already created this row for
                // the SAME exact event; fall back to whatever they committed and treat this call
                // as the idempotent ack it is (their ProcessedEvent row already covers dedup).
                metrics.dropped(streamName, "duplicate");
                return states.findByStreamAndSubjectId(streamName, subjectId)
                    .orElseThrow(EventIngestException::notFound);
            }
        } else {
            // An EXISTING row IS serialized by the PESSIMISTIC_WRITE lock above — a concurrent
            // exact-duplicate against it is routed into the behind_watermark branch instead
            // (the winner's commit advances the watermark to exactly this occurredAt before the
            // loser re-checks), which is already protected by recordProcessedEventIdempotently.
            row.apply(stateValue, eventId, occurredAt, capturedAt, recordedAt);
            result = row;
        }
        recordProcessedEventIdempotently(result.getId(), streamName, eventId, recordedAt);
        return result;
    }

    /**
     * INGEST-IDEMPOTENT-APPLY-001 — the {@code existsProcessedEvent} read above is a fast-path
     * check, not a lock: two concurrent deliveries of the SAME (stream, event_id) can both pass
     * it before either has committed (TOCTOU). The `uq_processed_event_stream_eventid` unique
     * constraint is the real backstop a race cannot slip past — the loser's insert fails here,
     * and that failure is caught and treated as the graceful idempotent ack it actually is
     * (mirroring GovernedRecordService's persistAndFlush-then-catch pattern), never surfaced as
     * an unmapped 500. persistAndFlush (not persist) is required so the constraint violation
     * happens INSIDE this try block, not at end-of-transaction commit after this method returns.
     */
    private void recordProcessedEventIdempotently(UUID ingestStateId, String streamName, String eventId,
                                                  Instant recordedAt) {
        try {
            members.persistAndFlush(new ProcessedEvent(UUID.randomUUID(), ingestStateId, streamName,
                eventId, recordedAt));
        } catch (DataIntegrityViolationException e) {
            metrics.dropped(streamName, "duplicate");
        }
    }

    @Transactional(readOnly = true)
    public IngestState get(IngestStream stream, String subjectId) {
        return states.findByStreamAndSubjectId(stream.name(), subjectId)
            .orElseThrow(EventIngestException::notFound);
    }
}
