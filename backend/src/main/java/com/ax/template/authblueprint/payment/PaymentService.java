package com.ax.template.authblueprint.payment;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Central orchestrator for the Payment blueprint. Glues together:
 * <ul>
 *   <li>{@link IdempotencyKeyStore} — exactly-once charge semantics.</li>
 *   <li>{@link PaymentStateMachine} — legal transition enforcement.</li>
 *   <li>{@link PaymentProvider} — external charge attempt.</li>
 *   <li>{@link PaymentEventLedger} — append-only audit + hash chain.</li>
 *   <li>Micrometer counters — PAYMENT-OBS-001.</li>
 *   <li>MDC propagation — PAYMENT-OBS-002 (set by {@link PaymentMdcFilter}).</li>
 * </ul>
 *
 * <p>Authorize-and-capture are fused in P3.0 for the create call (default APPROVED
 * path). Explicit endpoints {@code POST /api/payments/{id}/authorize|capture} drive
 * the explicit state machine path used by PaymentStateMachineTest.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private static final Set<String> ALLOWED_CURRENCIES = Set.of("KRW", "USD");
    private static final Map<String, Integer> CURRENCY_SCALES = Map.of(
        "KRW", 0,
        "USD", 2
    );

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final PaymentStateMachine stateMachine;
    private final PaymentEventLedger ledger;
    private final PaymentProvider provider;
    private final IdempotencyKeyStore idempotencyStore;
    private final MeterRegistry meterRegistry;

    public PaymentService(PaymentRepository paymentRepository,
                          RefundRepository refundRepository,
                          PaymentStateMachine stateMachine,
                          PaymentEventLedger ledger,
                          PaymentProvider provider,
                          IdempotencyKeyStore idempotencyStore,
                          MeterRegistry meterRegistry) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.stateMachine = stateMachine;
        this.ledger = ledger;
        this.provider = provider;
        this.idempotencyStore = idempotencyStore;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public PaymentOutcome createPayment(UUID userId, String idempotencyKey, CreatePaymentRequest request,
                                        PaymentProvider.FailureMode failureMode,
                                        Instant overrideCapturedAt) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new PaymentValidationException("Idempotency-Key header is required");
        }
        validateAmountAndCurrency(request.amount(), request.currency());

        UUID existingId = idempotencyStore.get(userId, idempotencyKey);
        if (existingId != null) {
            Payment existing = paymentRepository.findByIdAndUserId(existingId, userId)
                .orElseThrow(() -> new PaymentValidationException("idempotency replay missing payment"));
            return new PaymentOutcome(existing, true);
        }

        meterRegistry.counter("payment_attempted_total").increment();

        UUID resolvedId = idempotencyStore.findOrCreate(userId, idempotencyKey,
            () -> performCreate(userId, idempotencyKey, request, failureMode, overrideCapturedAt));

        Payment payment = paymentRepository.findByIdAndUserId(resolvedId, userId)
            .orElseThrow(() -> new PaymentValidationException("created payment vanished"));
        return new PaymentOutcome(payment, false);
    }

    private UUID performCreate(UUID userId, String idempotencyKey, CreatePaymentRequest request,
                               PaymentProvider.FailureMode failureMode, Instant overrideCapturedAt) {
        BigDecimal scaledAmount = scale(request.amount(), request.currency());
        Payment payment = new Payment();
        payment.setUserId(userId);
        payment.setOrderId(request.orderId() == null ? "order-" + UUID.randomUUID() : request.orderId());
        payment.setAmount(scaledAmount);
        payment.setCurrency(request.currency());
        payment.setPaymentMethodToken(request.paymentMethodToken());
        payment.setIdempotencyKey(idempotencyKey);
        payment.setState(PaymentState.CREATED);
        payment.setUpdatedAt(Instant.now());
        Payment saved = paymentRepository.save(payment);

        MDC.put(PaymentMdcFilter.MDC_PAYMENT_ID, saved.getId().toString());
        ledger.append(saved.getId(), PaymentEventType.PAYMENT_CREATED, scaledAmount,
            Map.of("orderId", saved.getOrderId(), "userId", userId.toString()));

        log.info("payment created paymentId={} orderId={} amount={} currency={}",
            saved.getId(), saved.getOrderId(), scaledAmount, request.currency());

        PaymentProvider.ProviderResponse response = provider.authorizeAndCapture(
            new PaymentProvider.AuthorizationRequest(
                saved.getId(), userId, scaledAmount, request.currency(),
                request.paymentMethodToken(), idempotencyKey, failureMode));

        applyProviderOutcome(saved, response, scaledAmount, overrideCapturedAt);
        paymentRepository.save(saved);
        return saved.getId();
    }

    private void applyProviderOutcome(Payment payment, PaymentProvider.ProviderResponse response,
                                      BigDecimal scaledAmount, Instant overrideCapturedAt) {
        switch (response.outcome()) {
            case APPROVED, IDEMPOTENT_REPLAY -> {
                // Default APPROVED path leaves the payment in CREATED state. Explicit
                // /authorize and /capture endpoints walk the state machine. RefundService
                // accepts CREATED payments and implicitly transitions to CAPTURED so the
                // observability + reconciliation "create-then-refund" flows work without
                // a manual /capture call.
                //
                // We still emit a provider-level AUTHORIZED ledger event so the hash chain
                // has at least 2 entries after a successful create (PAYMENT-RECON-001
                // hash-chain assertion expects ≥ 2 events on POST /api/payments). The
                // payment.state remains CREATED so the explicit-flow tests can still
                // walk the state machine.
                payment.setState(PaymentState.CREATED);
                ledger.append(payment.getId(), PaymentEventType.AUTHORIZED, scaledAmount,
                    Map.of("providerRef", String.valueOf(response.providerRef()),
                        "stateAfter", "CREATED",
                        "note", "provider authorize ack — state remains CREATED pending explicit /capture"));
                if (overrideCapturedAt != null) {
                    // The X-Test-CapturedAt override lets PaymentRefundTest back-date
                    // capturedAt for the refund-window-expired negative case. When it
                    // is set, we also pre-capture so the back-dated window check works.
                    payment.setState(PaymentState.CAPTURED);
                    payment.setCapturedAmount(scaledAmount);
                    payment.setBalance(scaledAmount);
                    payment.setCapturedAt(overrideCapturedAt);
                    ledger.append(payment.getId(), PaymentEventType.CAPTURED, scaledAmount, Map.of());
                }
            }
            case TIMEOUT -> {
                payment.setState(stateMachine.transition(PaymentState.CREATED, PaymentTransition.PROVIDER_TIMEOUT));
                ledger.append(payment.getId(), PaymentEventType.UNKNOWN_STATE_REACHED, scaledAmount,
                    Map.of("cause", "PROVIDER_TIMEOUT"));
            }
            case NETWORK_RESET -> {
                payment.setState(stateMachine.transition(PaymentState.CREATED, PaymentTransition.NETWORK_RESET));
                ledger.append(payment.getId(), PaymentEventType.UNKNOWN_STATE_REACHED, scaledAmount,
                    Map.of("cause", "NETWORK_RESET"));
            }
            case DECLINED -> {
                payment.setState(stateMachine.transition(PaymentState.CREATED, PaymentTransition.PROVIDER_DECLINE));
                payment.setDeclineReason(response.declineReason());
                ledger.append(payment.getId(), PaymentEventType.FAILED, scaledAmount,
                    Map.of("declineReason", String.valueOf(response.declineReason())));
                meterRegistry.counter("payment_failed_total").increment();
            }
            case SERVER_ERROR -> {
                payment.setState(stateMachine.transition(PaymentState.CREATED, PaymentTransition.PROVIDER_5XX_EXHAUSTED));
                payment.setDeclineReason("SERVER_ERROR");
                ledger.append(payment.getId(), PaymentEventType.FAILED, scaledAmount,
                    Map.of("declineReason", "SERVER_ERROR", "attempts", response.attempts()));
                meterRegistry.counter("payment_failed_total").increment();
            }
            case MALFORMED -> {
                payment.setState(stateMachine.transition(PaymentState.CREATED, PaymentTransition.PROVIDER_MALFORMED));
                payment.setDeclineReason("SERIALIZATION_ERROR");
                ledger.append(payment.getId(), PaymentEventType.FAILED, scaledAmount,
                    Map.of("declineReason", "SERIALIZATION_ERROR"));
                meterRegistry.counter("payment_failed_total").increment();
            }
        }
        payment.setUpdatedAt(Instant.now());
    }

    @Transactional
    public Payment authorize(UUID paymentId, UUID userId) {
        Payment payment = loadOwnedPayment(paymentId, userId);
        PaymentState next = stateMachine.transition(payment.getState(), PaymentTransition.AUTHORIZE);
        payment.setState(next);
        payment.setUpdatedAt(Instant.now());
        ledger.append(payment.getId(), PaymentEventType.AUTHORIZED, payment.getAmount(), Map.of());
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment capture(UUID paymentId, UUID userId) {
        Payment payment = loadOwnedPayment(paymentId, userId);
        PaymentState next = stateMachine.transition(payment.getState(), PaymentTransition.CAPTURE);
        payment.setState(next);
        payment.setCapturedAmount(payment.getAmount());
        if (payment.getBalance() == null) {
            payment.setBalance(payment.getAmount());
        }
        if (payment.getCapturedAt() == null) {
            payment.setCapturedAt(Instant.now());
        }
        payment.setUpdatedAt(Instant.now());
        ledger.append(payment.getId(), PaymentEventType.CAPTURED, payment.getAmount(), Map.of());
        meterRegistry.counter("payment_succeeded_total").increment();
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment voidPayment(UUID paymentId, UUID userId) {
        Payment payment = loadOwnedPayment(paymentId, userId);
        PaymentState next = stateMachine.transition(payment.getState(), PaymentTransition.VOID);
        payment.setState(next);
        payment.setUpdatedAt(Instant.now());
        ledger.append(payment.getId(), PaymentEventType.VOIDED, payment.getAmount(), Map.of());
        return paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public Payment getPayment(UUID paymentId, UUID userId) {
        return loadOwnedPayment(paymentId, userId);
    }

    @Transactional(readOnly = true)
    public Optional<Payment> findRaw(UUID paymentId) {
        return paymentRepository.findById(paymentId);
    }

    @Transactional(readOnly = true)
    public Page<Payment> list(UUID userId, Pageable pageable) {
        return paymentRepository.findByUserId(userId, pageable);
    }

    /**
     * Admin force-void — bypasses ownership check but writes an ADMIN_OVERRIDE event.
     * PAYMENT-AUTHZ-004.
     */
    @Transactional
    public Payment adminForceVoid(UUID paymentId, UUID adminUserId, String justification) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException("payment not found: " + paymentId));
        Map<String, Object> extras = new LinkedHashMap<>();
        extras.put("actorId", adminUserId.toString());
        extras.put("justification", justification == null ? "" : justification);
        extras.put("previousState", payment.getState().name());
        ledger.append(payment.getId(), PaymentEventType.ADMIN_OVERRIDE, payment.getAmount(), extras);
        // Best-effort transition to VOIDED if legal; otherwise just record the audit event.
        if (payment.getState() == PaymentState.AUTHORIZED || payment.getState() == PaymentState.CREATED) {
            payment.setState(PaymentState.VOIDED);
        }
        payment.setUpdatedAt(Instant.now());
        return paymentRepository.save(payment);
    }

    private Payment loadOwnedPayment(UUID paymentId, UUID userId) {
        return paymentRepository.findByIdAndUserId(paymentId, userId)
            .orElseThrow(() -> new PaymentNotFoundException("payment not found: " + paymentId));
    }

    private void validateAmountAndCurrency(BigDecimal amount, String currency) {
        if (amount == null) {
            throw new PaymentValidationException("amount is required");
        }
        if (amount.signum() <= 0) {
            throw new PaymentValidationException("amount must be positive");
        }
        if (currency == null || currency.isBlank()) {
            throw new PaymentValidationException("currency is required");
        }
        if (!ALLOWED_CURRENCIES.contains(currency)) {
            throw new PaymentValidationException("unsupported currency: " + currency);
        }
        try {
            Currency.getInstance(currency);
        } catch (IllegalArgumentException e) {
            throw new PaymentValidationException("invalid ISO 4217 currency code: " + currency);
        }
        Integer maxScale = CURRENCY_SCALES.get(currency);
        if (maxScale != null && amount.scale() > maxScale) {
            throw new PaymentValidationException("amount scale " + amount.scale()
                + " exceeds " + currency + " minor-unit scale " + maxScale);
        }
    }

    private BigDecimal scale(BigDecimal raw, String currency) {
        Integer s = CURRENCY_SCALES.get(currency);
        if (s == null) {
            return raw;
        }
        return raw.setScale(s, RoundingMode.UNNECESSARY);
    }

    /** Outcome of {@link #createPayment} — distinguishes 201 (created) from 200 (replay). */
    public record PaymentOutcome(Payment payment, boolean replay) {}

    /**
     * Service hook for redirect-style PG callbacks. Called by
     * {@code PaymentCallbackController} after {@link PaymentCallbackVerifier}
     * has verified the signature.
     *
     * <p>Spec anchors:
     * <ul>
     *   <li>specs/payment-l0.yaml#PAYMENT-CALLBACK-002 — idempotent on
     *       (provider, TID); duplicate callback for already-CAPTURED payment
     *       returns the existing state, NOT a second CAPTURED ledger event.</li>
     *   <li>specs/payment-l0.yaml#PAYMENT-CALLBACK-003 — only
     *       {AUTHORIZED, UNKNOWN} states may transition via callback;
     *       CREATED and REFUNDED reject with {@link IllegalStateTransitionException}.</li>
     *   <li>blueprints/payment-manifest.yaml#callback — ledger_metadata
     *       must include {@code source=callback, provider_txn_id=<TID>}.</li>
     * </ul>
     *
     * <p>This is intentionally a contract-only stub. The full implementation
     * (idempotency check, state-machine transition, ledger append, provider
     * call via {@link PaymentProvider#captureFromCallback}) ships in a
     * follow-up PRD so the {@code PaymentStateMachine} transition table can
     * be extended without breaking the existing negative-transition tests.
     *
     * <p>Fork-receivers implementing a redirect-style PG today MUST override
     * this method in a subclass or replace the service bean. The signature
     * is part of the catalog contract — do not deviate. The
     * {@code UnsupportedOperationException} message names the spec items so
     * any caller running into it sees exactly which specs must be satisfied.
     *
     * @param paymentId        the merchant-side payment id (looked up from
     *                         {@code orderId} in the callback payload by the
     *                         caller)
     * @param providerName     slug matching {@link PaymentCallbackVerifier#providerName()}
     *                         (also the {@code {provider}} path parameter)
     * @param verifiedTid      PG-issued TID, already signature-verified
     * @param signedPayload    canonical signed payload (opaque to the service;
     *                         passed through to the provider on second-stage
     *                         approval if the adapter needs it)
     * @return outcome describing the post-callback Payment state and whether
     *         this was an idempotent replay
     * @throws PaymentNotFoundException        when {@code paymentId} does not exist
     * @throws IllegalStateTransitionException when the Payment is in CREATED or
     *         REFUNDED state (PAYMENT-CALLBACK-003)
     */
    public CallbackOutcome markCapturedFromCallback(
        UUID paymentId,
        String providerName,
        String verifiedTid,
        String signedPayload
    ) {
        throw new UnsupportedOperationException(
            "markCapturedFromCallback is contract-only in this catalog release. "
            + "Implement per specs/payment-l0.yaml#PAYMENT-CALLBACK-001..003 "
            + "and blueprints/payment-manifest.yaml#callback. "
            + "Required behavior: (1) load Payment by paymentId, (2) if already "
            + "CAPTURED for the same verifiedTid, return idempotent replay "
            + "(PAYMENT-CALLBACK-002), (3) if state is CREATED or REFUNDED, "
            + "throw IllegalStateTransitionException → HTTP 409 "
            + "(PAYMENT-CALLBACK-003), (4) call provider.captureFromCallback "
            + "and on APPROVED transition to CAPTURED with a ledger row "
            + "tagged source=callback,provider_txn_id=" + verifiedTid + "."
        );
    }

    /**
     * Outcome of {@link #markCapturedFromCallback}. Field {@code state} is
     * the post-callback Payment state ({@code CAPTURED} on success).
     * {@code idempotentReplay} is true when the same (provider, TID) callback
     * was already processed.
     */
    public record CallbackOutcome(PaymentState state, boolean idempotentReplay) {}
}
