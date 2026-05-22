package com.ax.template.authblueprint.sessionmanagement;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.ax.template.authblueprint.sessionmanagement.SessionDtos.RegisterSessionRequest;
import com.ax.template.authblueprint.sessionmanagement.SessionDtos.RevokeOthersResponse;
import com.ax.template.authblueprint.sessionmanagement.SessionDtos.SessionListResponse;
import com.ax.template.authblueprint.sessionmanagement.SessionDtos.SessionResponse;

/**
 * Orchestrates the session-management lifecycle.
 *
 * <p>Trace:
 * <ul>
 *   <li>SESS-LIFECYCLE-001 — register is idempotent on (userId, jti)</li>
 *   <li>SESS-LIFECYCLE-003 — logout flips status, never hard-deletes</li>
 *   <li>SESS-REVOKE-001 — user self-revoke (idempotent)</li>
 *   <li>SESS-REVOKE-002 — bulk revoke-others via repository UPDATE</li>
 *   <li>SESS-AUTHZ-002 — every lookup uses (id, userId)</li>
 *   <li>SESS-AUTHZ-003 — admin force-logout records actor in revokedByUserId</li>
 * </ul>
 */
@Service
public class SessionService {

    private final SessionRecordRepository repository;
    private final SessionManagementProperties properties;
    private final Clock clock;

    public SessionService(SessionRecordRepository repository,
                          SessionManagementProperties properties,
                          Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public RegisterResult register(String userId, RegisterSessionRequest request) {
        Instant now = Instant.now(clock);
        // SESS-LIFECYCLE-004 — expiresAt MUST be in the future. A past expiresAt would
        // produce a born-revoked session that confuses the UI ("ACTIVE but expired").
        if (request.expiresAt() == null || !request.expiresAt().isAfter(now)) {
            throw new ExpiresAtInPastException(
                "expiresAt must be strictly in the future (got " + request.expiresAt() + ", now=" + now + ")");
        }

        return repository.findByUserIdAndJti(userId, request.jti())
            .map(existing -> new RegisterResult(SessionResponse.from(existing, clock), false))
            .orElseGet(() -> {
                // SESS-LIFECYCLE-005 — enforce the per-user active cap by revoking the
                // oldest ACTIVE session BEFORE inserting the new one. The cap is soft —
                // we don't reject the request; we make room for it.
                enforceMaxActiveSessions(userId, now);
                SessionRecord row = SessionRecord.builder()
                    .userId(userId)
                    .jti(request.jti())
                    .deviceLabel(request.deviceLabel())
                    .ipAddress(request.ipAddress())
                    .userAgent(request.userAgent())
                    .status(SessionStatus.ACTIVE)
                    .createdAt(now)
                    .expiresAt(request.expiresAt())
                    .build();
                SessionRecord saved = repository.save(row);
                return new RegisterResult(SessionResponse.from(saved, clock), true);
            });
    }

    private void enforceMaxActiveSessions(String userId, Instant now) {
        int cap = properties.getMaxActiveSessionsPerUser();
        if (cap <= 0) {
            return;
        }
        java.util.List<SessionRecord> active = repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .filter(s -> s.getStatus() == SessionStatus.ACTIVE && !s.isExpired(now))
            .sorted(java.util.Comparator.comparing(SessionRecord::getCreatedAt))
            .toList();
        int overage = active.size() - (cap - 1);  // need room for the about-to-insert row
        for (int i = 0; i < overage; i++) {
            SessionRecord oldest = active.get(i);
            oldest.markRevoked(now, userId);
            repository.save(oldest);
        }
    }

    @Transactional(readOnly = true)
    public SessionListResponse listMine(String userId) {
        List<SessionRecord> rows = repository.findByUserIdOrderByCreatedAtDesc(userId);
        List<SessionResponse> items = rows.stream().map(s -> SessionResponse.from(s, clock)).toList();
        return new SessionListResponse(items, items.size());
    }

    @Transactional(readOnly = true)
    public SessionResponse getMine(String userId, UUID id) {
        return SessionResponse.from(loadOwn(userId, id), clock);
    }

    @Transactional
    public void revokeMine(String userId, UUID id) {
        SessionRecord row = loadOwn(userId, id);
        row.markRevoked(Instant.now(clock), userId);
        repository.save(row);
    }

    @Transactional
    public RevokeOthersResponse revokeOthers(String userId, UUID keepId) {
        int activeBefore = (int) repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .filter(s -> s.getStatus() == SessionStatus.ACTIVE).count();
        int revoked = repository.revokeOthers(userId, keepId, Instant.now(clock), userId);
        // 'kept' is the count of ACTIVE sessions remaining — the keep session if it was already ACTIVE.
        int kept = activeBefore - revoked;
        return new RevokeOthersResponse(revoked, kept);
    }

    @Transactional
    public void heartbeat(String userId, UUID id) {
        SessionRecord row = loadOwn(userId, id);
        row.touchLastSeen(Instant.now(clock));
        repository.save(row);
    }

    /** SESS-AUTHZ-003 — admin force-logout. Loads by id (no userId filter); records actor. */
    @Transactional
    public void adminRevoke(String adminUserId, UUID id) {
        SessionRecord row = repository.findById(id)
            .orElseThrow(() -> new SessionNotFoundException(id));
        row.markRevoked(Instant.now(clock), adminUserId);
        repository.save(row);
    }

    private SessionRecord loadOwn(String userId, UUID id) {
        return repository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new SessionNotFoundException(id));
    }

    public record RegisterResult(SessionResponse response, boolean created) {}
}
