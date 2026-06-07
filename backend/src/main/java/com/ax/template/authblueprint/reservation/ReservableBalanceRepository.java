package com.ax.template.authblueprint.reservation;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ReservableBalanceRepository extends JpaRepository<ReservableBalance, UUID> {

    Optional<ReservableBalance> findByScopeKey(String scopeKey);

    boolean existsByScopeKey(String scopeKey);

    /** RSV-RESERVE-001 — lock the balance so concurrent reservers serialize: each computes available
     *  against the freshly-committed reserved/committed terms, so the granted holds can never
     *  collectively over-reserve past the balance. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM ReservableBalance b WHERE b.scopeKey = :scopeKey")
    Optional<ReservableBalance> findByScopeKeyForUpdate(@Param("scopeKey") String scopeKey);

    /** Settle/release/sweep lock the balance by id FIRST (deterministic order: balance before hold). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM ReservableBalance b WHERE b.id = :id")
    Optional<ReservableBalance> findByIdForUpdate(@Param("id") UUID id);
}
