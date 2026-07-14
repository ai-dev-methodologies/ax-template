package com.ax.template.authblueprint.obligation;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * deadline-obligation-l0 sole orchestrator. Every write path (create, usage advance,
 * acknowledge) and the sweep serialize on the obligation's PESSIMISTIC_WRITE row
 * (OBL-CONCURRENT-001). The effective deadline is ALWAYS min(axis candidates), re-evaluated
 * under the lock with the new derivation appended (OBL-GROUND/AXIS-001). The ONLY terminal
 * writer is {@link #acknowledge} (OBL-ACK-001). Members are written via {@link MemberWriter}
 * and read via JPQL on {@link ObligationRepository}.
 */
@Service
public class ObligationService {

    static final int MAX_PAGE_SIZE = 200;

    private final ObligationRepository obligations;
    private final MemberWriter members;
    private final ObligationMetrics metrics;
    private final Clock clock;

    public ObligationService(ObligationRepository obligations, MemberWriter members,
                             ObligationMetrics metrics, Clock clock) {
        this.obligations = obligations;
        this.members = members;
        this.metrics = metrics;
        this.clock = clock;
    }

    public record AxisSpec(AxisKind kind, Instant anchorAt, Integer intervalDays,
                           BigDecimal limitUnits, BigDecimal unitsPerDay) {}

    /** Create with derivable axes — the deadline is computed, never accepted raw (OBL-GROUND-001).
     *  {@code breachBasisAmount} is OPTIONAL (OBL-CONSEQUENCE-001) — {@code null} means the obligation
     *  never binds a monetary consequence, no matter how long it stays breached. */
    @Transactional
    public Obligation create(String obligationKey, List<AxisSpec> axisSpecs, BigDecimal breachBasisAmount) {
        if (axisSpecs == null || axisSpecs.isEmpty()) {
            metrics.record("create", "invalid");
            throw ObligationException.invalidAxis();
        }
        if (breachBasisAmount != null && breachBasisAmount.signum() <= 0) {
            metrics.record("create", "invalid");
            throw ObligationException.invalidConsequenceBasis();
        }
        if (obligations.existsByObligationKey(obligationKey)) {
            metrics.record("create", "rejected");
            throw ObligationException.duplicateKey();
        }
        Instant now = Instant.now(clock);
        try {
            Instant windowStart = axisSpecs.stream()
                .map(spec -> spec.anchorAt() == null ? now : spec.anchorAt())
                .min(Comparator.naturalOrder()).orElse(now);
            Obligation o = new Obligation(UUID.randomUUID(), obligationKey, now, windowStart, now,
                breachBasisAmount == null ? null : breachBasisAmount.setScale(4));
            obligations.saveAndFlush(o);
            Instant earliest = null;
            for (AxisSpec spec : axisSpecs) {
                ObligationAxis axis = buildAxis(o.getId(), spec, now);
                members.persist(axis);
                members.persist(new DerivationRecord(UUID.randomUUID(), o.getId(), axis.getId(),
                    axis.getCandidateDeadline(), axis.derivationFormula(axis.getAnchorAt()), now));
                if (earliest == null || axis.getCandidateDeadline().isBefore(earliest)) {
                    earliest = axis.getCandidateDeadline();
                }
            }
            o.reevaluate(earliest);
            metrics.record("create", "ok");
            return o;
        } catch (DataIntegrityViolationException e) {
            metrics.record("create", "rejected");
            throw ObligationException.duplicateKey();
        }
    }

    private ObligationAxis buildAxis(UUID obligationId, AxisSpec spec, Instant now) {
        Instant anchor = spec.anchorAt() == null ? now : spec.anchorAt();
        if (spec.kind() == AxisKind.CALENDAR) {
            if (spec.intervalDays() == null || spec.intervalDays() <= 0) {
                metrics.record("create", "invalid");
                throw ObligationException.invalidAxis();
            }
            return ObligationAxis.calendar(UUID.randomUUID(), obligationId, anchor, spec.intervalDays());
        }
        if (spec.limitUnits() == null || spec.limitUnits().signum() <= 0
                || spec.unitsPerDay() == null || spec.unitsPerDay().signum() <= 0) {
            metrics.record("create", "invalid");
            throw ObligationException.invalidAxis();
        }
        return ObligationAxis.usage(UUID.randomUUID(), obligationId, anchor,
            spec.limitUnits().setScale(4), spec.unitsPerDay().setScale(4));
    }

    /** OBL-AXIS-001 — advance the USAGE axis; re-derive, re-record, re-evaluate under the lock. */
    @Transactional
    public Obligation advanceUsage(String obligationKey, BigDecimal units) {
        if (units == null || units.signum() <= 0) {
            metrics.record("usage", "invalid");
            throw ObligationException.invalidAxis();
        }
        Obligation o = obligations.findByObligationKeyForUpdate(obligationKey)
            .orElseThrow(ObligationException::notFound);
        ObligationAxis axis = obligations.findAxis(o.getId(), AxisKind.USAGE)
            .orElseThrow(ObligationException::notFound);
        Instant now = Instant.now(clock);
        Instant candidate = axis.advanceUsage(units.setScale(4), now);
        members.persist(new DerivationRecord(UUID.randomUUID(), o.getId(), axis.getId(),
            candidate, axis.derivationFormula(now), now));
        o.reevaluate(earliestCandidate(o.getId()));
        o.incrementUsageCycle();                          // OBL-WAIVER-001 — the cycle axis advances too
        metrics.record("usage", "ok");
        return o;
    }

    /** OBL-ACK-001 — the ONLY terminal edge; who/when recorded; the loop closes once. */
    @Transactional
    public Obligation acknowledge(String obligationKey, String acknowledger) {
        if (acknowledger == null || acknowledger.isBlank()) {
            metrics.record("ack", "invalid");
            throw ObligationException.acknowledgerRequired();
        }
        Obligation o = obligations.findByObligationKeyForUpdate(obligationKey)
            .orElseThrow(ObligationException::notFound);
        if (o.getStatus() == ObligationStatus.ACKNOWLEDGED) {
            metrics.record("ack", "rejected");
            throw ObligationException.alreadyAcknowledged();
        }
        o.acknowledge(acknowledger.strip(), Instant.now(clock));
        metrics.record("ack", "ok");
        return o;
    }

    @Transactional(readOnly = true)
    public Obligation get(String obligationKey) {
        return obligations.findByObligationKey(obligationKey).orElseThrow(ObligationException::notFound);
    }

    @Transactional(readOnly = true)
    public List<ObligationAxis> axes(String obligationKey) {
        return obligations.findAxes(get(obligationKey).getId());
    }

    @Transactional(readOnly = true)
    public List<EscalationEvent> escalations(String obligationKey) {
        // rung is @Enumerated(STRING) — SQL would sort it alphabetically (BREACH < IMMINENT),
        // so order by firing time then LADDER position here
        return obligations.findEscalations(get(obligationKey).getId()).stream()
            .sorted(Comparator.comparing(EscalationEvent::getFiredAt)
                .thenComparing(e -> EscalationRung.LADDER.indexOf(e.getRung())))
            .toList();
    }

    @Transactional(readOnly = true)
    public Page<DerivationRecord> derivations(String obligationKey, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return obligations.findDerivationsPage(get(obligationKey).getId(), PageRequest.of(safePage, safeSize));
    }

    /** OBL-INTEREST-ACCRUE-001 — the consequence, plus its accrued interest DERIVED fresh at read time. */
    public record ConsequenceView(BreachConsequence consequence, BigDecimal accruedInterest) {}

    @Transactional(readOnly = true)
    public ConsequenceView consequence(String obligationKey) {
        BreachConsequence c = obligations.findConsequence(get(obligationKey).getId())
            .orElseThrow(ObligationException::notFound);
        return new ConsequenceView(c, c.accruedInterest(Instant.now(clock)));
    }

    /** OBL-WAIVER-001 — a waiver plus its currently-derived validity (never stored as a flag). */
    public record WaiverView(ObligationWaiver waiver, boolean active) {}

    /** OBL-WAIVER-002 — 4-eyes (grantor != declared owner) + non-blank reason; immutable once granted. */
    @Transactional
    public ObligationWaiver grantWaiver(String obligationKey, String grantedBy, String obligationOwner,
                                        String reason, Instant expiresAt, long expiresAfterCycles) {
        Obligation o = obligations.findByObligationKeyForUpdate(obligationKey)
            .orElseThrow(ObligationException::notFound);
        if (obligationOwner == null || obligationOwner.isBlank() || reason == null || reason.isBlank()
                || expiresAt == null || !expiresAt.isAfter(Instant.now(clock)) || expiresAfterCycles <= 0) {
            metrics.record("waiver-grant", "invalid");
            throw ObligationException.invalidWaiver();
        }
        if (grantedBy.strip().equals(obligationOwner.strip())) {
            metrics.record("waiver-grant", "rejected");
            throw ObligationException.waiverSelfGrant();
        }
        ObligationWaiver w = new ObligationWaiver(UUID.randomUUID(), o.getId(), grantedBy.strip(),
            obligationOwner.strip(), reason.strip(), Instant.now(clock), o.getUsageCycleCount(),
            expiresAt, expiresAfterCycles);
        members.persist(w);
        metrics.record("waiver-grant", "ok");
        return w;
    }

    /** OBL-WAIVER-002 — revoke APPENDS a {@link WaiverRevocation}; the grant row is never re-mutated. */
    @Transactional
    public void revokeWaiver(String obligationKey, UUID waiverId, String revokedBy) {
        Obligation o = obligations.findByObligationKeyForUpdate(obligationKey)
            .orElseThrow(ObligationException::notFound);
        ObligationWaiver w = obligations.findWaiver(o.getId(), waiverId).orElseThrow(ObligationException::notFound);
        if (obligations.isRevoked(w.getId())) {
            metrics.record("waiver-revoke", "rejected");
            throw ObligationException.waiverAlreadyRevoked();
        }
        members.persist(new WaiverRevocation(UUID.randomUUID(), w.getId(), o.getId(), revokedBy, Instant.now(clock)));
        metrics.record("waiver-revoke", "ok");
    }

    @Transactional(readOnly = true)
    public List<WaiverView> waivers(String obligationKey) {
        Obligation o = get(obligationKey);
        Instant now = Instant.now(clock);
        return obligations.findWaivers(o.getId()).stream()
            .map(w -> new WaiverView(w, !obligations.isRevoked(w.getId())
                && w.isValidAt(now, o.getUsageCycleCount())))
            .toList();
    }

    private Instant earliestCandidate(UUID obligationId) {
        return obligations.findAxes(obligationId).stream()
            .map(ObligationAxis::getCandidateDeadline)
            .min(Comparator.naturalOrder())
            .orElseThrow(ObligationException::invalidAxis);
    }
}
