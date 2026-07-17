package com.ax.template.authblueprint.withholdingsplit;

import com.ax.template.authblueprint.common.IdempotentInsert;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * withholding-split-l0 sole orchestrator for {@link RemittanceRun} (WHT-REMIT-003). The FIRST
 * collection for a period sums every WITHHOLDING leg posted in that period and freezes the result;
 * every subsequent collection for the SAME period returns that frozen row UNCHANGED — it is never
 * re-summed even if more postings land in the period afterward. {@code uq(period)} backstops a
 * concurrent double-collect: the losing writer catches the constraint violation and re-fetches the
 * winner's row (the same pattern {@code ThresholdRegisterService#createRegister} and
 * {@code DivisibilityService#declare} already use in this catalog).
 */
@Service
public class RemittanceService {

    private static final Pattern PERIOD = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

    private final RemittanceRunRepository remittances;
    private final WithholdingPostingRepository postings;
    private final IdempotentInsert idempotentInsert;
    private final Clock clock;

    public RemittanceService(RemittanceRunRepository remittances, WithholdingPostingRepository postings,
                             IdempotentInsert idempotentInsert, Clock clock) {
        this.remittances = remittances;
        this.postings = postings;
        this.idempotentInsert = idempotentInsert;
        this.clock = clock;
    }

    @Transactional
    public RemittanceRun collect(String period) {
        if (period == null || !PERIOD.matcher(period).matches()) {
            throw WithholdingSplitException.invalidPeriod();
        }
        return remittances.findByPeriod(period).orElseGet(() -> {
            BigDecimal total = postings.sumWithholdingForPeriod(period);
            long count = postings.countByPeriod(period);
            try {
                // P1-64 — isolate the racy insert in a REQUIRES_NEW inner tx so a uq(period)
                // violation aborts only that inner tx; the catch-block requery runs in this
                // (unpoisoned) outer tx even on PostgreSQL (25P02).
                return idempotentInsert.insert(() -> remittances.saveAndFlush(
                    new RemittanceRun(UUID.randomUUID(), period, total, (int) count, Instant.now(clock))));
            } catch (DataIntegrityViolationException e) {                 // lost the uq(period) race
                return remittances.findByPeriod(period).orElseThrow(WithholdingSplitException::remittanceNotFound);
            }
        });
    }

    @Transactional(readOnly = true)
    public RemittanceRun get(String period) {
        return remittances.findByPeriod(period).orElseThrow(WithholdingSplitException::remittanceNotFound);
    }
}
