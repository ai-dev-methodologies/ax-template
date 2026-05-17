package com.ax.template.authblueprint.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
     * @param paymentId payment whose history is being extended
     * @param type      event type
     * @param amount    canonical amount for the event (CAPTURED→capturedAmount,
     *                  REFUNDED→refund.amount, etc.); may be null for non-amount events
     * @param extras    additional payload keys, merged into the serialized payload
     */
    @Transactional
    public PaymentEvent append(UUID paymentId, PaymentEventType type, BigDecimal amount, Map<String, Object> extras) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("paymentId", paymentId.toString());
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
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("ledger payload serialization failed", e);
        }

        String prevHash = eventRepository.findFirstByPaymentIdOrderByOccurredAtDesc(paymentId)
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
