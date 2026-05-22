package com.ax.template.authblueprint.sessionmanagement;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/**
 * Default fail-closed implementation. Trace: SESS-REVOKE-003.
 */
@Component
public class DefaultSessionRevocationCheck implements SessionRevocationCheck {

    private final SessionRecordRepository repository;
    private final Clock clock;

    public DefaultSessionRevocationCheck(SessionRecordRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public boolean isRevoked(String jti) {
        if (jti == null || jti.isBlank()) {
            return true;  // fail-closed
        }
        return repository.findByJti(jti)
            .map(s -> s.getStatus() == SessionStatus.REVOKED || s.isExpired(Instant.now(clock)))
            .orElse(true);  // unknown jti → fail-closed
    }
}
