package com.ax.template.authblueprint.inventoryreservation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * One hold against an {@link InventoryItem} (INVRES-RESERVE-001). The reserved quantity and the
 * item it holds against are immutable ({@code @Column(updatable=false)}); only the {@code status}
 * moves — HELD → (COMMITTED | RELEASED) exactly once, mutated SOLELY by {@link ReservationStateMachine}.
 * A committed reservation means the goods left (item onHand and reserved both fell by q); a released
 * one means the hold was freed (item reserved fell by q, onHand untouched).
 *
 * <p>This is an {@code @AggregateMember} of {@link InventoryItem}: it owns no repository — the root's
 * service writes it through {@code common/MemberWriter} and reads it via root-JPQL (HG-AGG-REPO).
 * Named {@code InventoryReservation} (not bare {@code Reservation}) so its JPA entity name and DDD
 * member identity do not collide with the telecom reservation domain's {@code Reservation} root.
 */
@AggregateMember(root = InventoryItem.class)
@Entity
@Table(name = "inventory_reservations")
public class InventoryReservation {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "item_id", nullable = false, updatable = false)
    private UUID itemId;

    @Column(name = "quantity", nullable = false, updatable = false)
    private long quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "actor", nullable = false, updatable = false, length = 200)
    private String actor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected InventoryReservation() {}

    public InventoryReservation(UUID id, UUID itemId, long quantity, ReservationStatus status,
                                String actor, Instant createdAt) {
        this.id = id;
        this.itemId = itemId;
        this.quantity = quantity;
        this.status = status;
        this.actor = actor;
        this.createdAt = createdAt;
    }

    /** Sole status mutator — package-private; invoked ONLY by {@link ReservationStateMachine}
     *  (HG-STATE-SOLE-MUTATOR). Models the HELD → (COMMITTED | RELEASED) exactly-once edges. */
    void setStatus(ReservationStatus next) {
        this.status = next;
    }

    public UUID getId() { return id; }
    public UUID getItemId() { return itemId; }
    public long getQuantity() { return quantity; }
    public ReservationStatus getStatus() { return status; }
    public String getActor() { return actor; }
    public Instant getCreatedAt() { return createdAt; }
}
