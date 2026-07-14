package com.ax.template.authblueprint.thresholdfiling;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * threshold-filing-obligation-l0 sole orchestrator. {@link #accrue} acquires the register row
 * under PESSIMISTIC_WRITE (mirrors ThresholdRegisterService) and, in the SAME transaction as the
 * crossing accrual, flips the register to TRIGGERED via {@link FilingRegisterStateMachine} AND
 * binds the {@link FilingObligation} member via {@link MemberWriter} — ONE aggregate root
 * (FilingRegister) mutated, satisfying HG-ANTI-GODSERVICE-TX even though two DIFFERENT facts
 * (register state + filing record) are established atomically (TFO-TRIGGER-001). {@link #acknowledge}
 * is the ONLY terminal writer for the bound filing (TFO-DEADLINE-001) — there is no silent expiry.
 */
@Service
public class FilingService {

    static final int MEASURE_SCALE = 4;
    static final int MAX_PAGE_SIZE = 200;

    private final FilingRegisterRepository registers;
    private final MemberWriter members;
    private final FilingRegisterStateMachine stateMachine;
    private final FilingMetrics metrics;
    private final Clock clock;

    public FilingService(FilingRegisterRepository registers, MemberWriter members,
                         FilingRegisterStateMachine stateMachine, FilingMetrics metrics, Clock clock) {
        this.registers = registers;
        this.members = members;
        this.stateMachine = stateMachine;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public FilingRegister createRegister(String subjectKey, BigDecimal threshold) {
        if (threshold == null || threshold.signum() <= 0) {
            metrics.record("create", "invalid");
            throw FilingException.invalidValue();
        }
        if (registers.existsBySubjectKey(subjectKey)) {
            metrics.record("create", "rejected");
            throw FilingException.duplicateSubject();
        }
        try {
            FilingRegister r = registers.saveAndFlush(new FilingRegister(UUID.randomUUID(), subjectKey,
                threshold.setScale(MEASURE_SCALE), Instant.now(clock)));
            metrics.record("create", "ok");
            return r;
        } catch (DataIntegrityViolationException e) {
            metrics.record("create", "rejected");
            throw FilingException.duplicateSubject();
        }
    }

    /** TFO-TRIGGER-001 — accrue under the row lock; the crossing accrual binds the filing
     *  obligation in this SAME transaction. */
    @Transactional
    public FilingRegister accrue(String subjectKey, BigDecimal delta) {
        FilingRegister r = registers.findBySubjectKeyForUpdate(subjectKey)
            .orElseThrow(FilingException::notFound);
        if (r.getStatus().isTerminal()) {                              // late accrual — no re-trigger
            metrics.record("accrue", "triggered");
            throw FilingException.triggered();
        }
        if (delta == null || delta.signum() <= 0) {
            metrics.record("accrue", "invalid");
            throw FilingException.invalidValue();
        }
        BigDecimal next = r.getAccruedValue().add(delta.setScale(MEASURE_SCALE));
        r.advanceAccrual(next);
        if (next.compareTo(r.getThreshold()) >= 0) {
            Instant now = Instant.now(clock);
            stateMachine.trigger(r);                                  // SAME transaction — one atomic fact
            members.persist(new FilingObligation(UUID.randomUUID(), r.getId(), r.getSubjectKey(),
                r.getThreshold(), now));
            metrics.record("accrue", "triggered");
        } else {
            metrics.record("accrue", "ok");
        }
        return r;
    }

    /** TFO-DEADLINE-001 — the ONLY terminal edge for the bound filing; who/when recorded. */
    @Transactional
    public FilingObligation acknowledge(String subjectKey, String acknowledger) {
        FilingRegister r = registers.findBySubjectKeyForUpdate(subjectKey)
            .orElseThrow(FilingException::notFound);
        FilingObligation f = registers.findFilingObligation(r.getId()).orElseThrow(FilingException::notFound);
        if (f.getStatus() == FilingObligationStatus.ACKNOWLEDGED) {
            metrics.record("ack", "rejected");
            throw FilingException.alreadyAcknowledged();
        }
        f.acknowledge(acknowledger, Instant.now(clock));
        metrics.record("ack", "ok");
        return f;
    }

    @Transactional(readOnly = true)
    public FilingRegister getRegister(String subjectKey) {
        return registers.findBySubjectKey(subjectKey).orElseThrow(FilingException::notFound);
    }

    @Transactional(readOnly = true)
    public FilingObligation getFiling(String subjectKey) {
        return registers.findFilingObligation(getRegister(subjectKey).getId()).orElseThrow(FilingException::notFound);
    }

    @Transactional(readOnly = true)
    public Page<FilingObligation> overdueOpen(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return registers.findOverdueOpen(Instant.now(clock), PageRequest.of(safePage, safeSize));
    }
}
