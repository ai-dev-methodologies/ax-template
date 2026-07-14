package com.ax.template.authblueprint.rangeownership;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * range-ownership-l0 sole orchestrator. Block registration serializes on the singleton
 * {@link RangeRegistryLock} row (RNG-NONOVERLAP-002 keystone) before the overlap check + insert.
 * Assignment and porting both fail-closed on containment (RNG-CONTAINMENT-001 /
 * RNG-PORT-003) — a port re-validates containment against the NEW owner and appends an
 * immutable {@link OwnershipEvent}; the current owner is always derived-on-read.
 */
@Service
public class RangeOwnershipService {

    private final RangeRegistryLockRepository lock;
    private final RangeRegistryLockInitializer lockInitializer;
    private final RangeBlockRepository blocks;
    private final IdentifierAssignmentRepository assignments;
    private final MemberWriter members;
    private final RangeOwnershipMetrics metrics;
    private final Clock clock;

    public RangeOwnershipService(RangeRegistryLockRepository lock, RangeRegistryLockInitializer lockInitializer,
                                 RangeBlockRepository blocks, IdentifierAssignmentRepository assignments,
                                 MemberWriter members, RangeOwnershipMetrics metrics, Clock clock) {
        this.lock = lock;
        this.lockInitializer = lockInitializer;
        this.blocks = blocks;
        this.assignments = assignments;
        this.members = members;
        this.metrics = metrics;
        this.clock = clock;
    }

    /**
     * RNG-NONOVERLAP-002 — the registry-row lock is acquired FIRST, so the overlap check and the
     * insert are atomic with respect to every other concurrent registration.
     */
    @Transactional
    public RangeBlock registerBlock(String ownerRef, long rangeStart, long rangeEnd) {
        if (ownerRef == null || ownerRef.isBlank()) {
            metrics.record("registerBlock", "invalid");
            throw RangeOwnershipException.invalidBlock("ownerRef must not be blank");
        }
        if (rangeStart >= rangeEnd) {
            metrics.record("registerBlock", "invalid");
            throw RangeOwnershipException.invalidBlock("rangeStart must be < rangeEnd");
        }
        lockInitializer.ensureExists();
        lock.lockForUpdate(RangeRegistryLock.GLOBAL_ID).orElseThrow(RangeOwnershipException::notFound);

        for (RangeBlock existing : blocks.findAll()) {
            if (existing.overlaps(rangeStart, rangeEnd)) {
                metrics.record("registerBlock", "overlap");
                throw RangeOwnershipException.blockOverlap();
            }
        }
        RangeBlock saved = blocks.save(new RangeBlock(UUID.randomUUID(), ownerRef, rangeStart, rangeEnd, Instant.now(clock)));
        metrics.record("registerBlock", "ok");
        return saved;
    }

    /** RNG-CONTAINMENT-001 — fail-closed: the identifier must fall inside a block the assigning owner owns. */
    @Transactional
    public IdentifierAssignment assign(long identifierValue, String ownerRef, String actor) {
        assignments.findByIdentifierValue(identifierValue).ifPresent(a -> {
            metrics.record("assign", "already_assigned");
            throw RangeOwnershipException.alreadyAssigned();
        });
        assertContained(ownerRef, identifierValue);

        IdentifierAssignment root = assignments.save(new IdentifierAssignment(UUID.randomUUID(), identifierValue, Instant.now(clock)));
        members.persist(new OwnershipEvent(UUID.randomUUID(), root.getId(), null, ownerRef, "INITIAL_ASSIGNMENT", Instant.now(clock)));
        metrics.record("assign", "ok");
        return root;
    }

    /**
     * RNG-PORT-003 — re-validates the destination's STANDING (does {@code toOwner} own at least
     * one range block anywhere in the plan?), deliberately NOT that toOwner's block covers this
     * exact identifier — under RNG-NONOVERLAP-002's global non-overlap invariant no OTHER owner
     * ever could cover the same point the original block permanently holds. Mirrors real E.164
     * number portability: the receiving carrier must be a licensed plan participant, not the
     * holder of this specific number's original range. Appends an immutable event whose
     * fromOwner is derived server-side from the current (latest) owner, never client input.
     */
    @Transactional
    public OwnershipEvent port(long identifierValue, String toOwner, String reason, String actor) {
        IdentifierAssignment root = assignments.findByIdentifierValue(identifierValue)
            .orElseThrow(RangeOwnershipException::notFound);
        String fromOwner = currentOwner(root.getId());
        if (!blocks.existsByOwnerRef(toOwner)) {
            metrics.record("port", "not_owned");
            throw RangeOwnershipException.notOwned();
        }

        OwnershipEvent event = members.persist(new OwnershipEvent(UUID.randomUUID(), root.getId(), fromOwner, toOwner, reason, Instant.now(clock)));
        metrics.record("port", "ok");
        return event;
    }

    @Transactional(readOnly = true)
    public String currentOwner(long identifierValue) {
        IdentifierAssignment root = assignments.findByIdentifierValue(identifierValue)
            .orElseThrow(RangeOwnershipException::notFound);
        return currentOwner(root.getId());
    }

    @Transactional(readOnly = true)
    public List<OwnershipEvent> history(long identifierValue) {
        IdentifierAssignment root = assignments.findByIdentifierValue(identifierValue)
            .orElseThrow(RangeOwnershipException::notFound);
        return assignments.findEvents(root.getId());
    }

    private String currentOwner(UUID assignmentId) {
        List<OwnershipEvent> latestFirst = assignments.findLatestEventFirst(assignmentId);
        return latestFirst.isEmpty() ? null : latestFirst.get(0).getToOwner();
    }

    private void assertContained(String ownerRef, long identifierValue) {
        List<RangeBlock> owned = blocks.findOwnedContaining(ownerRef, identifierValue);
        if (owned.isEmpty()) {
            metrics.record("assign", "not_owned");
            throw RangeOwnershipException.notOwned();
        }
    }
}
