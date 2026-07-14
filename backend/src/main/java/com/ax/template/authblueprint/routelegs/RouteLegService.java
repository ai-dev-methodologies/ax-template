package com.ax.template.authblueprint.routelegs;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * route-leg-contiguity-l0 sole orchestrator. LEG-SEQUENCE-001: append/insert verify the new leg's
 * origin/dest against its adjoining neighbor(s) before it is ever written. LEG-GAP-001: the ordinal
 * sequence is ALWAYS renumbered to a contiguous 1..N via {@link RouteRepository}'s two-phase
 * park-then-land bulk shift (see its javadoc) — never a naive single-statement shift that could
 * transiently collide with {@code uq(route_id, ordinal)}. LEG-MUTATE-001: insert/remove/replace
 * re-validate BOTH affected neighbors in the SAME transaction, and every structural mutation ends
 * by dirtying {@link Route} ({@link Route#touchMutation()} + {@code saveAndFlush}) so a concurrent
 * mutation on the SAME route is caught by the root's {@code @Version} optimistic lock OR the
 * {@code uq(route_id, ordinal)} backstop — either failure mode is mapped to the same 409
 * (CWE-362).
 */
@Service
public class RouteLegService {

    /** Two-phase shift temp offset — far larger than any realistic route's leg count. */
    static final int TEMP_OFFSET = 1_000_000;

    private final RouteRepository routes;
    private final MemberWriter members;
    private final RouteLegMetrics metrics;
    private final Clock clock;

    public RouteLegService(RouteRepository routes, MemberWriter members,
                           RouteLegMetrics metrics, Clock clock) {
        this.routes = routes;
        this.members = members;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public Route createRoute() {
        Route saved = routes.save(new Route(UUID.randomUUID(), Instant.now(clock)));
        metrics.record("create", "ok");
        return saved;
    }

    /** LEG-SEQUENCE-001 — append to the end; origin MUST equal the current last leg's destination. */
    @Transactional
    public Route appendLeg(UUID routeId, String origin, String dest) {
        Route route = routes.findById(routeId).orElseThrow(RouteLegException::notFound);
        List<RouteLeg> legs = routes.findLegsByRouteId(routeId);
        if (!legs.isEmpty() && !legs.get(legs.size() - 1).getDestCode().equals(origin)) {
            metrics.record("append", "sequence_violation");
            throw RouteLegException.sequenceViolation();
        }
        int nextOrdinal = legs.size() + 1;
        return withConcurrencyGuard("append", () -> {
            members.persistAndFlush(new RouteLeg(UUID.randomUUID(), routeId, nextOrdinal, origin, dest, Instant.now(clock)));
            commitMutation(route);
            return route;
        });
    }

    /** LEG-MUTATE-001 — insert at an arbitrary position; re-validates BOTH neighbors. */
    @Transactional
    public Route insertLegAt(UUID routeId, int atOrdinal, String origin, String dest) {
        Route route = routes.findById(routeId).orElseThrow(RouteLegException::notFound);
        List<RouteLeg> legs = routes.findLegsByRouteId(routeId);
        int size = legs.size();
        if (atOrdinal < 1 || atOrdinal > size + 1) {
            metrics.record("insert", "gap_violation");
            throw RouteLegException.gapViolation();
        }
        RouteLeg before = atOrdinal > 1 ? legs.get(atOrdinal - 2) : null;
        RouteLeg after = atOrdinal <= size ? legs.get(atOrdinal - 1) : null;
        if (before != null && !before.getDestCode().equals(origin)) {
            metrics.record("insert", "sequence_violation");
            throw RouteLegException.sequenceViolation();
        }
        if (after != null && !dest.equals(after.getOriginCode())) {
            metrics.record("insert", "sequence_violation");
            throw RouteLegException.sequenceViolation();
        }
        return withConcurrencyGuard("insert", () -> {
            if (atOrdinal <= size) {
                routes.parkOrdinalsFrom(routeId, atOrdinal, TEMP_OFFSET);
                routes.landOrdinalsFrom(routeId, TEMP_OFFSET, TEMP_OFFSET - 1);   // parked value → original+1
            }
            members.persistAndFlush(new RouteLeg(UUID.randomUUID(), routeId, atOrdinal, origin, dest, Instant.now(clock)));
            commitMutation(route);
            return route;
        });
    }

    /** LEG-MUTATE-001 — remove a leg; re-validates that the remaining BOTH neighbors still match. */
    @Transactional
    public Route removeLeg(UUID routeId, int ordinal) {
        Route route = routes.findById(routeId).orElseThrow(RouteLegException::notFound);
        List<RouteLeg> legs = routes.findLegsByRouteId(routeId);
        int idx = ordinal - 1;
        if (idx < 0 || idx >= legs.size()) {
            throw RouteLegException.notFound();
        }
        RouteLeg target = legs.get(idx);
        RouteLeg before = idx > 0 ? legs.get(idx - 1) : null;
        RouteLeg after = idx < legs.size() - 1 ? legs.get(idx + 1) : null;
        if (before != null && after != null && !before.getDestCode().equals(after.getOriginCode())) {
            metrics.record("remove", "sequence_violation");
            throw RouteLegException.sequenceViolation();
        }
        int size = legs.size();
        return withConcurrencyGuard("remove", () -> {
            routes.deleteLeg(target.getId());
            if (ordinal < size) {
                routes.parkOrdinalsFrom(routeId, ordinal + 1, TEMP_OFFSET);
                routes.landOrdinalsFrom(routeId, TEMP_OFFSET, TEMP_OFFSET + 1);   // parked value → original-1
            }
            commitMutation(route);
            return route;
        });
    }

    /** LEG-MUTATE-001 — replace a leg's origin/dest in place; re-validates BOTH neighbors. */
    @Transactional
    public Route replaceLeg(UUID routeId, int ordinal, String origin, String dest) {
        Route route = routes.findById(routeId).orElseThrow(RouteLegException::notFound);
        List<RouteLeg> legs = routes.findLegsByRouteId(routeId);
        int idx = ordinal - 1;
        if (idx < 0 || idx >= legs.size()) {
            throw RouteLegException.notFound();
        }
        RouteLeg target = legs.get(idx);
        RouteLeg before = idx > 0 ? legs.get(idx - 1) : null;
        RouteLeg after = idx < legs.size() - 1 ? legs.get(idx + 1) : null;
        if (before != null && !before.getDestCode().equals(origin)) {
            metrics.record("replace", "sequence_violation");
            throw RouteLegException.sequenceViolation();
        }
        if (after != null && !dest.equals(after.getOriginCode())) {
            metrics.record("replace", "sequence_violation");
            throw RouteLegException.sequenceViolation();
        }
        return withConcurrencyGuard("replace", () -> {
            routes.replaceLegFields(target.getId(), origin, dest);
            commitMutation(route);
            return route;
        });
    }

    /** LEG-GAP-001 — reorder into a new permutation; the WHOLE new sequence is validated for
     *  contiguity BEFORE any ordinal changes (all-or-nothing). */
    @Transactional
    public Route reorderLegs(UUID routeId, List<UUID> newOrderLegIds) {
        Route route = routes.findById(routeId).orElseThrow(RouteLegException::notFound);
        List<RouteLeg> legs = routes.findLegsByRouteId(routeId);
        Set<UUID> currentIds = legs.stream().map(RouteLeg::getId).collect(Collectors.toSet());
        Set<UUID> proposedIds = new HashSet<>(newOrderLegIds);
        if (newOrderLegIds.size() != legs.size() || proposedIds.size() != newOrderLegIds.size()
            || !proposedIds.equals(currentIds)) {
            metrics.record("reorder", "gap_violation");
            throw RouteLegException.gapViolation();
        }
        Map<UUID, RouteLeg> byId = legs.stream().collect(Collectors.toMap(RouteLeg::getId, l -> l));
        for (int i = 0; i < newOrderLegIds.size() - 1; i++) {
            RouteLeg a = byId.get(newOrderLegIds.get(i));
            RouteLeg b = byId.get(newOrderLegIds.get(i + 1));
            if (!a.getDestCode().equals(b.getOriginCode())) {
                metrics.record("reorder", "gap_violation");
                throw RouteLegException.gapViolation();
            }
        }
        return withConcurrencyGuard("reorder", () -> {
            routes.parkOrdinalsFrom(routeId, 1, TEMP_OFFSET);        // park every leg of the route
            for (int i = 0; i < newOrderLegIds.size(); i++) {
                routes.setOrdinal(newOrderLegIds.get(i), i + 1);
            }
            commitMutation(route);
            return route;
        });
    }

    @Transactional(readOnly = true)
    public Route get(UUID routeId) {
        return routes.findById(routeId).orElseThrow(RouteLegException::notFound);
    }

    @Transactional(readOnly = true)
    public List<RouteLeg> legs(UUID routeId) {
        get(routeId);                                                 // 404 before an empty list
        return routes.findLegsByRouteId(routeId);
    }

    /** LEG-MUTATE-001 — dirty the root so its @Version check fires at flush. */
    private void commitMutation(Route route) {
        route.touchMutation();
        routes.saveAndFlush(route);
    }

    /**
     * Any concurrency-class failure on this path maps to the SAME deterministic 409 — belt-and-
     * suspenders, mirroring the catalog's other keystones. {@code ConcurrencyFailureException} is
     * Spring's common superclass for BOTH failure shapes an H2 race can actually produce here:
     * {@code ObjectOptimisticLockingFailureException} (a stale @Version on the root's saveAndFlush)
     * AND {@code CannotAcquireLockException} / {@code PessimisticLockingFailureException} (a lock-
     * wait timeout when two threads' inserts contend for the SAME uq(route_id, ordinal) row before
     * either commits — observed to surface as an unmapped 500 before this catch was widened past
     * only the optimistic-lock subtype). {@code DataIntegrityViolationException} is the third shape
     * (the uq constraint violation itself, once the contending insert is actually evaluated).
     */
    private Route withConcurrencyGuard(String op, Supplier<Route> mutation) {
        try {
            Route result = mutation.get();
            metrics.record(op, "ok");
            return result;
        } catch (ConcurrencyFailureException | DataIntegrityViolationException conflict) {
            metrics.record(op, "concurrent_modification");
            throw RouteLegException.concurrentModification();
        }
    }
}
