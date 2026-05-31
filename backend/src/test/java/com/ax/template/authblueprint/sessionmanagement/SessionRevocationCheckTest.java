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
// R22 aggregate-test isolation: with 88+ @SpringBootTest classes the TestContext ContextCache
// (cap 32) churns its LRU; this default-properties MOCK context can be evicted (its Hikari pool
// shut down) before its methods run, surfacing as 4x UndeclaredThrowableException at SPI proxy
// invocation. The realtime-policy RANDOM_PORT compliance test (added 2026-05-31) is the trigger
// that tips the eviction onto THIS class specifically. BEFORE_CLASS forces a fresh context boot
// immediately before this class's methods — the documented surgical R22 lever already applied to
// BillingFlowIT / FeatureFlagFlowIT / ApiKeyComplianceTest / I18nPolicyComplianceTest. No
// production change; this class's behavior is identical, it just no longer reuses an evicted context.
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
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
