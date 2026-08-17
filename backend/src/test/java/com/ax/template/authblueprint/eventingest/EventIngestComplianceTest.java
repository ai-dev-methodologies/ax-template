package com.ax.template.authblueprint.eventingest;

import io.micrometer.core.instrument.MeterRegistry;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * monotonic-event-ingest-l0 compliance — verified against the live eventingest reference
 * workload. The invariant: the authoritative row's watermark only ever advances; a
 * behind-the-watermark or exact-replay event is ack'd-dropped-and-counted, never rejected as an
 * error; recorded_at is always server-assigned, never client-supplied.
 * Spec: specs/monotonic-event-ingest-l0.yaml (Flink event-time/watermark + Idempotent Consumer).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("EVENTINGEST")
class EventIngestComplianceTest {

    @LocalServerPort int port;
    @Autowired IngestStateRepository states;
    @Autowired MeterRegistry meterRegistry;
    String member;

    @BeforeEach
    void setup() {
        member = EventIngestTestSupport.obtainToken(EventIngestTestSupport.freshEmail("ei-member"), "MEMBER");
    }

    private ExtractableResponse<Response> apply(String subjectId, String eventId, Instant occurredAt,
                                                Instant capturedAt, String stateValue) {
        String body = "{\"stream\":\"SHIPMENT_STATUS\",\"subjectId\":\"" + subjectId
            + "\",\"eventId\":\"" + eventId + "\",\"occurredAt\":\"" + occurredAt
            + "\",\"capturedAt\":\"" + capturedAt + "\",\"stateValue\":\"" + stateValue + "\"}";
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body(body)
        .when().post("/api/event-ingest/events").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> get(String subjectId) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/event-ingest/streams/SHIPMENT_STATUS/subjects/" + subjectId)
            .then().statusCode(200).extract();
    }

    // ── INGEST-WATERMARK-001 — the watermark holds the LATER event-time, not last-received ──
    @Test @Tag("INGEST-WATERMARK-001")
    void watermark_holdsLaterEventTime_notLastReceived() {
        String subject = "ship-" + UUID.randomUUID();
        Instant t1 = Instant.parse("2026-01-01T00:00:00Z");
        Instant t2 = Instant.parse("2026-01-02T00:00:00Z");

        // post the LATER event first, then an EARLIER one (reverse event-time arrival order)
        assertThat(apply(subject, "evt-late-arrival-1", t2, t2, "DELIVERED").statusCode()).isEqualTo(200);
        apply(subject, "evt-late-arrival-2", t1, t1, "OUT_FOR_DELIVERY");

        ExtractableResponse<Response> state = get(subject);
        assertThat(state.jsonPath().getString("lastAppliedOccurredAt")).isEqualTo(t2.toString());
        assertThat(state.jsonPath().getString("stateValue")).isEqualTo("DELIVERED");
    }

    // ── INGEST-REJECT-STALE-001 — a late event is ack'd (200), not an error, and never rewinds ──
    @Test @Tag("INGEST-REJECT-STALE-001")
    void staleEvent_isAckdNotError_andRowStaysUnchanged() {
        String subject = "ship-" + UUID.randomUUID();
        Instant t1 = Instant.parse("2026-02-01T00:00:00Z");
        Instant t2 = Instant.parse("2026-02-02T00:00:00Z");

        apply(subject, "evt-a", t2, t2, "DELIVERED");
        ExtractableResponse<Response> late = apply(subject, "evt-b", t1, t1, "OUT_FOR_DELIVERY");
        assertThat(late.statusCode()).isEqualTo(200);                  // ack'd, never a 5xx/error

        ExtractableResponse<Response> state = get(subject);
        assertThat(state.jsonPath().getString("lastAppliedOccurredAt")).isEqualTo(t2.toString());
        assertThat(state.jsonPath().getString("stateValue")).isEqualTo("DELIVERED");
    }

    // ── INGEST-IDEMPOTENT-APPLY-001 — replay is a no-op; exactly one dedup-ledger row ──
    @Test @Tag("INGEST-IDEMPOTENT-APPLY-001")
    void replayedEvent_isNoOp_exactlyOneDedupLedgerRow() {
        String subject = "ship-" + UUID.randomUUID();
        Instant t = Instant.parse("2026-03-01T00:00:00Z");

        apply(subject, "evt-once", t, t, "DELIVERED");
        ExtractableResponse<Response> replay = apply(subject, "evt-once", t, t, "TAMPERED_STATE");
        assertThat(replay.statusCode()).isEqualTo(200);

        ExtractableResponse<Response> state = get(subject);
        assertThat(state.jsonPath().getString("stateValue")).isEqualTo("DELIVERED");  // no second mutation
        assertThat(states.countProcessedEvent("SHIPMENT_STATUS", "evt-once")).isEqualTo(1L);
    }

    /**
     * INGEST-IDEMPOTENT-APPLY-001 keystone — the existsProcessedEvent read is a fast-path check,
     * not a lock, so two concurrent deliveries of the SAME (stream, event_id) can both pass it
     * before either commits. Both requests MUST still ack 200 (never an unmapped 500 from the
     * loser's unique-constraint race) and exactly ONE dedup-ledger row must exist afterward.
     */
    @Test @Tag("INGEST-IDEMPOTENT-APPLY-001")
    void concurrentExactDuplicateDelivery_bothAck200_exactlyOneDedupRow() throws Exception {
        String subject = "ship-" + UUID.randomUUID();
        Instant t = Instant.parse("2026-03-15T00:00:00Z");
        int n = 2;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<Integer> statusCodes = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < n; i++) {
            pool.submit(() -> {
                start.await();
                statusCodes.add(apply(subject, "evt-race", t, t, "DELIVERED").statusCode());
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(statusCodes).as("neither racer surfaces an unmapped 500").allMatch(code -> code == 200);
        assertThat(states.countProcessedEvent("SHIPMENT_STATUS", "evt-race")).isEqualTo(1L);
    }

    // ── INGEST-OBSERVABILITY-001 — stale_event_dropped_total{stream,reason} counts each drop ──
    @Test @Tag("INGEST-OBSERVABILITY-001")
    void droppedEvents_incrementBoundedCardinalityCounter() {
        String subject = "ship-" + UUID.randomUUID();
        Instant t1 = Instant.parse("2026-04-01T00:00:00Z");
        Instant t2 = Instant.parse("2026-04-02T00:00:00Z");
        apply(subject, "evt-x", t2, t2, "DELIVERED");

        double beforeStale = counter("behind_watermark");
        double beforeDup = counter("duplicate");

        apply(subject, "evt-y", t1, t1, "OUT_FOR_DELIVERY");            // behind watermark
        apply(subject, "evt-x", t2, t2, "DELIVERED");                   // exact replay

        assertThat(counter("behind_watermark")).isEqualTo(beforeStale + 1);
        assertThat(counter("duplicate")).isEqualTo(beforeDup + 1);
    }

    private double counter(String reason) {
        var c = meterRegistry.find(EventIngestMetrics.STALE_EVENT_DROPPED_TOTAL)
            .tag("reason", reason).counter();
        return c == null ? 0.0 : c.count();
    }

    // ── INGEST-CAPTURE-001 — the third timestamp axis, fail-closed, server-assigned ──
    @Test @Tag("INGEST-CAPTURE-001")
    void captureOrder_isValidatedFailClosed_recordedAtIsServerAssigned() {
        String subject = "ship-" + UUID.randomUUID();
        Instant now = Instant.now();

        // occurred_at far ahead of captured_at (beyond tolerance) → 422
        ExtractableResponse<Response> badOccurred =
            apply(subject, "evt-bad-1", now.plusSeconds(3600), now, "DELIVERED");
        assertThat(badOccurred.statusCode()).isEqualTo(422);
        assertThat(badOccurred.jsonPath().getString("code")).isEqualTo("INGEST_CAPTURE_ORDER_INVALID");

        // captured_at claiming a time after the server's own recorded_at (the future) → 422
        ExtractableResponse<Response> badCaptured =
            apply(subject, "evt-bad-2", now, now.plusSeconds(3600), "DELIVERED");
        assertThat(badCaptured.statusCode()).isEqualTo(422);
        assertThat(badCaptured.jsonPath().getString("code")).isEqualTo("INGEST_CAPTURE_ORDER_INVALID");

        // a well-formed event applies; recorded_at is server-set (never echoes a client value —
        // the request shape carries no recordedAt field at all)
        ExtractableResponse<Response> ok = apply(subject, "evt-ok", now, now, "DELIVERED");
        assertThat(ok.statusCode()).isEqualTo(200);
        Instant recordedAt = Instant.parse(ok.jsonPath().getString("lastAppliedRecordedAt"));
        assertThat(recordedAt).isAfterOrEqualTo(now.minusSeconds(1));
    }
}
