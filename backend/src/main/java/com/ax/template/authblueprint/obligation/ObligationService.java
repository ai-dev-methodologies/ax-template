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

    /** Create with derivable axes — the deadline is computed, never accepted raw (OBL-GROUND-001). */
    @Transactional
    public Obligation create(String obligationKey, List<AxisSpec> axisSpecs) {
        if (axisSpecs == null || axisSpecs.isEmpty()) {
            metrics.record("create", "invalid");
            throw ObligationException.invalidAxis();
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
            Obligation o = new Obligation(UUID.randomUUID(), obligationKey, now, windowStart, now);
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

    private Instant earliestCandidate(UUID obligationId) {
        return obligations.findAxes(obligationId).stream()
            .map(ObligationAxis::getCandidateDeadline)
            .min(Comparator.naturalOrder())
            .orElseThrow(ObligationException::invalidAxis);
    }
}
