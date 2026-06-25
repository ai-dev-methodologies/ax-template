package com.ax.template.authblueprint.payment;

import java.math.BigDecimal;

/**
 * Thrown when the sum of active authorized tenders (AUTHORIZED + CAPTURED) for
 * an orderId falls short of the requested order total.
 *
 * <p>PAYMENT-SPLIT-001: translated to HTTP 422 with RFC 7807 ProblemDetail by
 * {@link PaymentExceptionHandler}. Carries the residual shortfall so the client
 * can know exactly how much additional tender is required.
 */
public class TendersUnderfundedException extends RuntimeException {

    private final String orderId;
    private final BigDecimal orderTotal;
    private final BigDecimal covered;
    private final BigDecimal shortfall;

    public TendersUnderfundedException(String orderId, BigDecimal orderTotal, BigDecimal covered) {
        super("Tenders underfunded for orderId=" + orderId
            + ": covered=" + covered + " < orderTotal=" + orderTotal
            + ", shortfall=" + orderTotal.subtract(covered));
        this.orderId = orderId;
        this.orderTotal = orderTotal;
        this.covered = covered;
        this.shortfall = orderTotal.subtract(covered);
    }

    public String getOrderId() { return orderId; }
    public BigDecimal getOrderTotal() { return orderTotal; }
    public BigDecimal getCovered() { return covered; }
    public BigDecimal getShortfall() { return shortfall; }
}
