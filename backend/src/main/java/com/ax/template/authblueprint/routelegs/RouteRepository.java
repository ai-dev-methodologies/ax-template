package com.ax.template.authblueprint.routelegs;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * {@link RouteLeg} rows are members (HG-AGG-REPO): they own no repository of their own — reads are
 * root-JPQL here, writes are bulk JPQL here (never a Java setter — {@link RouteLeg} has none).
 *
 * <p>Ordinal renumbering ALWAYS goes through a two-phase PARK-then-LAND shift ({@link
 * #parkOrdinalsFrom} then {@link #landOrdinalsFrom}) so no intra-statement duplicate can ever hit
 * the {@code uq(route_id, ordinal)} backstop: phase 1 moves the affected legs into a disjoint,
 * far-out temporary ordinal range in ONE bulk statement (safe — the shift preserves each leg's
 * relative order and the temp range never overlaps a real 1..N ordinal); phase 2 lands them at
 * their final values in a second bulk statement (also safe, for the same reason). LEG-MUTATE-001's
 * root @Version optimistic lock (on {@link Route}, via {@link Route#touchMutation()} +
 * {@code save}) is the SEPARATE serialization point for concurrent mutation across requests —
 * these two phases are the SAME-request collision-avoidance technique, distinct from that lock.
 */
public interface RouteRepository extends JpaRepository<Route, UUID> {

    @Query("SELECT l FROM RouteLeg l WHERE l.routeId = :routeId ORDER BY l.ordinal ASC")
    List<RouteLeg> findLegsByRouteId(@Param("routeId") UUID routeId);

    /** Two-phase shift, step 1 — park every leg at/after fromOrdinal into a disjoint temp zone.
     *  {@code clearAutomatically} is deliberately NOT set: {@link Route} (loaded before any of
     *  these bulk writes run) must stay MANAGED and ATTACHED so its later {@code saveAndFlush}
     *  performs a direct version-checked UPDATE, not a merge. Safe because no code in this
     *  service re-reads a {@link RouteLeg} via JPQL after a bulk write within the same
     *  transaction (each method reads legs ONCE, up front, before mutating). */
    @Modifying
    @Query("UPDATE RouteLeg l SET l.ordinal = l.ordinal + :tempOffset"
        + " WHERE l.routeId = :routeId AND l.ordinal >= :fromOrdinal")
    int parkOrdinalsFrom(@Param("routeId") UUID routeId, @Param("fromOrdinal") int fromOrdinal,
                        @Param("tempOffset") int tempOffset);

    /** Two-phase shift, step 2 — land the parked legs at (parked value − landOffset). */
    @Modifying
    @Query("UPDATE RouteLeg l SET l.ordinal = l.ordinal - :landOffset"
        + " WHERE l.routeId = :routeId AND l.ordinal >= :tempFloor")
    int landOrdinalsFrom(@Param("routeId") UUID routeId, @Param("tempFloor") int tempFloor,
                        @Param("landOffset") int landOffset);

    /** Reorder step — land ONE parked leg at its final permutation position (id-targeted, safe
     *  once every leg of the route has been parked by {@link #parkOrdinalsFrom} first). */
    @Modifying
    @Query("UPDATE RouteLeg l SET l.ordinal = :ordinal WHERE l.id = :legId")
    int setOrdinal(@Param("legId") UUID legId, @Param("ordinal") int ordinal);

    @Modifying
    @Query("UPDATE RouteLeg l SET l.originCode = :origin, l.destCode = :dest WHERE l.id = :legId")
    int replaceLegFields(@Param("legId") UUID legId, @Param("origin") String origin, @Param("dest") String dest);

    @Modifying
    @Query("DELETE FROM RouteLeg l WHERE l.id = :legId")
    int deleteLeg(@Param("legId") UUID legId);
}
