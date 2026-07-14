package com.ax.template.authblueprint.rangeownership;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * RNG-CONTAINMENT/NONOVERLAP-001/002 root — one HALF-OPEN [rangeStart, rangeEnd) block owned by
 * a single owner. Immutable once registered (RNG-NONOVERLAP-002's non-overlap invariant only
 * holds if a block cannot be silently resized after other blocks have been checked against it).
 * A separate {@link AggregateRoot} from {@link IdentifierAssignment} — referenced by owner ref
 * (an opaque string, not an object pointer) and by identity-value containment lookups only.
 */
@AggregateRoot
@Entity
@Table(name = "range_blocks")
@Check(constraints = "range_start < range_end")
public class RangeBlock {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "owner_ref", nullable = false, updatable = false, length = 200)
    private String ownerRef;

    /** Inclusive lower bound of the half-open [rangeStart, rangeEnd) block. */
    @Column(name = "range_start", nullable = false, updatable = false)
    private long rangeStart;

    /** Exclusive upper bound of the half-open [rangeStart, rangeEnd) block. */
    @Column(name = "range_end", nullable = false, updatable = false)
    private long rangeEnd;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RangeBlock() {}

    public RangeBlock(UUID id, String ownerRef, long rangeStart, long rangeEnd, Instant createdAt) {
        this.id = id;
        this.ownerRef = ownerRef;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
        this.createdAt = createdAt;
    }

    /** RNG-NONOVERLAP-002 — half-open overlap test: s1 < e2 AND s2 < e1. Adjacency (touching) is NOT overlap. */
    public boolean overlaps(long otherStart, long otherEnd) {
        return this.rangeStart < otherEnd && otherStart < this.rangeEnd;
    }

    /** RNG-CONTAINMENT-001 — is {@code identifierValue} inside this half-open block? */
    public boolean contains(long identifierValue) {
        return identifierValue >= rangeStart && identifierValue < rangeEnd;
    }

    public UUID getId() { return id; }
    public String getOwnerRef() { return ownerRef; }
    public long getRangeStart() { return rangeStart; }
    public long getRangeEnd() { return rangeEnd; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
