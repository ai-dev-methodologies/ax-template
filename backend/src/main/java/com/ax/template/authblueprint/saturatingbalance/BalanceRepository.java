package com.ax.template.authblueprint.saturatingbalance;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BalanceRepository extends JpaRepository<Balance, UUID> {

    /** SATBAL-CONCURRENT-004 — the balance row is the serialization point for every accrue/debit. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Balance b WHERE b.id = :id")
    Optional<Balance> findByIdForUpdate(@Param("id") UUID id);

    // ── through-root member reads (HG-AGG-REPO — LedgerEntry owns no repository) ──

    @Query("SELECT l FROM LedgerEntry l WHERE l.balanceId = :balanceId ORDER BY l.occurredAt ASC")
    List<LedgerEntry> findLedger(@Param("balanceId") UUID balanceId);

    /** Independent conservation derivation — repo SUM, a different code path than the balance row. */
    @Query("SELECT COALESCE(SUM(l.appliedAmount), 0) FROM LedgerEntry l WHERE l.balanceId = :balanceId")
    BigDecimal sumApplied(@Param("balanceId") UUID balanceId);
}
