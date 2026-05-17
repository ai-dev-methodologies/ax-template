package com.ax.template.authblueprint.payment;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * RFC 7807 ProblemDetail responses for payment exceptions. Stable {@code type} URIs
 * enable programmatic handling per the OpenAPI contract.
 */
@ControllerAdvice(basePackages = "com.ax.template.authblueprint.payment")
@Order(0)
public class PaymentExceptionHandler {

    @ExceptionHandler(PaymentValidationException.class)
    public ResponseEntity<ProblemDetail> handleValidation(PaymentValidationException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setType(java.net.URI.create("urn:ax:payment:validation-error"));
        pd.setTitle("Validation error");
        pd.setDetail(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @ExceptionHandler(IllegalStateTransitionException.class)
    public ResponseEntity<ProblemDetail> handleIllegalTransition(IllegalStateTransitionException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setType(java.net.URI.create("urn:ax:payment:illegal-state-transition"));
        pd.setTitle("Illegal state transition");
        pd.setDetail(ex.getMessage());
        pd.setProperty("currentState", ex.getCurrentState().name());
        pd.setProperty("attemptedEvent", ex.getAttemptedEvent().name());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }

    @ExceptionHandler(RefundException.class)
    public ResponseEntity<ProblemDetail> handleRefund(RefundException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(ex.getStatus());
        pd.setType(java.net.URI.create(ex.getTypeUri()));
        pd.setTitle("Refund rejected");
        pd.setDetail(ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(pd);
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(PaymentNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setType(java.net.URI.create("urn:ax:payment:not-found"));
        pd.setTitle("Payment not found");
        // PCI-safe: never include the requesting user id or payment-id in the response detail —
        // use a generic message to avoid IDOR enumeration cues.
        pd.setDetail("payment not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setType(java.net.URI.create("urn:ax:payment:concurrent-modification"));
        pd.setTitle("Concurrent modification");
        pd.setDetail("payment was modified concurrently; retry the request");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleNotReadable(HttpMessageNotReadableException ex) {
        // Distinguish float-JSON rejection from generic parse errors — PAYMENT-MONEY-002
        String message = ex.getMostSpecificCause() != null
            ? ex.getMostSpecificCause().getMessage()
            : ex.getMessage();
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setType(java.net.URI.create("urn:ax:payment:validation-error"));
        pd.setTitle("Validation error");
        pd.setDetail(message == null ? "request body is unreadable" : message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }
}
