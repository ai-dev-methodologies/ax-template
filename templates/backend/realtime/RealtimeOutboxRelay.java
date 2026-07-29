/**
 * @ax-template-meta
 * template_id: backend/realtime/RealtimeOutboxRelay
 * layer: backend-cross-cutting
 * anchors_rule: transactional-outbox-no-dual-write.md
 * provenance_class: internal_design
 * opt_in_via: ax.realtime.sse.enabled=true (blueprints/realtime-policy-manifest.yaml)
 * serverless_safe: false
 * evidence:
 *   - source_type: external
 *     citation: "Microservices.io — Transactional Outbox Pattern"
 *     url: "https://microservices.io/patterns/data/transactional-outbox.html"
 *   - source_type: external
 *     citation: "Spring Framework Reference — @Scheduled (Integration §6.3)"
 *     url: "https://docs.spring.io/spring-framework/reference/integration/scheduling.html"
 * usage: |
 *   Polls the realtime_outbox table every 1s and relays PENDING events to RealtimeEventBus.
 *   Uses the SP25 JobDispatcher pattern: DB-row outbox for durability, poll-and-relay for fanout.
 *   Opt-in: requires ax.realtime.sse.enabled=true (no-op when SSE disabled).
 *
 *   Schema (add via Flyway migration V<ts>__add_realtime_outbox.sql):
 *     CREATE TABLE realtime_outbox (
 *       id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 *       user_id     UUID NOT NULL,
 *       topic       VARCHAR(64) NOT NULL,
 *       payload     TEXT NOT NULL,
 *       status      VARCHAR(16) NOT NULL DEFAULT 'PENDING',
 *       created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
 *       relayed_at  TIMESTAMPTZ
 *     );
 *     CREATE INDEX idx_realtime_outbox_status ON realtime_outbox(status, created_at);
 */
package com.example.app.realtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * DB outbox → SSE bridge (opt-in, polling relay loop).
 *
 * <p>Activated only when SSE is opted in ({@code ax.realtime.sse.enabled=true}).
 * In the default polling configuration, this bean does not load.
 *
 * <p>Relay loop:
 * <ol>
 *   <li>Fetch up to {@link #BATCH_SIZE} PENDING rows from {@code realtime_outbox}.
 *   <li>For each row: call {@link RealtimeEventBus#publish} (fans out to active SSE emitters).
 *   <li>Mark row as RELAYED.
 * </ol>
 *
 * <p>Rows older than 5 minutes are dead-lettered (status=EXPIRED) to prevent backlog buildup.
 * This mirrors the SP25 JobDispatcher DLQ pattern.
 */
@Component
@ConditionalOnProperty(name = "ax.realtime.sse.enabled", havingValue = "true")
public class RealtimeOutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(RealtimeOutboxRelay.class);
    private static final int BATCH_SIZE = 100;
    private static final long EXPIRY_MINUTES = 5;

    private final JdbcTemplate jdbc;
    private final RealtimeEventBus eventBus;

    public RealtimeOutboxRelay(JdbcTemplate jdbc, RealtimeEventBus eventBus) {
        this.jdbc = jdbc;
        this.eventBus = eventBus;
    }

    /**
     * Main relay loop — runs every 1 second on the scheduling thread pool.
     * Processes PENDING rows and expires stale ones.
     */
    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void relay() {
        expireStaleRows();
        relayPendingRows();
    }

    // ─── private ─────────────────────────────────────────────────────────────

    private void relayPendingRows() {
        List<Map<String, Object>> rows = jdbc.queryForList("""
            SELECT id, user_id, topic, payload
            FROM realtime_outbox
            WHERE status = 'PENDING'
            ORDER BY created_at
            LIMIT ?
            """, BATCH_SIZE);

        for (Map<String, Object> row : rows) {
            String id      = row.get("id").toString();
            String userId  = row.get("user_id").toString();
            String topic   = row.get("topic").toString();
            String payload = row.get("payload").toString();

            try {
                eventBus.publish(userId, topic, payload);
                markRelayed(id);
            } catch (Exception ex) {
                log.warn("Relay failed for outbox row id={}: {}", id, ex.getMessage());
                // Row stays PENDING — will be retried on next tick (up to EXPIRY_MINUTES)
            }
        }
    }

    private void expireStaleRows() {
        int expired = jdbc.update("""
            UPDATE realtime_outbox
            SET status = 'EXPIRED'
            WHERE status = 'PENDING'
              AND created_at < NOW() - INTERVAL '%d minutes'
            """.formatted(EXPIRY_MINUTES));

        if (expired > 0) {
            log.warn("Expired {} stale realtime_outbox rows (>{} min old)", expired, EXPIRY_MINUTES);
        }
    }

    private void markRelayed(String id) {
        jdbc.update("""
            UPDATE realtime_outbox
            SET status = 'RELAYED', relayed_at = ?
            WHERE id = ?::uuid
            """, Instant.now(), id);
    }
}
