package com.ax.template.authblueprint.inventoryreservation;

import org.springframework.http.HttpStatus;

/** Domain exception for two-axis-inventory-reservation. status + RFC 9457 type + machine code. */
public class InventoryReservationException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private InventoryReservationException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static InventoryReservationException itemNotFound() {
        return new InventoryReservationException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Inventory item not found");
    }

    public static InventoryReservationException reservationNotFound() {
        return new InventoryReservationException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Reservation not found");
    }

    /** INVRES-RESERVE-001 — derived available < requested quantity; nothing is mutated. */
    public static InventoryReservationException insufficientAvailable() {
        return new InventoryReservationException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:inventory-insufficient-available", "INVENTORY_INSUFFICIENT_AVAILABLE",
            "Insufficient available inventory — available (onHand − reserved) is less than the requested quantity");
    }

    /** INVRES-COMMIT/RELEASE-001 — committing/releasing a reservation that is not HELD (exactly-once). */
    public static InventoryReservationException reservationNotHeld() {
        return new InventoryReservationException(HttpStatus.CONFLICT,
            "urn:problem:inventory-reservation-not-held", "INVENTORY_RESERVATION_NOT_HELD",
            "Reservation is not HELD — a hold commits or releases exactly once and cannot transition again");
    }
}
