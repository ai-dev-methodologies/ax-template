package com.ax.template.authblueprint.routelegs;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * route-leg-contiguity-l0 member: one leg of a {@link Route}, at {@code ordinal} position, from
 * {@code originCode} to {@code destCode} (LEG-SEQUENCE-001). {@code ordinal}/{@code originCode}/
 * {@code destCode} are the only mutable columns — reorder renumbers ordinal, replace rewrites the
 * origin/dest — and BOTH are mutated exclusively via bulk JPQL on {@link RouteRepository}
 * (never a Java setter on this class; it has none). The {@code uq(route_id, ordinal)} backstop
 * makes a duplicate ordinal for one route a deterministic constraint violation
 * (LEG-GAP-001); {@code id}/{@code routeId}/{@code createdAt} are immutable. No repository of its
 * own (HG-AGG-REPO — a member, written/read only through {@link Route}'s own repository).
 */
@AggregateMember(root = Route.class)
@Entity
@Table(name = "route_legs", uniqueConstraints = {
    @UniqueConstraint(name = "uq_route_leg_ordinal", columnNames = {"route_id", "ordinal"})
})
@Check(constraints = "ordinal >= 1")
public class RouteLeg {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "route_id", nullable = false, updatable = false)
    private UUID routeId;

    /** LEG-GAP-001 — the leg's position in the contiguous 1..N sequence; mutated via bulk JPQL only. */
    @Column(name = "ordinal", nullable = false)
    private int ordinal;

    /** LEG-SEQUENCE-001 — mutated via bulk JPQL only (replace). */
    @Column(name = "origin_code", nullable = false, length = 200)
    private String originCode;

    /** LEG-SEQUENCE-001 — mutated via bulk JPQL only (replace). */
    @Column(name = "dest_code", nullable = false, length = 200)
    private String destCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RouteLeg() {}

    public RouteLeg(UUID id, UUID routeId, int ordinal, String originCode, String destCode, Instant createdAt) {
        this.id = id;
        this.routeId = routeId;
        this.ordinal = ordinal;
        this.originCode = originCode;
        this.destCode = destCode;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getRouteId() { return routeId; }
    public int getOrdinal() { return ordinal; }
    public String getOriginCode() { return originCode; }
    public String getDestCode() { return destCode; }
    public Instant getCreatedAt() { return createdAt; }
}
