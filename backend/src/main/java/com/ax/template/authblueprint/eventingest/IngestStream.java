package com.ax.template.authblueprint.eventingest;

/**
 * Fixed, bounded-cardinality stream enum (INGEST-OBSERVABILITY-001 requires a fixed
 * stream-name enum, never a free-form string, so the metric label stays bounded).
 * The three names mirror the IDW6 logistics-dogfood findings this spec generalizes:
 * a carrier webhook (shipment), a telemetry stream (device), a payment-provider callback.
 */
public enum IngestStream {
    SHIPMENT_STATUS,
    DEVICE_TELEMETRY,
    PAYMENT_STATUS
}
