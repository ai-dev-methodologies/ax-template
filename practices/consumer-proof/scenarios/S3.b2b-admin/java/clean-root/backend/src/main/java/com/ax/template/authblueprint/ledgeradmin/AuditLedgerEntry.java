package com.ax.template.authblueprint.ledgeradmin;

import java.time.Instant;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * AuditLedgerEntry — one immutable append-only ledger row surfaced to the
 * B2B admin ledger view. Composes the audit-log L4 domain's append-only
 * posture (practices/rules/audit-log-pii-hash-required.md): actor identity
 * is a hash, never a raw PII field.
 */
@Entity
public class AuditLedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String actorHash;
    private String action;
    private String entityRef;
    private Instant occurredAt;

    protected AuditLedgerEntry() {
    }

    public AuditLedgerEntry(String actorHash, String action, String entityRef, Instant occurredAt) {
        this.actorHash = actorHash;
        this.action = action;
        this.entityRef = entityRef;
        this.occurredAt = occurredAt;
    }

    public Long getId() {
        return id;
    }

    public String getActorHash() {
        return actorHash;
    }

    public String getAction() {
        return action;
    }

    public String getEntityRef() {
        return entityRef;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
