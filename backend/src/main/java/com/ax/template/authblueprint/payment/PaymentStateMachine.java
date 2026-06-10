package com.ax.template.authblueprint.payment;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Pure function that enforces the payment state transition graph from
 * blueprints/payment-manifest.yaml#state_machine.legal_transitions.
 *
 * <p>No DB side effects. Service layer calls this before persisting; an illegal
 * transition throws {@link IllegalStateTransitionException} and aborts the txn
 * before any state mutation hits the DB.
 *
 * <p>PAYMENT-STATE-001: legal transitions table.
 * PAYMENT-STATE-003: illegal transitions → 409 + RFC 7807 with currentState/attemptedEvent
 * extension fields (handled by {@link PaymentExceptionHandler}).
 */
@Component
public class PaymentStateMachine {

    private static final Map<PaymentState, Map<PaymentTransition, PaymentState>> LEGAL =
        new EnumMap<>(PaymentState.class);

    static {
        put(PaymentState.CREATED, PaymentTransition.AUTHORIZE, PaymentState.AUTHORIZED);
        // Convenience: explicit /capture on a freshly-created payment fuses
        // authorize+capture in a single step. PCI-DSS audit trail captures both
        // AUTHORIZED + CAPTURED ledger events in PaymentService.capture.
        put(PaymentState.CREATED, PaymentTransition.CAPTURE, PaymentState.CAPTURED);
        // CREATED can fail-fast on a 4xx decline or 5xx exhaustion / malformed response
        // returned synchronously from the provider during the create-and-authorize call.
        put(PaymentState.CREATED, PaymentTransition.PROVIDER_DECLINE, PaymentState.FAILED);
        put(PaymentState.CREATED, PaymentTransition.PROVIDER_5XX_EXHAUSTED, PaymentState.FAILED);
        put(PaymentState.CREATED, PaymentTransition.PROVIDER_MALFORMED, PaymentState.FAILED);
        put(PaymentState.CREATED, PaymentTransition.PROVIDER_TIMEOUT, PaymentState.UNKNOWN);
        put(PaymentState.CREATED, PaymentTransition.NETWORK_RESET, PaymentState.UNKNOWN);

        put(PaymentState.AUTHORIZED, PaymentTransition.CAPTURE, PaymentState.CAPTURED);
        put(PaymentState.AUTHORIZED, PaymentTransition.VOID, PaymentState.VOIDED);
        put(PaymentState.AUTHORIZED, PaymentTransition.PROVIDER_TIMEOUT, PaymentState.UNKNOWN);
        put(PaymentState.AUTHORIZED, PaymentTransition.NETWORK_RESET, PaymentState.UNKNOWN);

        put(PaymentState.CAPTURED, PaymentTransition.REFUND, PaymentState.REFUNDED);
        put(PaymentState.CAPTURED, PaymentTransition.PARTIAL_REFUND, PaymentState.PARTIAL_REFUNDED);

        put(PaymentState.PARTIAL_REFUNDED, PaymentTransition.REFUND, PaymentState.REFUNDED);
        put(PaymentState.PARTIAL_REFUNDED, PaymentTransition.PARTIAL_REFUND, PaymentState.PARTIAL_REFUNDED);
    }

    private static final Set<PaymentState> TERMINAL = EnumSet.of(
        PaymentState.VOIDED,
        PaymentState.REFUNDED,
        PaymentState.UNKNOWN,
        PaymentState.FAILED
    );

    private static void put(PaymentState from, PaymentTransition via, PaymentState to) {
        LEGAL.computeIfAbsent(from, k -> new EnumMap<>(PaymentTransition.class)).put(via, to);
    }

    /**
     * Resolve the next state for ({@code from}, {@code event}) or throw
     * {@link IllegalStateTransitionException} on an illegal transition.
     */
    public PaymentState transition(PaymentState from, PaymentTransition event) {
        Map<PaymentTransition, PaymentState> row = LEGAL.get(from);
        if (row == null || !row.containsKey(event)) {
            throw new IllegalStateTransitionException(from, event);
        }
        return row.get(event);
    }

    public boolean isTerminal(PaymentState state) {
        return TERMINAL.contains(state);
    }

    /**
     * Apply a transition TO the aggregate — the sole-mutator seam (BACKLOG P0-26):
     * computes the next state from the payment's CURRENT state and assigns it via the
     * package-private {@link Payment#setState}. Services never set the state directly;
     * an illegal event throws {@link IllegalStateTransitionException} (→ 409).
     */
    public void apply(Payment payment, PaymentTransition event) {
        payment.setState(transition(payment.getState(), event));
    }

    /**
     * Admin force-void escape hatch (PAYMENT-AUTHZ-004 admin override): best-effort —
     * voids a CREATED or AUTHORIZED payment, silently leaves any other state untouched
     * (the ADMIN_OVERRIDE ledger event is still recorded by the caller). The
     * CREATED→VOIDED move is deliberately NOT an edge in {@code LEGAL} (AUTHORIZED→VOID
     * already is): the public /void endpoint must keep rejecting CREATED→VOID; only the
     * audited admin path may take it, and the state machine stays the single place that
     * encodes the exception.
     */
    public void forceVoid(Payment payment) {
        if (payment.getState() == PaymentState.AUTHORIZED || payment.getState() == PaymentState.CREATED) {
            payment.setState(PaymentState.VOIDED);
        }
    }
}
