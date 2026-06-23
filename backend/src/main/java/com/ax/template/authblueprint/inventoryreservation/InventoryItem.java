package com.ax.template.authblueprint.inventoryreservation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * two-axis-inventory-reservation-l0 root: one stock item tracking exactly TWO persisted
 * quantities — {@code onHand} (the physical goods present) and {@code reserved} (the quantity
 * held against open demand). The third quantity every order system needs, AVAILABLE, is the
 * DERIVED projection {@link #available()} = onHand − reserved — never a stored column, so it
 * cannot drift from its two axes (INVRES-RESERVE-001 / INVRES-CONSERVE-001).
 *
 * <p>The two-phase hold mutates the axes ONLY through the package-private sole-mutator hooks,
 * called by {@link InventoryReservationService} under the item's PESSIMISTIC_WRITE row lock
 * (INVRES-CONCURRENT-001): {@link #reserve} increments reserved (the hold); {@link #commitReservation}
 * decrements BOTH axes (the goods leave); {@link #releaseReservation} decrements reserved alone
 * (the hold frees). The {@code @Check reserved >= 0 AND reserved <= on_hand} is the DB backstop
 * that makes an over-reserve or a broken conservation unrepresentable (INVRES-CONSERVE-001).
 */
@AggregateRoot
@Entity
@Table(name = "inventory_items")
@Check(constraints = "on_hand >= 0 AND reserved >= 0 AND reserved <= on_hand")
public class InventoryItem {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The item's external reference (SKU / stock-location id) — opaque, recorded verbatim. */
    @Column(name = "sku", nullable = false, updatable = false, length = 200)
    private String sku;

    /** Physical goods present at this location. Reduced ONLY when a held reservation commits. */
    @Column(name = "on_hand", nullable = false)
    private long onHand;

    /** Quantity currently held against open demand. reserved == Σ(HELD reservation quantities). */
    @Column(name = "reserved", nullable = false)
    private long reserved;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected InventoryItem() {}

    public InventoryItem(UUID id, String sku, long onHand, Instant createdAt) {
        this.id = id;
        this.sku = sku;
        this.onHand = onHand;
        this.reserved = 0L;
        this.createdAt = createdAt;
    }

    /** DERIVED available (INVRES-RESERVE-001) — onHand − reserved; NEVER a stored column. */
    public long available() {
        return onHand - reserved;
    }

    /** Sole-mutator hook — place a hold: reserved += q (onHand untouched). The caller MUST have
     *  gated on {@link #available()} ≥ q under the item's row lock first (INVRES-RESERVE-001). */
    void reserve(long quantity) {
        this.reserved += quantity;
    }

    /** Sole-mutator hook — commit a held quantity: BOTH onHand and reserved fall by q; the goods
     *  physically leave the location (INVRES-COMMIT-001). */
    void commitReservation(long quantity) {
        this.onHand -= quantity;
        this.reserved -= quantity;
    }

    /** Sole-mutator hook — release a held quantity: reserved falls by q, onHand untouched; the
     *  hold is freed so available grows back (INVRES-RELEASE-001). */
    void releaseReservation(long quantity) {
        this.reserved -= quantity;
    }

    public UUID getId() { return id; }
    public String getSku() { return sku; }
    public long getOnHand() { return onHand; }
    public long getReserved() { return reserved; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
