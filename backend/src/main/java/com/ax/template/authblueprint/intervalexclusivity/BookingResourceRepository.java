package com.ax.template.authblueprint.intervalexclusivity;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** NO delete method — a resource and its bookings are never removed, only cancelled (append-only lifecycle). */
public interface BookingResourceRepository extends JpaRepository<BookingResource, UUID> {

    Optional<BookingResource> findByResourceKey(String resourceKey);

    boolean existsByResourceKey(String resourceKey);

    /** IVX-CONCURRENT-002 — the resource row is the SINGLE serialization point for every booking
     *  create/resize/cancel on it (H2-honest: no GiST EXCLUDE available, see the spec). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM BookingResource r WHERE r.resourceKey = :key")
    Optional<BookingResource> findByResourceKeyForUpdate(@Param("key") String key);

    // ── through-root member reads (HG-AGG-REPO — Booking owns no repository) ──

    @Query("SELECT b FROM Booking b WHERE b.resourceKey = :key ORDER BY b.startAt ASC")
    List<Booking> findBookings(@Param("key") String key, Pageable pageable);

    /** IVX-OVERLAP-001 — any ACTIVE booking overlapping [start, end) on this resource. */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.resourceKey = :key"
        + " AND b.status = com.ax.template.authblueprint.intervalexclusivity.BookingStatus.ACTIVE"
        + " AND b.startAt < :end AND b.endAt > :start")
    long countOverlappingAny(@Param("key") String key, @Param("start") Instant start, @Param("end") Instant end);

    /** IVX-MUTATE-003 — the same overlap check, excluding the booking being resized itself. */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.resourceKey = :key"
        + " AND b.status = com.ax.template.authblueprint.intervalexclusivity.BookingStatus.ACTIVE"
        + " AND b.id <> :excludeId AND b.startAt < :end AND b.endAt > :start")
    long countOverlappingExcluding(@Param("key") String key, @Param("excludeId") UUID excludeId,
                                   @Param("start") Instant start, @Param("end") Instant end);
}
