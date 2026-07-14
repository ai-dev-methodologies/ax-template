package com.ax.template.authblueprint.bilateralhandoff;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * bilateral-handoff-l0 root: a handoff PROPOSED between two NAMED parties — a releasor (current
 * custodian) and a receiver (incoming custodian), both immutable after creation (BHO-FSM-001).
 * Each party's confirmation is recorded independently; status moves ONLY through
 * {@link HandoffStateMachine} (no public setter) — COMPLETED and VOIDED are both terminal (zero
 * outgoing edges). custodyHolder starts at the releasor and flips to the receiver EXACTLY ONCE,
 * atomically with the COMPLETED transition (BHO-ATOMIC-001) — never on a single confirmation,
 * never for a VOIDED handoff even if one party had confirmed before the decline (BHO-VOID-001).
 * The @Check backstops make a COMPLETED row with a missing confirmation, or a custody holder that
 * is neither named party, unrepresentable.
 */
@AggregateRoot
@Entity
@Table(name = "handoffs")
@Check(constraints =
    "(status <> 'COMPLETED' OR (releasor_confirmed_at IS NOT NULL AND receiver_confirmed_at IS NOT NULL))"
    + " AND (custody_holder = releasor_party OR custody_holder = receiver_party)")
public class Handoff {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "releasor_party", nullable = false, updatable = false, length = 200)
    private String releasorParty;

    @Column(name = "receiver_party", nullable = false, updatable = false, length = 200)
    private String receiverParty;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private HandoffStatus status;

    /** BHO-ATOMIC-001 — starts at releasorParty; flips to receiverParty exactly once, at COMPLETED. */
    @Column(name = "custody_holder", nullable = false, length = 200)
    private String custodyHolder;

    @Column(name = "releasor_confirmed_at")
    private Instant releasorConfirmedAt;

    @Column(name = "receiver_confirmed_at")
    private Instant receiverConfirmedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Handoff() {}

    public Handoff(UUID id, String releasorParty, String receiverParty, Instant createdAt) {
        this.id = id;
        this.releasorParty = releasorParty;
        this.receiverParty = receiverParty;
        this.status = HandoffStatus.PROPOSED;
        this.custodyHolder = releasorParty;
        this.createdAt = createdAt;
    }

    public boolean isParty(String caller) {
        return releasorParty.equals(caller) || receiverParty.equals(caller);
    }

    /** BHO-BIND-001 — per-party idempotent confirm hook; a no-op if this party already confirmed. */
    void markReleasorConfirmed(Instant at) {
        if (this.releasorConfirmedAt == null) {
            this.releasorConfirmedAt = at;
        }
    }

    void markReceiverConfirmed(Instant at) {
        if (this.receiverConfirmedAt == null) {
            this.receiverConfirmedAt = at;
        }
    }

    public boolean bothConfirmed() {
        return releasorConfirmedAt != null && receiverConfirmedAt != null;
    }

    /** Sole-mutator hook — BHO-FSM/ATOMIC-001: status → COMPLETED, custody flips, in one write. */
    void complete() {
        this.status = HandoffStatus.COMPLETED;
        this.custodyHolder = receiverParty;
    }

    /** Sole-mutator hook — BHO-VOID-001: status → VOIDED terminally; custody never flips. */
    void voidHandoff() {
        this.status = HandoffStatus.VOIDED;
    }

    public UUID getId() { return id; }
    public String getReleasorParty() { return releasorParty; }
    public String getReceiverParty() { return receiverParty; }
    public HandoffStatus getStatus() { return status; }
    public String getCustodyHolder() { return custodyHolder; }
    public Instant getReleasorConfirmedAt() { return releasorConfirmedAt; }
    public Instant getReceiverConfirmedAt() { return receiverConfirmedAt; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
