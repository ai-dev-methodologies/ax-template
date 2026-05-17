package com.ax.template.authblueprint.payment;

/**
 * Thrown when a {@link PaymentStateMachine} transition is illegal per the legal
 * transitions table. Translated to HTTP 409 by {@link PaymentExceptionHandler}.
 */
public class IllegalStateTransitionException extends RuntimeException {

    private final PaymentState currentState;
    private final PaymentTransition attemptedEvent;

    public IllegalStateTransitionException(PaymentState currentState, PaymentTransition attemptedEvent) {
        super("Cannot transition from " + currentState + " via " + attemptedEvent);
        this.currentState = currentState;
        this.attemptedEvent = attemptedEvent;
    }

    public PaymentState getCurrentState() { return currentState; }
    public PaymentTransition getAttemptedEvent() { return attemptedEvent; }
}
