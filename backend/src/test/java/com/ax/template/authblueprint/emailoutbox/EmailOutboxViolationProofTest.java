package com.ax.template.authblueprint.emailoutbox;

import jakarta.persistence.Column;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * R51 — VIOLATION proof tests for email-outbox L4. Mirrors R31..R36 / R48 /
 * R49 convention. Structural invariants that would re-open an audit /
 * retry-loop / data-loss surface if relaxed.
 */
@Tag("EMAIL")
class EmailOutboxViolationProofTest {

    @Test
    @Tag("EMAIL-QUEUE-001")
    void violation_immutableContentColumns() throws Exception {
        // The original (recipient, subject, body, templateCode, createdAt)
        // MUST be immutable after enqueue — re-attributing a send to a
        // different recipient / body / template would falsify the audit
        // trail of "what we tried to deliver to whom".
        for (String name : new String[] { "recipient", "templateCode", "subject", "body", "createdAt" }) {
            Field f = EmailOutbox.class.getDeclaredField(name);
            Column c = f.getAnnotation(Column.class);
            assertThat(c).as("EmailOutbox." + name + " missing @Column").isNotNull();
            assertThat(c.updatable())
                .as("EmailOutbox." + name + " MUST be @Column(updatable=false) — outbox content "
                  + "must reflect the original enqueue, not a later operator-edited string")
                .isFalse();
        }
    }

    @Test
    @Tag("EMAIL-SEND-001")
    void violation_noPublicSetters() {
        for (var m : EmailOutbox.class.getDeclaredMethods()) {
            String name = m.getName();
            // markSent / markFailure / resetForRetry are state machine mutators —
            // they MUST NOT be public (service is the sole mutator).
            if (name.startsWith("set") || name.startsWith("mark") || name.startsWith("reset")) {
                assertThat(Modifier.isPublic(m.getModifiers()))
                    .as("EmailOutbox." + name + " must NOT be public — sole mutator is EmailOutboxService")
                    .isFalse();
            }
        }
    }

    @Test
    @Tag("EMAIL-RETRY-001")
    void violation_maxRetriesIsTerminalThreshold() {
        // The catalog policy is "after 3 consecutive failures, DLQ". Changing
        // MAX_RETRIES upward without an explicit catalog amendment would
        // change the per-row retry surface — operators rely on the constant
        // staying small and bounded so failed sends do not spam partners.
        assertThat(EmailOutbox.MAX_RETRIES)
            .as("MAX_RETRIES is the EMAIL-RETRY-001 terminal threshold; widening it must be a "
              + "catalog-level decision with a recorded blueprint update, not an inline tweak")
            .isEqualTo(3);
    }

    @Test
    @Tag("EMAIL-SEND-002")
    void violation_exponentialBackoffFormula() {
        // 2^retryCount × 30s. retryCount=1 → 60s. retryCount=2 → 120s.
        // The shape (powers of 2 × baseline) is the catalog SLA contract;
        // adopting a different schedule would change downstream incident
        // response expectations.
        java.time.Instant base = java.time.Instant.parse("2026-05-25T10:00:00Z");
        EmailOutbox row = EmailOutbox.create("u@x.kr", "c", "s", "b", base);
        row.markFailure("e", base, d -> base.plusSeconds(d));
        assertThat(row.getNextAttemptAt()).isEqualTo(base.plusSeconds(60));   // 2^1 × 30
        row.markFailure("e", base, d -> base.plusSeconds(d));
        assertThat(row.getNextAttemptAt()).isEqualTo(base.plusSeconds(120));  // 2^2 × 30
    }

    @Test
    @Tag("EMAIL-RETRY-001")
    void violation_markFailureSetsLastFailureAt_resetForRetryClears() {
        // R84 (F4 closure) — lastFailureAt MUST track the wall-clock moment
        // of the most recent failure, and MUST reset to null when an admin
        // triggers retry (resetForRetry restarts the failure clock).
        java.time.Instant base = java.time.Instant.parse("2026-05-26T10:00:00Z");
        EmailOutbox row = EmailOutbox.create("u@x.kr", "c", "s", "b", base);
        assertThat(row.getLastFailureAt())
            .as("freshly-enqueued PENDING row has no failure timestamp")
            .isNull();

        row.markFailure("e", base.plusSeconds(60), d -> base.plusSeconds(d));
        assertThat(row.getLastFailureAt())
            .as("markFailure stamps lastFailureAt with the supplied now")
            .isEqualTo(base.plusSeconds(60));

        row.markFailure("e", base.plusSeconds(120), d -> base.plusSeconds(d));
        assertThat(row.getLastFailureAt())
            .as("subsequent markFailure overwrites with the latest moment")
            .isEqualTo(base.plusSeconds(120));

        row.resetForRetry();
        assertThat(row.getLastFailureAt())
            .as("admin retry clears the failure clock so the next failure starts fresh")
            .isNull();
    }
}
