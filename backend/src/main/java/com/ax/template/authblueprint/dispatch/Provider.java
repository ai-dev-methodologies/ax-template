package com.ax.template.authblueprint.dispatch;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * dispatch supply unit (timed-offer-l0 AVAIL-FSM-001 / AVAIL-FRESH-002). Non-contended status
 * edges move only through {@link ProviderStateMachine} (no public setter); the contended
 * AVAILABLE→ASSIGNED claim is the atomic conditional UPDATE in {@link ProviderRepository#claim}
 * (exclusive-assignment-l0 EXCL-CLAIM-001). {@code id}/{@code handle}/{@code createdAt} immutable.
 */
@AggregateRoot
@Entity
@Table(name = "dispatch_providers")
public class Provider {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "handle", nullable = false, updatable = false, length = 120)
    private String handle;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ProviderStatus status;

    @Column(name = "last_heartbeat_at", nullable = false)
    private Instant lastHeartbeatAt;

    /** Optimistic-lock version — makes the timeout sweep LOSE the race against a live claim. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Provider() {}

    public Provider(UUID id, String handle, ProviderStatus status, Instant lastHeartbeatAt, Instant createdAt) {
        this.id = id;
        this.handle = handle;
        this.status = status;
        this.lastHeartbeatAt = lastHeartbeatAt;
        this.createdAt = createdAt;
    }

    /** Sole-mutator hook — package-private so only {@link ProviderStateMachine} advances status. */
    void setStatus(ProviderStatus next) {
        this.status = next;
    }

    /** Heartbeat update — package-private (only the service in this package touches it). */
    void setLastHeartbeatAt(Instant at) {
        this.lastHeartbeatAt = at;
    }

    public UUID getId() { return id; }
    public String getHandle() { return handle; }
    public ProviderStatus getStatus() { return status; }
    public Instant getLastHeartbeatAt() { return lastHeartbeatAt; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }

    /** AVAIL-FRESH-002 — offerable only while the heartbeat is within the staleness window. */
    public boolean isFresh(Instant now, Duration stalenessWindow) {
        return lastHeartbeatAt.isAfter(now.minus(stalenessWindow));
    }
}
