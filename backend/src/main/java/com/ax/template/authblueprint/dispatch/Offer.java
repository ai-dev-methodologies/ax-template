package com.ax.template.authblueprint.dispatch;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * Timed push-offer (timed-offer-l0 OFFER-FSM-001). At most one PENDING offer per request
 * (partial unique index on request_id WHERE status='PENDING'). Status advances only through
 * {@link OfferStateMachine} (no public setter). {@code requestId}/{@code providerId}/
 * {@code expiresAt}/{@code ordinal}/{@code createdAt} immutable. {@code @Version} makes the
 * sweep LOSE the race against a live accept (timeout-sweep-is-a-concurrent-mutator).
 */
@AggregateRoot
@Entity
@Table(name = "dispatch_offers")
public class Offer {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "request_id", nullable = false, updatable = false)
    private UUID requestId;

    @Column(name = "provider_id", nullable = false, updatable = false)
    private UUID providerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private OfferStatus status;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    /** Strictly monotonic cascade position (1,2,3,…) — OFFER-CASCADE-004 auditable ordinal. */
    @Column(name = "ordinal", nullable = false, updatable = false)
    private int ordinal;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Offer() {}

    public Offer(UUID id, UUID requestId, UUID providerId, Instant expiresAt, int ordinal, Instant createdAt) {
        this.id = id;
        this.requestId = requestId;
        this.providerId = providerId;
        this.status = OfferStatus.PENDING;
        this.expiresAt = expiresAt;
        this.ordinal = ordinal;
        this.createdAt = createdAt;
    }

    /** Sole-mutator hook — package-private so only {@link OfferStateMachine} advances status. */
    void setStatus(OfferStatus next) {
        this.status = next;
    }

    public UUID getId() { return id; }
    public UUID getRequestId() { return requestId; }
    public UUID getProviderId() { return providerId; }
    public OfferStatus getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
    public int getOrdinal() { return ordinal; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }

    /** OFFER-TOCTOU-003 — accept is valid only while PENDING and strictly before the deadline. */
    public boolean isAcceptableAt(Instant now) {
        return status == OfferStatus.PENDING && now.isBefore(expiresAt);
    }
}
