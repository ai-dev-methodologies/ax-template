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
 * dispatch demand unit. Non-contended edges move only through {@link ServiceRequestStateMachine}
 * (no public setter); the contended OFFERED→ASSIGNED claim is the atomic conditional UPDATE in
 * {@link ServiceRequestRepository#claim} (exclusive-assignment-l0 EXCL-CLAIM-001 / EXCL-409-004).
 * {@code id}/{@code description}/{@code createdBy}/{@code createdAt} immutable.
 */
@AggregateRoot
@Entity
@Table(name = "dispatch_requests")
public class ServiceRequest {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "description", nullable = false, updatable = false, length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ServiceRequestStatus status;

    /** Set ONLY by the atomic OFFERED→ASSIGNED claim; null until assigned. */
    @Column(name = "assigned_provider_id")
    private UUID assignedProviderId;

    @Column(name = "created_by", nullable = false, updatable = false, length = 255)
    private String createdBy;

    /** Optimistic-lock version (atomic state transitions; sweep-loses-race). */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ServiceRequest() {}

    public ServiceRequest(UUID id, String description, String createdBy, Instant createdAt) {
        this.id = id;
        this.description = description;
        this.status = ServiceRequestStatus.PENDING;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    /** Sole-mutator hook — package-private so only {@link ServiceRequestStateMachine} advances status. */
    void setStatus(ServiceRequestStatus next) {
        this.status = next;
    }

    public UUID getId() { return id; }
    public String getDescription() { return description; }
    public ServiceRequestStatus getStatus() { return status; }
    public UUID getAssignedProviderId() { return assignedProviderId; }
    public String getCreatedBy() { return createdBy; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
