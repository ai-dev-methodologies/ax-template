package com.ax.template.authblueprint.payment;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * PAYMENT-RECON-001: append-only event ledger with sha256 hash chain.
 *
 * <p>Each appended event records:
 * <ul>
 *   <li>{@code payload_hash} = sha256(JSON payload bytes).</li>
 *   <li>{@code prev_hash} = previous event's {@code payload_hash} (null for genesis).</li>
 * </ul>
 *
 * <p>Hashes are 64-char lowercase hex strings; chain integrity is verified by
 * {@link PaymentReconciliationTest}.
 */
@Service
public class PaymentEventLedger {

    private final PaymentEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    public PaymentEventLedger(PaymentEventRepository eventRepository, ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Appends a ledger event with hash-chain linkage.
     *
     * <p>Two append modes:
     * <ol>
     *   <li><b>Payment-anchored</b> (paymentId != null) — normal case. The new event's
     *       {@code prev_hash} is set to the previous event's {@code payload_hash}
     *       for the same {@code paymentId}, forming the PAYMENT-RECON-001 hash chain.</li>
     *   <li><b>Orphan audit</b> (paymentId == null) — dogfood-11 R11 GAP-B closure.
     *       Used by {@code PaymentService.auditCallbackFailure} when an inbound
     *       redirect-style PG callback fails signature verification BEFORE the
     *       inbound {@code orderId} can be resolved to a Payment row. Each orphan
     *       audit row is intentionally isolated: {@code prev_hash} is fixed to
     *       {@code null} so the audit row does NOT chain with any other orphan
     *       (preventing a "sentinel chain" from forming, which was the dogfood-10
     *       sentinel-UUID(0,0) workaround's failure mode), and the row is NEVER
     *       picked up as a {@code prev_hash} source for a future event. The
     *       {@code payload.paymentId} field is omitted from the serialized JSON
     *       so {@code sha256(payload)} cannot accidentally collide with a real
     *       payment's genesis event.</li>
     * </ol>
     *
     * @param paymentId payment whose history is being extended; null for orphan audit rows
     * @param type      event type
     * @param amount    canonical amount for the event (CAPTURED→capturedAmount,
     *                  REFUNDED→refund.amount, etc.); may be null for non-amount events
     * @param extras    additional payload keys, merged into the serialized payload
     */
    @Transactional
    public PaymentEvent append(UUID paymentId, PaymentEventType type, BigDecimal amount, Map<String, Object> extras) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (paymentId != null) {
            payload.put("paymentId", paymentId.toString());
        }
        payload.put("type", type.name());
        if (amount != null) {
            payload.put("amount", amount.toPlainString());
        }
        Instant occurredAt = Instant.now();
        payload.put("occurredAt", occurredAt.toString());
        if (extras != null) {
            payload.putAll(extras);
        }

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (JacksonException e) {
            throw new IllegalStateException("ledger payload serialization failed", e);
        }

        // Orphan audit rows (paymentId == null) deliberately skip the prev_hash
        // lookup. Chaining them would either (a) require a separate "orphan
        // chain" repository method that finds by paymentId IS NULL — which would
        // group unrelated signature-fail events from different inbound callbacks
        // into a fake chain — or (b) leave Spring Data JPA's derived query
        // {@code findFirstByPaymentIdOrderByOccurredAtDesc(null)} to map null
        // parameter to a literal {@code = ?} (returning empty by default) and
        // depend on undocumented behavior. Explicit null-skip is the safer
        // semantic: every orphan audit row is a self-contained genesis entry.
        String prevHash = paymentId == null
            ? null
            : eventRepository.findFirstByPaymentIdOrderByOccurredAtDesc(paymentId)
                .map(PaymentEvent::getPayloadHash)
                .orElse(null);

        PaymentEvent event = new PaymentEvent();
        event.setEventId(UUID.randomUUID());
        event.setPaymentId(paymentId);
        event.setType(type);
        event.setOccurredAt(occurredAt);
        event.setPayload(payloadJson);
        event.setPayloadHash(sha256Hex(payloadJson));
        event.setPrevHash(prevHash);
        event.setAmountNumeric(amount);

        return eventRepository.save(event);
    }

    /**
     * Recomputes the ledger-derived balance for a payment:
     * sum(CAPTURED) − sum(REFUNDED) − sum(PARTIAL_REFUNDED).
     */
    public BigDecimal computeLedgerBalance(UUID paymentId) {
        BigDecimal captured = BigDecimal.ZERO;
        BigDecimal refunded = BigDecimal.ZERO;
        for (PaymentEvent e : eventRepository.findByPaymentIdOrderByOccurredAtAsc(paymentId)) {
            if (e.getAmountNumeric() == null) {
                continue;
            }
            if (e.getType() == PaymentEventType.CAPTURED) {
                captured = captured.add(e.getAmountNumeric());
            } else if (e.getType() == PaymentEventType.REFUNDED) {
                refunded = refunded.add(e.getAmountNumeric());
            }
        }
        return captured.subtract(refunded);
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
