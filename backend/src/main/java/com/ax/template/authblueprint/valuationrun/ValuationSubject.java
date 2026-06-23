package com.ax.template.authblueprint.valuationrun;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * valuation-run-projection-l0 root: the lockable HEAD of one subject's versioned valuation
 * chain (VALRUN-CONCURRENT-001). Every recompute/rebase takes THIS row's PESSIMISTIC_WRITE
 * lock first, so the read-current-version / write-next-version sequence cannot interleave;
 * {@link ValuationRunService} is the sole caller of the package-private {@link #advanceHead}.
 * The head pointer records the current version; the full run history lives in immutable
 * {@link ValuationRun} rows linked back by {@code subjectId} (reference-by-identity).
 */
@AggregateRoot
@Entity
@Table(name = "valuation_subjects")
public class ValuationSubject {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The thing being valued (portfolio/book/fund id) — opaque, recorded verbatim. */
    @Column(name = "subject_ref", nullable = false, updatable = false, length = 200)
    private String subjectRef;

    /** The current head version; 0 means no run computed yet. Advanced ONLY by the service. */
    @Column(name = "head_run_version", nullable = false)
    private int headRunVersion;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ValuationSubject() {}

    public ValuationSubject(UUID id, String subjectRef, Instant createdAt) {
        this.id = id;
        this.subjectRef = subjectRef;
        this.headRunVersion = 0;
        this.createdAt = createdAt;
    }

    /** Sole-mutator hook — move the head pointer forward to the newly created version. */
    void advanceHead(int runVersion) {
        this.headRunVersion = runVersion;
    }

    public UUID getId() { return id; }
    public String getSubjectRef() { return subjectRef; }
    public int getHeadRunVersion() { return headRunVersion; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
