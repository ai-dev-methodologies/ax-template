package com.ax.template.authblueprint.reservation;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    /** Re-read the hold under lock (after the balance lock) — fresh DB state, never the stale L1 peek. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reservation r WHERE r.id = :id")
    Optional<Reservation> findByIdForUpdate(@Param("id") UUID id);

    /** Scalar projection to discover the parent balance id WITHOUT loading a managed Reservation
     *  (so the subsequent locking re-read is a fresh load, not an L1-cached stale instance). */
    @Query("SELECT r.balanceId FROM Reservation r WHERE r.id = :id")
    Optional<UUID> findBalanceId(@Param("id") UUID id);

    /** Append-ordered holds of one balance (paginated, never an unbounded list). */
    Page<Reservation> findByBalanceIdOrderByCreatedAtAsc(UUID balanceId, Pageable pageable);

    /** RSV-SWEEP-001 — bounded batch of due (expired) OUTSTANDING hold ids for the timeout sweep. */
    @Query("SELECT r.id FROM Reservation r WHERE r.status = :status AND r.expiresAt <= :now ORDER BY r.expiresAt ASC")
    List<UUID> findDueIds(@Param("status") ReservationStatus status, @Param("now") Instant now, Pageable pageable);

    /** RSV-SWEEP-001 reconciliation truth — reserved must equal Σ amount of OUTSTANDING holds. */
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Reservation r WHERE r.balanceId = :balanceId AND r.status = :status")
    BigDecimal sumByStatus(@Param("balanceId") UUID balanceId, @Param("status") ReservationStatus status);
}
