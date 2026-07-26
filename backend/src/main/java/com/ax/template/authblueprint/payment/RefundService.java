package com.ax.template.authblueprint.payment;

import com.ax.template.authblueprint.common.Money;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Refund lifecycle. Enforces window + sum invariant + refund-of-refund denial.
 *
 * <p>PAYMENT-REFUND-001: refund window (default 720h) enforced from capturedAt.
 * PAYMENT-REFUND-002: sum(refunds) ≤ capturedAmount; atomic via optimistic lock on Payment.
 * PAYMENT-REFUND-003: refund-of-refund denied — state machine throws on REFUNDED → REFUND.
 * PAYMENT-IDEMP-004: a retry carrying an already-used (payment, Idempotency-Key) REPLAYS the
 * original refund row instead of creating a second one.
 * PAYMENT-REFUND-004: the requested amount must be positive and exactly representable in the
 * currency's minor units — checked BEFORE any mutation (see {@code admissibleAmount}).
 *
 * <h2>Refund idempotency (PAYMENT-IDEMP-004, P1-70)</h2>
 * The replay lookup runs immediately after the ownership load and BEFORE the state/window/sum
 * guards. That placement is load-bearing: a retried FULL refund leaves the payment in REFUNDED, so
 * checking the guards first would answer a legitimate retry with 409 {@code refund-of-refund}
 * instead of replaying the original 201 body.
 *
 * <p>The replay executes ZERO side effects — no state transition, no ledger append, no counter
 * increment — and does NOT compare the retried request body against the original (deliberate parity
 * with {@code PaymentService#createPayment}, whose Idempotency-Key replay is likewise key-only; the
 * body-fingerprint conflict check is the {@code idempotency} domain's separate concern).
 *
 * <p>Concurrency: two concurrent same-key refunds can both miss the lookup. Both then mutate the
 * parent {@link Payment}, which carries an optimistic-lock {@code @Version}, so the loser normally
 * fails with an OptimisticLockException → 409 {@code urn:ax:payment:concurrent-modification}; the
 * client's retry then hits the replay path. When the loser instead reaches its INSERT (interleavings
 * where the version check is not the first to fail), the
 * {@code ux_refunds_payment_id_idempotency_key} unique constraint (V116) refuses the duplicate — and
 * that failure is classified here into the SAME retryable 409 rather than escaping as an unmapped
 * server fault ({@code isRefundIdempotencyConflict}). Either way the loser gets a retryable conflict,
 * exactly one refund row exists, and {@code refund_processed_total} counts only the winner (the
 * increment is deferred to afterCommit).
 */
@Service
public class RefundService {

    /** Lower-cased name of the (payment_id, idempotency_key) unique constraint — V116 / Refund.java. */
    private static final String REFUND_IDEMPOTENCY_CONSTRAINT = "ux_refunds_payment_id_idempotency_key";

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final PaymentStateMachine stateMachine;
    private final PaymentEventLedger ledger;
    private final MeterRegistry meterRegistry;
    private final long refundWindowHours;

    public RefundService(PaymentRepository paymentRepository,
                         RefundRepository refundRepository,
                         PaymentStateMachine stateMachine,
                         PaymentEventLedger ledger,
                         MeterRegistry meterRegistry,
                         @Value("${ax.payment.refund.window-hours:720}") long refundWindowHours) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.stateMachine = stateMachine;
        this.ledger = ledger;
        this.meterRegistry = meterRegistry;
        this.refundWindowHours = refundWindowHours;
    }

    @Transactional
    public RefundOutcome refund(UUID paymentId, UUID userId, RefundRequest request, String idempotencyKey) {
        Payment payment = paymentRepository.findByIdAndUserId(paymentId, userId)
            .orElseThrow(() -> new PaymentNotFoundException("payment not found: " + paymentId));

        // 0. Idempotent replay (PAYMENT-IDEMP-004). MUST precede the state/window/sum guards: a
        // retried FULL refund already left the payment REFUNDED, so guard-first would 409 a
        // legitimate retry. Ownership was enforced by findByIdAndUserId above (IDOR → 404), so the
        // (paymentId, key) pair is sufficient. Zero side effects on this path.
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Refund> replay = refundRepository
                .findByPaymentIdAndIdempotencyKey(paymentId, idempotencyKey);
            if (replay.isPresent()) {
                return new RefundOutcome(replay.get(), true);
            }
        }

        // 0.5 Amount admissibility (PAYMENT-REFUND-004). MUST precede EVERY mutation below — including
        // the implicit authorize+capture of a CREATED payment — so an inadmissible amount can never
        // move state, money or the ledger. The sum invariant in step 3 only bounds the amount from
        // ABOVE, so without this a negative amount passed it and CREATED money (balance went UP), and
        // a sub-minor-unit amount committed a refund that RefundResponse.from could not even encode.
        BigDecimal requestedOrNull = request.amount() == null
            ? null
            : admissibleAmount(request.amount().resolveMajor(payment.getCurrency()), payment.getCurrency());

        // 1. State-machine check: REFUNDED cannot be re-refunded (PAYMENT-REFUND-003).
        if (payment.getState() == PaymentState.REFUNDED) {
            throw new RefundException(409, "urn:ax:payment:refund-of-refund",
                "payment is already fully refunded; refund-of-refund is forbidden");
        }
        // Implicit authorize + capture when refunding a freshly CREATED payment so the
        // observability + reconciliation "create-then-refund" path succeeds without an
        // explicit /capture call. Both ledger events are still appended for audit completeness.
        if (payment.getState() == PaymentState.CREATED) {
            BigDecimal amt = payment.getAmount();
            stateMachine.apply(payment, PaymentTransition.AUTHORIZE);
            ledger.append(payment.getId(), PaymentEventType.AUTHORIZED, amt, Map.of("implicit", true));
            stateMachine.apply(payment, PaymentTransition.CAPTURE);
            payment.setCapturedAmount(amt);
            payment.setBalance(amt);
            if (payment.getCapturedAt() == null) {
                payment.setCapturedAt(Instant.now());
            }
            ledger.append(payment.getId(), PaymentEventType.CAPTURED, amt, Map.of("implicit", true));
        }
        if (payment.getState() != PaymentState.CAPTURED && payment.getState() != PaymentState.PARTIAL_REFUNDED) {
            throw new IllegalStateTransitionException(payment.getState(), PaymentTransition.REFUND);
        }

        // 2. Window check (PAYMENT-REFUND-001).
        Instant capturedAt = payment.getCapturedAt() == null ? payment.getCreatedAt() : payment.getCapturedAt();
        if (capturedAt != null
            && capturedAt.plus(Duration.ofHours(refundWindowHours)).isBefore(Instant.now())) {
            throw new RefundException(409, "urn:ax:payment:refund-window-expired",
                "refund window of " + refundWindowHours + " hours has expired");
        }

        // 3. Sum invariant (PAYMENT-REFUND-002).
        BigDecimal capturedAmount = payment.getCapturedAmount() == null ? payment.getAmount() : payment.getCapturedAmount();
        BigDecimal existingSum = refundRepository.sumByPaymentId(paymentId);
        if (existingSum == null) existingSum = BigDecimal.ZERO;
        // The wire amount is a SHAPE (integer MINOR units or decimal-string MAJOR units); it was
        // resolved against the payment's own currency (P1-69) and admitted in step 0.5. A null amount
        // means "refund everything that is LEFT" — the REMAINING balance, not the original captured
        // amount (PAYMENT-REFUND-005).
        BigDecimal requested = requestedOrNull == null
            ? remainingRefundable(capturedAmount, existingSum, payment.getCurrency())
            : requestedOrNull;
        BigDecimal newSum = existingSum.add(requested);
        if (newSum.compareTo(capturedAmount) > 0) {
            throw new PaymentValidationException("sum of refunds " + newSum
                + " exceeds captured amount " + capturedAmount);
        }

        // 4. State transition: full vs partial.
        boolean full = newSum.compareTo(capturedAmount) == 0;
        PaymentTransition transition = full ? PaymentTransition.REFUND : PaymentTransition.PARTIAL_REFUND;
        stateMachine.apply(payment, transition);
        BigDecimal newBalance = capturedAmount.subtract(newSum);
        payment.setBalance(newBalance);
        payment.setUpdatedAt(Instant.now());
        paymentRepository.save(payment);

        // 5. Persist Refund row + ledger event.
        Refund refund = new Refund();
        refund.setPaymentId(paymentId);
        refund.setAmount(requested);
        refund.setCurrency(payment.getCurrency());
        refund.setReason(request.reason());
        refund.setIdempotencyKey(idempotencyKey);
        refund.setState(RefundState.COMPLETED);
        // saveAndFlush (not save): forces the INSERT here so the ux_refunds_payment_id_idempotency_key
        // backstop raises INSIDE this method and can be classified. With a deferred commit-time flush
        // the violation surfaces after the service returns, where nothing maps it — the caller then
        // gets whatever the unmapped-exception path yields (an empty-bodied 403 from the Spring
        // Security /error dispatch), never an RFC 7807 conflict.
        Refund saved;
        try {
            saved = refundRepository.saveAndFlush(refund);
        } catch (DataIntegrityViolationException e) {
            if (!isRefundIdempotencyConflict(e)) {
                throw e;   // unrelated integrity error — never swallowed by the replay mapping
            }
            // Concurrent same-key refund: both requests missed the replay lookup, this one lost the
            // unique-index race. It is a retryable CONFLICT, not a server fault — the client's retry
            // finds the winner's row and replays it (200).
            throw new RefundException(409, "urn:ax:payment:concurrent-modification",
                "a concurrent request already created a refund for this Idempotency-Key; retry to replay it");
        }

        // PAYMENT-RECON-002 invariant: the reconciliation query sums ledger events of
        // type=REFUNDED. Both full and partial refunds emit a REFUNDED ledger event so
        // a single SUM is sufficient. The full-vs-partial distinction is tracked on the
        // Refund row + the payment.state transition; the ledger type is intentionally unified.
        ledger.append(paymentId, PaymentEventType.REFUNDED, requested,
            Map.of("refundId", saved.getId().toString(),
                "balance", newBalance.toPlainString(),
                "kind", full ? "FULL" : "PARTIAL"));

        // PAYMENT-OBS-001: count only refunds that actually COMMIT. A Micrometer counter is NOT
        // transactional, so an inline increment also counts refunds whose transaction later rolls
        // back (the unique-index loser above, an optimistic-lock loser at commit) — the counter would
        // then over-report a double-refund that never happened. afterCommit is the exactly-once hook;
        // the replay path returns earlier and never reaches here. (No REQUIRES_NEW: the increment is
        // an out-of-band side effect, not a second write to be isolated.)
        incrementRefundCounterAfterCommit();
        return new RefundOutcome(saved, false);
    }

    /**
     * PAYMENT-REFUND-004 — admit a requested refund amount, or reject it as a 400.
     *
     * <p>Mirrors {@code PaymentService.validateAmountAndCurrency}'s semantics for the create path
     * (positive + exactly representable in the currency's ISO-4217 minor units), which the refund
     * path never had: it accepted whatever the wire shape resolved to and let the sum invariant —
     * an UPPER bound only — be the sole check.
     *
     * <ul>
     *   <li>{@code signum() <= 0} → 400. A negative amount is not a refund; it would ADD to the
     *       payment balance (money creation). Zero is a no-op row plus a state/ledger mutation.</li>
     *   <li>not representable in minor units (e.g. USD {@code "0.001"}) → 400. The check IS the
     *       {@code Money.toMinorUnits} conversion the wire encoder performs later, run here where
     *       its {@link ArithmeticException} is still an inadmissible-INPUT signal rather than a
     *       post-commit failure.</li>
     * </ul>
     *
     * @return the amount normalised through the minor-unit round-trip, so the persisted value is
     *         exactly representable by construction (e.g. USD {@code "2.5"} → {@code 2.50}).
     */
    private static BigDecimal admissibleAmount(BigDecimal requested, String currency) {
        if (requested.signum() <= 0) {
            throw new PaymentValidationException("refund amount must be positive");
        }
        long minor;
        try {
            minor = Money.toMinorUnits(requested, currency);
        } catch (ArithmeticException e) {
            throw new PaymentValidationException(
                "refund amount is not representable in " + currency + " minor units");
        }
        return Money.toMajorUnits(minor, currency);
    }

    /**
     * PAYMENT-REFUND-005 — resolve the amount of an omitted-{@code amount} ("full") refund.
     *
     * <p>An omitted amount means "refund what is LEFT", i.e. {@code capturedAmount - Σ(prior
     * refunds)}. Substituting the ORIGINAL {@code capturedAmount} instead is wrong the moment a
     * partial refund exists: on a ₩10,000 payment already refunded ₩3,000 it proposes a new sum of
     * ₩13,000, the step-3 invariant rejects it with 400, and the remaining ₩7,000 becomes
     * unrefundable through the omitted-amount path — which is the path the operator UI uses
     * (transactions-screen.tsx posts {@code {reason}} with no amount).
     *
     * <ul>
     *   <li>remaining {@code <= 0} → 409 {@code urn:ax:payment:refund-of-refund}, the SAME type the
     *       explicit REFUNDED guard raises. Nothing is left to refund, so this is a state conflict
     *       (the request carries no amount, hence nothing to call invalid) — 409 matches the
     *       {@link RefundException} style already used for both state conflicts in this method,
     *       whereas the 400 {@link PaymentValidationException} branch is reserved for inadmissible
     *       INPUT. Reachable only if a payment is left CAPTURED/PARTIAL_REFUNDED while its refunds
     *       already sum to the captured amount (state drift); the invariant is asserted, not
     *       assumed.</li>
     *   <li>otherwise the remainder is normalised through {@link #admissibleAmount} — the same
     *       minor-unit round-trip the explicit-amount path takes — so the persisted value carries the
     *       currency's scale by construction (e.g. USD {@code 7.5} → {@code 7.50}). Both operands are
     *       already representable, so the check cannot fire here; it exists so the two paths cannot
     *       drift apart.</li>
     * </ul>
     */
    private static BigDecimal remainingRefundable(BigDecimal capturedAmount, BigDecimal existingSum,
                                                  String currency) {
        BigDecimal remaining = capturedAmount.subtract(existingSum);
        if (remaining.signum() <= 0) {
            throw new RefundException(409, "urn:ax:payment:refund-of-refund",
                "payment is already fully refunded; refund-of-refund is forbidden");
        }
        return admissibleAmount(remaining, currency);
    }

    private void incrementRefundCounterAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // No surrounding transaction (direct, unproxied call): nothing to wait for.
            meterRegistry.counter("refund_processed_total").increment();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                meterRegistry.counter("refund_processed_total").increment();
            }
        });
    }

    /**
     * True when {@code e} is the {@code ux_refunds_payment_id_idempotency_key} unique-constraint
     * violation — the DB backstop for two concurrent same-key refunds that both missed the replay
     * lookup (PAYMENT-IDEMP-004). Matched on the CONSTRAINT NAME anywhere in the cause chain so an
     * unrelated integrity error (not-null, FK, another unique index) is never mis-mapped to the
     * replay conflict. Vendor message shapes differ (PostgreSQL quotes the constraint name; H2
     * appends an {@code _INDEX_n} suffix), hence a case-insensitive containment check.
     */
    static boolean isRefundIdempotencyConflict(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause() == t ? null : t.getCause()) {
            String message = t.getMessage();
            if (message != null
                && message.toLowerCase(Locale.ROOT).contains(REFUND_IDEMPOTENCY_CONSTRAINT)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Outcome of {@link #refund} — distinguishes a freshly created refund (201) from an
     * Idempotency-Key replay of an existing one (200). Mirrors
     * {@code PaymentService.PaymentOutcome}.
     */
    public record RefundOutcome(Refund refund, boolean replay) {}
}
