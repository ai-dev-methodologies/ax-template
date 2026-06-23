package com.ax.template.authblueprint.inventoryreservation;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** NO delete method is declared anywhere in this domain — an item/reservation is never removed. */
public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    /** INVRES-CONCURRENT-001 — the item row serializes the read-available / write-reserved sequence. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryItem i WHERE i.id = :id")
    Optional<InventoryItem> findByIdForUpdate(@Param("id") UUID id);

    // ── through-root member reads (HG-AGG-REPO — Reservation owns no repository) ──

    @Query("SELECT r FROM InventoryReservation r WHERE r.id = :id")
    Optional<InventoryReservation> findReservation(@Param("id") UUID id);

    @Query("SELECT r FROM InventoryReservation r WHERE r.itemId = :itemId ORDER BY r.createdAt ASC, r.id ASC")
    List<InventoryReservation> findReservations(@Param("itemId") UUID itemId, Pageable pageable);

    /** INVRES-CONSERVE-001 — the sum of still-HELD reservation quantities (the conservation check). */
    @Query("SELECT COALESCE(SUM(r.quantity), 0) FROM InventoryReservation r "
        + "WHERE r.itemId = :itemId AND r.status = com.ax.template.authblueprint.inventoryreservation.ReservationStatus.HELD")
    long sumHeldQuantity(@Param("itemId") UUID itemId);
}
