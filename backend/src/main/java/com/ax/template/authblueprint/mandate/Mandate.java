package com.ax.template.authblueprint.mandate;

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
 * mandate-fanout-l0 root: one directive that FANS OUT to N child tasks (MANDATE-FANOUT-001),
 * gated by a check battery (MANDATE-BATTERY-001). The {@code issuedCount} is recorded once at
 * fan-out and is immutable — the conserved target the DERIVED completion recall counts terminal
 * children against (Σ terminal == issuedCount); completion is NEVER a stored flag on this row.
 * Lifecycle moves only via the package-private hooks, called by {@link MandateService}. The
 * @Check backstops that issuedCount is positive and a SATISFIED mandate carries who/when.
 */
@AggregateRoot
@Entity
@Table(name = "mandates")
@Check(constraints =
    "issued_count > 0"
    + " AND (status <> 'SATISFIED' OR (satisfied_by IS NOT NULL AND satisfied_at IS NOT NULL))")
public class Mandate {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The directive being fanned out — opaque, recorded verbatim. */
    @Column(name = "directive", nullable = false, updatable = false, length = 500)
    private String directive;

    /** The recorded fan-out target N — immutable; the conserved completion recall counts against it. */
    @Column(name = "issued_count", nullable = false, updatable = false)
    private int issuedCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MandateStatus status;

    @Column(name = "satisfied_by", length = 200)
    private String satisfiedBy;

    @Column(name = "satisfied_at")
    private Instant satisfiedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Mandate() {}

    public Mandate(UUID id, String directive, int issuedCount, Instant createdAt) {
        this.id = id;
        this.directive = directive;
        this.issuedCount = issuedCount;
        this.status = MandateStatus.ISSUED;
        this.createdAt = createdAt;
    }

    /** Sole-mutator hook — mark the directive SATISFIED once the check battery is fully PASSED
     *  (MANDATE-BATTERY-001). Records who cleared it and when; SATISFIED is terminal. */
    void markSatisfied(String by, Instant at) {
        this.status = MandateStatus.SATISFIED;
        this.satisfiedBy = by;
        this.satisfiedAt = at;
    }

    public boolean isSatisfied() {
        return status == MandateStatus.SATISFIED;
    }

    public UUID getId() { return id; }
    public String getDirective() { return directive; }
    public int getIssuedCount() { return issuedCount; }
    public MandateStatus getStatus() { return status; }
    public String getSatisfiedBy() { return satisfiedBy; }
    public Instant getSatisfiedAt() { return satisfiedAt; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
