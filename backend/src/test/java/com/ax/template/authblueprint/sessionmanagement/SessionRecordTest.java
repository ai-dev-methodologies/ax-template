package com.ax.template.authblueprint.sessionmanagement;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("SESSION")
class SessionRecordTest {

    @Test
    @Tag("SESS-LIFECYCLE-002")
    void isExpired_isPureClockPredicate() {
        Instant now = Instant.parse("2026-05-22T00:00:00Z");
        SessionRecord past = newRecord(now.minus(Duration.ofMinutes(1)));
        SessionRecord future = newRecord(now.plus(Duration.ofHours(1)));

        assertThat(past.isExpired(now)).isTrue();
        assertThat(future.isExpired(now)).isFalse();
    }

    @Test
    @Tag("SESS-LIFECYCLE-003")
    void markRevoked_isIdempotentAndPreservesOriginalTimestamp() {
        SessionRecord r = newRecord(Instant.now().plusSeconds(3600));
        Instant t1 = Instant.parse("2026-05-22T01:00:00Z");
        r.markRevoked(t1, "actor-1");

        Instant t2 = Instant.parse("2026-05-22T02:00:00Z");
        r.markRevoked(t2, "actor-2");

        assertThat(r.getStatus()).isEqualTo(SessionStatus.REVOKED);
        assertThat(r.getRevokedAt()).isEqualTo(t1);
        assertThat(r.getRevokedByUserId()).isEqualTo("actor-1");
    }

    private static SessionRecord newRecord(Instant expiresAt) {
        return SessionRecord.builder()
            .userId("u-1")
            .jti("jwt-jti-1")
            .deviceLabel("test")
            .status(SessionStatus.ACTIVE)
            .createdAt(Instant.parse("2026-05-22T00:00:00Z").minus(Duration.ofMinutes(5)))
            .expiresAt(expiresAt)
            .build();
    }
}
