package com.ax.template.authblueprint.rangeownership;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * RNG-PORT-003 root — the identity anchor for one identifier value's ownership history. Carries
 * NO current-owner field: the current owner is ALWAYS derived-on-read as the {@code toOwner} of
 * the latest {@link OwnershipEvent} member (never a separately-settable pointer that could drift
 * from the append-only history). {@code identifierValue} is immutable and unique — one
 * assignment root per identifier.
 */
@AggregateRoot
@Entity
@Table(name = "identifier_assignments")
public class IdentifierAssignment {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "identifier_value", nullable = false, updatable = false, unique = true)
    private long identifierValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected IdentifierAssignment() {}

    public IdentifierAssignment(UUID id, long identifierValue, Instant createdAt) {
        this.id = id;
        this.identifierValue = identifierValue;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public long getIdentifierValue() { return identifierValue; }
    public Instant getCreatedAt() { return createdAt; }
}
