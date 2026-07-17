package com.ax.template.authblueprint.cashinlieu;

import com.ax.template.authblueprint.common.IdempotentInsert;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * cash-in-lieu-l0 sole orchestrator. {@code rawEntitlement = holdingQuantity * ratio}, stored at
 * share-scale 6 (rounded HALF_UP from the exact product — a documented, deterministic step). It
 * splits into an integer {@code unitsInKind} (floor) and a {@code fractionalRemainder =
 * rawEntitlement - unitsInKind}, computed by pure `BigDecimal` subtraction so the two reconstruct the
 * STORED raw entitlement EXACTLY (CIL-CONSERVE-002) — this is independent of the SEPARATE currency
 * rounding that converts the remainder into {@code cashValue} at the {@code cashRate} snapshot
 * (CIL-FRACTION-001). Allocation is idempotent per (subjectRef, eventRef): the FIRST request computes
 * and freezes the row; {@code uq(subject_ref, event_ref)} backstops a concurrent double-allocate — the
 * losing writer catches the violation and returns the winner's row (CIL-IDEMPOTENT-003).
 */
@Service
public class CashInLieuService {

    static final int SHARE_SCALE = 6;
    static final int CASH_SCALE = 2;

    private final CashInLieuAllocationRepository allocations;
    private final IdempotentInsert idempotentInsert;
    private final Clock clock;

    public CashInLieuService(CashInLieuAllocationRepository allocations,
                             IdempotentInsert idempotentInsert, Clock clock) {
        this.allocations = allocations;
        this.idempotentInsert = idempotentInsert;
        this.clock = clock;
    }

    @Transactional
    public CashInLieuAllocation allocate(String subjectRef, String eventRef, BigDecimal holdingQuantity,
                                         BigDecimal ratio, BigDecimal cashRate) {
        return allocations.findBySubjectRefAndEventRef(subjectRef, eventRef).orElseGet(() -> {
            if (holdingQuantity == null || holdingQuantity.signum() <= 0) {
                throw CashInLieuException.invalidHoldingQuantity();
            }
            if (ratio == null || ratio.signum() <= 0) {
                throw CashInLieuException.invalidRatio();
            }
            if (cashRate == null || cashRate.signum() <= 0) {
                throw CashInLieuException.invalidCashRate();
            }

            BigDecimal raw = holdingQuantity.multiply(ratio).setScale(SHARE_SCALE, RoundingMode.HALF_UP);
            BigDecimal unitsInKindDecimal = raw.setScale(0, RoundingMode.FLOOR);   // CIL-FRACTION-001
            long unitsInKind = unitsInKindDecimal.longValueExact();
            BigDecimal fractionalRemainder = raw.subtract(unitsInKindDecimal);     // exact — CIL-CONSERVE-002
            BigDecimal cashValue = fractionalRemainder.multiply(cashRate).setScale(CASH_SCALE, RoundingMode.HALF_UP);

            try {
                // P1-64 — isolate the racy insert in a REQUIRES_NEW inner tx so a uq(subject,event)
                // violation aborts only that inner tx; the catch-block requery below runs in this
                // (unpoisoned) outer tx even on PostgreSQL (25P02).
                return idempotentInsert.insert(() -> allocations.saveAndFlush(
                    new CashInLieuAllocation(UUID.randomUUID(), subjectRef, eventRef,
                        raw, unitsInKind, fractionalRemainder, cashRate, cashValue, Instant.now(clock))));
            } catch (DataIntegrityViolationException e) {                 // lost the uq(subject,event) race
                return allocations.findBySubjectRefAndEventRef(subjectRef, eventRef)
                    .orElseThrow(CashInLieuException::allocationNotFound);
            }
        });
    }

    @Transactional(readOnly = true)
    public CashInLieuAllocation get(String subjectRef, String eventRef) {
        return allocations.findBySubjectRefAndEventRef(subjectRef, eventRef)
            .orElseThrow(CashInLieuException::allocationNotFound);
    }
}
