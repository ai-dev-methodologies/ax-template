package com.ax.template.authblueprint.intervalexclusivity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * interval-exclusivity-l0 root: the lockable ANCHOR for one bookable resource. Every booking
 * create/resize/cancel for this resource acquires THIS row under {@code PESSIMISTIC_WRITE} before
 * touching {@link Booking} rows (IVX-CONCURRENT-002) — the same single-anchor-row-serializes-all-
 * writes discipline {@code ThresholdRegister} uses for its per-scope register, applied here to a
 * per-resource booking ledger. HONEST DEGRADATION: this row lock — NOT a PostgreSQL
 * {@code EXCLUDE USING gist} range constraint — is the mechanism backstopping exclusivity on H2 (see
 * the spec's composition note). No public setter; the identity is immutable.
 */
@AggregateRoot
@Entity
@Table(name = "booking_resources")
public class BookingResource {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "resource_key", nullable = false, updatable = false, length = 200, unique = true)
    private String resourceKey;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BookingResource() {}

    public BookingResource(UUID id, String resourceKey, Instant createdAt) {
        this.id = id;
        this.resourceKey = resourceKey;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getResourceKey() { return resourceKey; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
