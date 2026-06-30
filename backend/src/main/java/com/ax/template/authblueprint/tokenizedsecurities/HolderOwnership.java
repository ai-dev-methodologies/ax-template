package com.ax.template.authblueprint.tokenizedsecurities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * Holder ownership record — one principal controls exactly one holder (unique on holder_id).
 * First-claim-wins; a holder with no claim is uncontrolled (fail-closed default).
 * Supports custodial/omnibus: one principal may claim many holders.
 */
@AggregateRoot
@Entity
@Table(name = "holder_ownership",
        uniqueConstraints = @UniqueConstraint(name = "uq_holder_ownership_holder",
                columnNames = {"holder_id"}))
public class HolderOwnership {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "holder_id", nullable = false, updatable = false, length = 200)
    private String holderId;

    @Column(name = "owner_principal", nullable = false, updatable = false, length = 200)
    private String ownerPrincipal;

    @Column(name = "claimed_at", nullable = false, updatable = false)
    private Instant claimedAt;

    protected HolderOwnership() {}

    HolderOwnership(String holderId, String ownerPrincipal, Instant claimedAt) {
        this.id = UUID.randomUUID();
        this.holderId = holderId;
        this.ownerPrincipal = ownerPrincipal;
        this.claimedAt = claimedAt;
    }

    public UUID getId() { return id; }
    public String getHolderId() { return holderId; }
    public String getOwnerPrincipal() { return ownerPrincipal; }
    public Instant getClaimedAt() { return claimedAt; }
}
