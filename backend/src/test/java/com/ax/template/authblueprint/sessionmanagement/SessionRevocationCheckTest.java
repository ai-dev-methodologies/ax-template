package com.ax.template.authblueprint.sessionmanagement;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SESS-REVOKE-003 — fail-closed SPI semantics.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("SESSION")
class SessionRevocationCheckTest {

    @Autowired SessionRevocationCheck spi;
    @Autowired SessionRecordRepository repository;
    @Autowired Clock clock;

    @Test
    @Tag("SESS-REVOKE-003")
    void unknownJti_failsClosed() {
        assertThat(spi.isRevoked("never-seen-jti")).isTrue();
    }

    @Test
    @Tag("SESS-REVOKE-003")
    void nullOrBlankJti_failsClosed() {
        assertThat(spi.isRevoked(null)).isTrue();
        assertThat(spi.isRevoked("")).isTrue();
        assertThat(spi.isRevoked("   ")).isTrue();
    }

    @Test
    @Tag("SESS-REVOKE-003")
    void activeFutureSession_returnsFalse() {
        SessionRecord row = repository.save(SessionRecord.builder()
            .userId("u-spi-1")
            .jti("spi-active-" + System.nanoTime())
            .status(SessionStatus.ACTIVE)
            .createdAt(Instant.now(clock))
            .expiresAt(Instant.now(clock).plus(Duration.ofHours(1)))
            .build());

        assertThat(spi.isRevoked(row.getJti())).isFalse();
    }

    @Test
    @Tag("SESS-REVOKE-003")
    void revokedSession_returnsTrue() {
        SessionRecord row = repository.save(SessionRecord.builder()
            .userId("u-spi-2")
            .jti("spi-revoked-" + System.nanoTime())
            .status(SessionStatus.ACTIVE)
            .createdAt(Instant.now(clock))
            .expiresAt(Instant.now(clock).plus(Duration.ofHours(1)))
            .build());
        row.markRevoked(Instant.now(clock), "u-spi-2");
        repository.save(row);

        assertThat(spi.isRevoked(row.getJti())).isTrue();
    }

    @Test
    @Tag("SESS-REVOKE-003")
    void expiredActiveSession_returnsTrue() {
        SessionRecord row = repository.save(SessionRecord.builder()
            .userId("u-spi-3")
            .jti("spi-expired-" + System.nanoTime())
            .status(SessionStatus.ACTIVE)
            .createdAt(Instant.now(clock).minus(Duration.ofHours(2)))
            .expiresAt(Instant.now(clock).minus(Duration.ofMinutes(1)))
            .build());

        assertThat(spi.isRevoked(row.getJti())).isTrue();
    }
}
