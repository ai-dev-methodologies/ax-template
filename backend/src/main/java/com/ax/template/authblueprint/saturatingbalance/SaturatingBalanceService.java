package com.ax.template.authblueprint.saturatingbalance;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * saturating-balance-l0 sole orchestrator. {@code accrue}/{@code debit} always take the
 * balance row's {@code PESSIMISTIC_WRITE} lock FIRST (SATBAL-CONCURRENT-004), then compute the
 * CLAMPED applied amount — accrual never exceeds the remaining headroom to {@code cap}
 * (SATBAL-CEILING-001), debit never exceeds the current balance (SATBAL-FLOOR-002) — and
 * append an immutable {@link LedgerEntry} recording BOTH the requested and applied amounts
 * (SATBAL-LEDGER-003). Neither operation ever throws for an out-of-range request; only a
 * non-positive requested amount is rejected. LedgerEntry rows are members:
 * {@link MemberWriter} writes, root-JPQL reads.
 */
@Service
public class SaturatingBalanceService {

    private final BalanceRepository balances;
    private final MemberWriter members;
    private final SaturatingBalanceMetrics metrics;
    private final Clock clock;

    public SaturatingBalanceService(BalanceRepository balances, MemberWriter members,
                                    SaturatingBalanceMetrics metrics, Clock clock) {
        this.balances = balances;
        this.members = members;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public Balance create(String ownerId, BigDecimal cap) {
        if (cap == null || cap.signum() <= 0) {
            throw SaturatingBalanceException.invalidCap();
        }
        return balances.save(new Balance(UUID.randomUUID(), ownerId, cap, Instant.now(clock)));
    }

    /** SATBAL-CEILING-001 — accrual clamps AT the cap; never errors, never stores above cap. */
    @Transactional
    public LedgerEntry accrue(UUID balanceId, BigDecimal requested) {
        requirePositive(requested);
        Balance balance = balances.findByIdForUpdate(balanceId).orElseThrow(SaturatingBalanceException::notFound);
        BigDecimal applied = requested.min(balance.headroom()).max(BigDecimal.ZERO);
        balance.applyAccrual(applied);
        metrics.record("accrue", applied.compareTo(requested) < 0 ? "clamped" : "ok");
        return members.persist(new LedgerEntry(UUID.randomUUID(), balanceId, LedgerOp.ACCRUE,
            requested, applied, Instant.now(clock)));
    }

    /** SATBAL-FLOOR-002 — debit clamps AT zero; never errors, never stores negative. */
    @Transactional
    public LedgerEntry debit(UUID balanceId, BigDecimal requested) {
        requirePositive(requested);
        Balance balance = balances.findByIdForUpdate(balanceId).orElseThrow(SaturatingBalanceException::notFound);
        BigDecimal applied = requested.min(balance.getCurrent()).max(BigDecimal.ZERO);
        balance.applyDebit(applied);
        metrics.record("debit", applied.compareTo(requested) < 0 ? "clamped" : "ok");
        return members.persist(new LedgerEntry(UUID.randomUUID(), balanceId, LedgerOp.DEBIT,
            requested, applied.negate(), Instant.now(clock)));
    }

    @Transactional(readOnly = true)
    public Balance get(UUID balanceId) {
        return balances.findById(balanceId).orElseThrow(SaturatingBalanceException::notFound);
    }

    @Transactional(readOnly = true)
    public List<LedgerEntry> ledgerOf(UUID balanceId) {
        get(balanceId);                                           // 404 before an empty list
        return balances.findLedger(balanceId);
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw SaturatingBalanceException.invalidAmount();
        }
    }
}
