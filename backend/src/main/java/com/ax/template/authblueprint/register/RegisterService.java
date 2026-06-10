package com.ax.template.authblueprint.register;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.MemberWriter;

/**
 * monotone-register-l0 sole orchestrator. Every append acquires the register row under
 * PESSIMISTIC_WRITE (REG-CONCURRENT-001) so a delta is never computed against a stale anchor. A NORMAL
 * read must be ≥ the anchor (REG-MONOTONE-001, delta = read − anchor); a ROLLOVER records the wrapped
 * delta (modulus − anchor) + read (REG-ROLLOVER-001); an EXCHANGE resets the baseline with delta 0
 * (REG-EXCHANGE-001). The anchor moves ONLY here (no public setter). Reads are append-only.
 */
@Service
public class RegisterService {

    static final int MEASURE_SCALE = 4;

    private final RegisterRepository registers;
    private final MemberWriter members;
    private final RegisterMetrics metrics;
    private final Clock clock;

    public RegisterService(RegisterRepository registers, MemberWriter members,
                           RegisterMetrics metrics, Clock clock) {
        this.registers = registers;
        this.members = members;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public Register createRegister(String scopeKey, BigDecimal modulus, BigDecimal initialAnchor) {
        if (modulus == null || modulus.signum() <= 0) {
            metrics.record("create", "invalid");
            throw RegisterException.invalidReading();
        }
        BigDecimal mod = modulus.setScale(MEASURE_SCALE);
        BigDecimal anchor = (initialAnchor == null ? BigDecimal.ZERO : initialAnchor).setScale(MEASURE_SCALE);
        if (anchor.signum() < 0 || anchor.compareTo(mod) >= 0) {     // anchor in [0, modulus)
            metrics.record("create", "invalid");
            throw RegisterException.invalidReading();
        }
        if (registers.existsByScopeKey(scopeKey)) {
            metrics.record("create", "rejected");
            throw RegisterException.duplicateScope();
        }
        try {
            Register r = registers.saveAndFlush(new Register(UUID.randomUUID(), scopeKey, mod, anchor, Instant.now(clock)));
            metrics.record("create", "ok");
            return r;
        } catch (DataIntegrityViolationException e) {
            metrics.record("create", "rejected");
            throw RegisterException.duplicateScope();
        }
    }

    /** REG-MONOTONE/DELTA/ROLLOVER/EXCHANGE-001 — append one read under the register row lock. */
    @Transactional
    public RegisterReading append(String scopeKey, ReadingKind kind, BigDecimal readingValue, String reason) {
        Register r = registers.findByScopeKeyForUpdate(scopeKey).orElseThrow(RegisterException::notFound);
        BigDecimal read = requireInRange(readingValue, r.getModulus(), kind);
        BigDecimal anchor = r.getAnchor();
        BigDecimal delta = switch (kind) {
            case NORMAL -> {
                if (read.compareTo(anchor) < 0) {               // a decrease needs a governed exception
                    metrics.record("NORMAL", "not_monotone");
                    throw RegisterException.notMonotone();
                }
                yield read.subtract(anchor);                     // delta ≥ 0
            }
            case ROLLOVER -> {
                requireReason(reason, "ROLLOVER");
                if (read.compareTo(anchor) >= 0) {              // a wrap means the new read is BELOW the anchor
                    metrics.record("ROLLOVER", "invalid");
                    throw RegisterException.invalidReading();
                }
                yield r.getModulus().subtract(anchor).add(read);  // (modulus − anchor) + read, ≥ 0
            }
            case EXCHANGE -> {
                requireReason(reason, "EXCHANGE");
                if (read.compareTo(anchor) >= 0) {              // a device swap resets the baseline DOWN;
                    metrics.record("EXCHANGE", "invalid");       // an upward EXCHANGE would silently drop real
                    throw RegisterException.invalidReading();    // consumption (delta 0) — i.e. erase billing
                }
                yield BigDecimal.ZERO.setScale(MEASURE_SCALE);    // baseline reset; no seam consumption
            }
        };
        long seq = registers.maxSequence(r.getId()) + 1;
        String storedReason = kind.isGovernedException() ? reason.strip() : null;
        RegisterReading row = members.persist(new RegisterReading(UUID.randomUUID(), r.getId(), kind,
            read, anchor, delta.setScale(MEASURE_SCALE), seq, storedReason, Instant.now(clock)));
        r.advanceAnchor(read);                                   // anchor := read (under the lock)
        metrics.record(kind.name(), "ok");
        return row;
    }

    @Transactional(readOnly = true)
    public Register getRegister(String scopeKey) {
        return registers.findByScopeKey(scopeKey).orElseThrow(RegisterException::notFound);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalConsumption(String scopeKey) {
        Register r = registers.findByScopeKey(scopeKey).orElseThrow(RegisterException::notFound);
        return registers.sumDelta(r.getId()).setScale(MEASURE_SCALE);
    }

    @Transactional(readOnly = true)
    public Page<RegisterReading> listReadings(String scopeKey, int page, int size) {
        Register r = registers.findByScopeKey(scopeKey).orElseThrow(RegisterException::notFound);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);
        return registers.findReadingsPage(r.getId(), PageRequest.of(safePage, safeSize));
    }

    private BigDecimal requireInRange(BigDecimal v, BigDecimal modulus, ReadingKind kind) {
        if (v == null || v.signum() < 0 || v.compareTo(modulus) >= 0) {   // every read lives in [0, modulus)
            metrics.record(kind.name(), "invalid");
            throw RegisterException.invalidReading();
        }
        return v.setScale(MEASURE_SCALE);
    }

    private void requireReason(String reason, String kind) {
        if (reason == null || reason.isBlank()) {
            metrics.record(kind, "reason_required");
            throw RegisterException.reasonRequired();
        }
    }
}
