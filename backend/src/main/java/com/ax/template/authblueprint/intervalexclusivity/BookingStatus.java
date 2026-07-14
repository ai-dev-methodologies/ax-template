package com.ax.template.authblueprint.intervalexclusivity;

/** A booking's lifecycle — CANCELLED is terminal (zero outgoing edges). */
public enum BookingStatus {
    ACTIVE,
    CANCELLED
}
