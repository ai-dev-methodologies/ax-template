package com.ax.template.authblueprint.routelegs;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * route-leg-contiguity-l0 root: an ORDERED sequence of {@link RouteLeg} members (LEG-SEQUENCE-001).
 * {@code mutationSeq} exists SOLELY so every structural leg mutation (append/insert/remove/replace/
 * reorder) dirties this row and forces the {@code @Version} check at flush — the legs themselves
 * are mutated by bulk JPQL on {@link RouteRepository}, which does not by itself touch this row
 * (LEG-MUTATE-001 keystone — the root's optimistic lock is the serialization point for concurrent
 * mutation, CWE-362). No public setter; mutated only by {@link RouteLegService}.
 */
@AggregateRoot
@Entity
@Table(name = "routes")
public class Route {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "mutation_seq", nullable = false)
    private long mutationSeq;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Route() {}

    public Route(UUID id, Instant createdAt) {
        this.id = id;
        this.mutationSeq = 0L;
        this.createdAt = createdAt;
    }

    /** LEG-MUTATE-001 — called once per structural leg mutation so this row is always dirtied,
     *  forcing the @Version optimistic-lock check regardless of which leg columns actually changed. */
    void touchMutation() {
        this.mutationSeq++;
    }

    public UUID getId() { return id; }
    public long getMutationSeq() { return mutationSeq; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
