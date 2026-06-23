package com.ax.template.authblueprint.inventoryreservation;

/**
 * two-axis-inventory-reservation-l0 reservation lifecycle (INVRES-COMMIT/RELEASE-001). A hold
 * starts HELD and moves to exactly ONE terminal — COMMITTED (the goods left: onHand and reserved
 * both fell) or RELEASED (the hold was freed: reserved fell, onHand untouched). Neither terminal
 * has an outgoing edge: a second commit/release on a non-HELD reservation is a deterministic 409.
 */
public enum ReservationStatus {
    HELD,
    COMMITTED,
    RELEASED
}
