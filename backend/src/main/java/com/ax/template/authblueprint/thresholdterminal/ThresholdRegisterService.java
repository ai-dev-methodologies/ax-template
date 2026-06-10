package com.ax.template.authblueprint.thresholdterminal;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * threshold-terminal-derivation-l0 sole orchestrator. Every accrual AND every use acquires the
 * register row under PESSIMISTIC_WRITE (TTD-CONCURRENT-001) so the two write-paths serialize on one
 * lock. The accrual that makes the anchor reach/cross the limit is ACCEPTED (exact overshoot recorded)
 * and drives EXPIRED via {@link ThresholdRegisterStateMachine} in the SAME transaction
 * (TTD-CROSS-001) — there is never a committed live over-limit row (backstopped by the entity @Check,
 * TTD-CHECK-001). A late accrual or a use on an EXPIRED register is a deterministic 409
 * (TTD-TERMINAL-001 / TTD-DERIVE-001) with the anchor untouched.
 */
@Service
public class ThresholdRegisterService {

    static final int MEASURE_SCALE = 4;

    private final ThresholdRegisterRepository registers;
    private final ThresholdRegisterStateMachine stateMachine;
    private final ThresholdMetrics metrics;
    private final Clock clock;

    public ThresholdRegisterService(ThresholdRegisterRepository registers,
                                    ThresholdRegisterStateMachine stateMachine,
                                    ThresholdMetrics metrics, Clock clock) {
        this.registers = registers;
        this.stateMachine = stateMachine;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public ThresholdRegister createRegister(String scopeKey, BigDecimal limit, BigDecimal initialAnchor) {
        if (limit == null || limit.signum() <= 0) {
            metrics.record("create", "invalid");
            throw ThresholdException.invalidValue();
        }
        BigDecimal lim = limit.setScale(MEASURE_SCALE);
        BigDecimal anchor = (initialAnchor == null ? BigDecimal.ZERO : initialAnchor).setScale(MEASURE_SCALE);
        if (anchor.signum() < 0 || anchor.compareTo(lim) >= 0) {     // a register is born LIVE: anchor in [0, limit)
            metrics.record("create", "invalid");
            throw ThresholdException.invalidValue();
        }
        if (registers.existsByScopeKey(scopeKey)) {
            metrics.record("create", "rejected");
            throw ThresholdException.duplicateScope();
        }
        try {
            ThresholdRegister r = registers.saveAndFlush(
                new ThresholdRegister(UUID.randomUUID(), scopeKey, lim, anchor, Instant.now(clock)));
            metrics.record("create", "ok");
            return r;
        } catch (DataIntegrityViolationException e) {
            metrics.record("create", "rejected");
            throw ThresholdException.duplicateScope();
        }
    }

    /** TTD-CROSS-001 / TTD-TERMINAL-001 / TTD-CONCURRENT-001 — accrue under the row lock; the crossing
     *  accrual drives EXPIRED in this same transaction. */
    @Transactional
    public ThresholdRegister accrue(String scopeKey, BigDecimal delta) {
        ThresholdRegister r = registers.findByScopeKeyForUpdate(scopeKey)
            .orElseThrow(ThresholdException::notFound);
        if (r.getStatus().isTerminal()) {                            // late accrual — anchor untouched
            metrics.record("accrue", "terminal");
            throw ThresholdException.terminal();
        }
        if (delta == null || delta.signum() <= 0) {
            metrics.record("accrue", "invalid");
            throw ThresholdException.invalidValue();
        }
        BigDecimal next = r.getAnchor().add(delta.setScale(MEASURE_SCALE));   // exact; overshoot recorded
        r.advanceAnchor(next);
        if (next.compareTo(r.getLimit()) >= 0) {
            stateMachine.expire(r);                                  // SAME transaction — one atomic fact
            metrics.record("accrue", "crossed");
        } else {
            metrics.record("accrue", "ok");
        }
        return r;
    }

    /** TTD-DERIVE-001 — the derived capability (install / dispatch / use): fail-closed on the SAME
     *  locked row; using is not accruing — the anchor is untouched. */
    @Transactional
    public ThresholdRegister use(String scopeKey) {
        ThresholdRegister r = registers.findByScopeKeyForUpdate(scopeKey)
            .orElseThrow(ThresholdException::notFound);
        if (r.getStatus().isTerminal()) {
            metrics.record("use", "terminal");
            throw ThresholdException.terminal();
        }
        metrics.record("use", "ok");
        return r;
    }

    @Transactional(readOnly = true)
    public ThresholdRegister getRegister(String scopeKey) {
        return registers.findByScopeKey(scopeKey).orElseThrow(ThresholdException::notFound);
    }
}
