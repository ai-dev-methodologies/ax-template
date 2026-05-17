package com.ax.template.authblueprint.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Append-only ledger row. PAYMENT-RECON-001: each event records a sha256
 * {@link #payloadHash} of its serialized payload and the previous event's
 * {@link #prevHash}, forming a tamper-evident hash chain per payment_id.
 *
 * <p>Append-only is enforced two ways:
 * <ol>
 *   <li>Repository exposes no update/delete methods (structural guard).</li>
 *   <li>A DB-level trigger (V003 migration) raises an exception on UPDATE/DELETE.</li>
 * </ol>
 */
@Entity
@Table(
    name = "payment_events",
    indexes = {
        @Index(name = "ix_payment_events_payment_id", columnList = "payment_id"),
        @Index(name = "ix_payment_events_occurred_at", columnList = "occurred_at")
    }
)
public class PaymentEvent {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "payment_id", nullable = false, updatable = false)
    private UUID paymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64, updatable = false)
    private PaymentEventType type;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "payload_hash", nullable = false, length = 64, updatable = false)
    private String payloadHash;

    @Column(name = "prev_hash", length = 64, updatable = false)
    private String prevHash;

    /**
     * JSON-serialized payload. Stored as TEXT/JSON; H2/PostgreSQL both accept varchar(MAX).
     * The query "CAST(payload->>'amount' AS NUMERIC)" used by PaymentReconciliationTest
     * is PostgreSQL syntax — H2 needs a compatible function. PaymentEventLedger also
     * surfaces the amount via {@link #amountNumeric} for portability.
     */
    @Lob
    @Column(nullable = false, updatable = false)
    private String payload;

    /**
     * Convenience column carrying the event's amount as a NUMERIC so the
     * reconciliation invariant query can use a portable SQL expression.
     * Mirrors the {@code payload->>'amount'} extraction without requiring JSONB.
     */
    @Column(name = "amount_numeric", precision = 19, scale = 8, updatable = false)
    private BigDecimal amountNumeric;

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }

    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }

    public PaymentEventType getType() { return type; }
    public void setType(PaymentEventType type) { this.type = type; }

    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }

    public String getPayloadHash() { return payloadHash; }
    public void setPayloadHash(String payloadHash) { this.payloadHash = payloadHash; }

    public String getPrevHash() { return prevHash; }
    public void setPrevHash(String prevHash) { this.prevHash = prevHash; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public BigDecimal getAmountNumeric() { return amountNumeric; }
    public void setAmountNumeric(BigDecimal amountNumeric) { this.amountNumeric = amountNumeric; }
}
