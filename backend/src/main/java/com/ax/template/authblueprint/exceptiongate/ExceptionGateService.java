package com.ax.template.authblueprint.exceptiongate;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * orthogonal-exception-gate-l0 sole orchestrator — generalized from the reference workload's
 * {@code dsr.DsrRestrictionGate} (GDPR Art 18 restriction), the first proven instance of this
 * shape. Raising/lifting the exception dimension NEVER touches the subject's primary lifecycle
 * field, and advancing the primary lifecycle NEVER touches the exception dimension
 * (EXC-DIM-INDEPENDENT-001) — both directions are separate mutator hooks on {@link ExceptionGate}.
 * While raised, a GATED operation is refused fail-closed BEFORE any mutation (EXC-DIM-BLOCK-001);
 * a non-gated operation is unaffected. Every raise/lift is audited append-only (EXC-DIM-LIFT-001).
 */
@Service
public class ExceptionGateService {

    /**
     * The configurable operation set gated while raised (a fork-receiver names its own real
     * operations; this reference workload's demo set stands in for "the operations that must
     * stop"). {@code read} is deliberately NOT gated — EXC-DIM-BLOCK-001 requires non-gated
     * operations to be unaffected.
     */
    static final Set<String> GATED_OPERATIONS = Set.of("write", "export");

    private final ExceptionGateRepository gates;
    private final MemberWriter members;
    private final Clock clock;

    public ExceptionGateService(ExceptionGateRepository gates, MemberWriter members, Clock clock) {
        this.gates = gates;
        this.members = members;
        this.clock = clock;
    }

    @Transactional
    public ExceptionGate raise(String subjectType, String subjectId, String reason, String actor) {
        ExceptionGate gate = getOrCreate(subjectType, subjectId);
        gate.raise(reason);                                             // idempotent — re-raise stays raised
        members.persist(new ExceptionAuditEntry(UUID.randomUUID(), gate.getId(), "RAISE", reason, actor,
            Instant.now(clock)));
        return gate;
    }

    @Transactional
    public ExceptionGate lift(String subjectType, String subjectId, String reason, String actor) {
        ExceptionGate gate = gates.findBySubjectTypeAndSubjectIdForUpdate(subjectType, subjectId)
            .orElseThrow(ExceptionGateException::notFound);
        gate.lift();                                                    // restores the FULL operation set
        members.persist(new ExceptionAuditEntry(UUID.randomUUID(), gate.getId(), "LIFT", reason, actor,
            Instant.now(clock)));
        return gate;
    }

    /** The subject's OWN primary lifecycle — orthogonal to the exception dimension. */
    @Transactional
    public ExceptionGate advancePrimary(String subjectType, String subjectId, String newState) {
        ExceptionGate gate = getOrCreate(subjectType, subjectId);
        gate.advancePrimary(newState);
        return gate;
    }

    /**
     * EXC-DIM-BLOCK-001 — a GATED operation fails closed BEFORE any mutation while raised;
     * a non-gated operation always succeeds regardless of the flag.
     */
    @Transactional(readOnly = true)
    public void checkAllowed(String subjectType, String subjectId, String operation) {
        if (!GATED_OPERATIONS.contains(operation)) {
            return;                                                      // non-gated — unaffected
        }
        ExceptionGate gate = gates.findBySubjectTypeAndSubjectId(subjectType, subjectId).orElse(null);
        if (gate != null && gate.isRaised()) {
            throw ExceptionGateException.blocked(gate.getReason());
        }
    }

    @Transactional(readOnly = true)
    public ExceptionGate get(String subjectType, String subjectId) {
        return gates.findBySubjectTypeAndSubjectId(subjectType, subjectId)
            .orElseThrow(ExceptionGateException::notFound);
    }

    @Transactional(readOnly = true)
    public List<ExceptionAuditEntry> auditTrail(String subjectType, String subjectId) {
        ExceptionGate gate = get(subjectType, subjectId);
        return gates.findAuditByGateId(gate.getId());
    }

    private ExceptionGate getOrCreate(String subjectType, String subjectId) {
        return gates.findBySubjectTypeAndSubjectIdForUpdate(subjectType, subjectId)
            .orElseGet(() -> gates.save(new ExceptionGate(UUID.randomUUID(), subjectType, subjectId,
                Instant.now(clock))));
    }
}
