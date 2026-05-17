package com.ax.template.authblueprint.payment;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.net.URI;

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
        pd.setType(URI.create("urn:ax:payment:validation-error"));
        pd.setTitle("Validation error");
        pd.setDetail(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @ExceptionHandler(IllegalStateTransitionException.class)
    public ResponseEntity<ProblemDetail> handleIllegalTransition(IllegalStateTransitionException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setType(URI.create("urn:ax:payment:illegal-state-transition"));
        pd.setTitle("Illegal state transition");
        pd.setDetail(ex.getMessage());
        pd.setProperty("currentState", ex.getCurrentState().name());
        pd.setProperty("attemptedEvent", ex.getAttemptedEvent().name());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }

    @ExceptionHandler(RefundException.class)
    public ResponseEntity<ProblemDetail> handleRefund(RefundException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(ex.getStatus());
        pd.setType(URI.create(ex.getTypeUri()));
        pd.setTitle("Refund rejected");
        pd.setDetail(ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(pd);
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(PaymentNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setType(URI.create("urn:ax:payment:not-found"));
        pd.setTitle("Payment not found");
        // PCI-safe: never include the requesting user id or payment-id in the response detail —
        // use a generic message to avoid IDOR enumeration cues.
        pd.setDetail("payment not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setType(URI.create("urn:ax:payment:concurrent-modification"));
        pd.setTitle("Concurrent modification");
        pd.setDetail("payment was modified concurrently; retry the request");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleNotReadable(HttpMessageNotReadableException ex) {
        // P5 security-review (US-014 MEDIUM): sanitize Jackson exception detail to
        // prevent leaking internal class names + field paths (e.g.,
        // "through reference chain: com.ax.template.authblueprint.payment.CreatePaymentRequest[\"amount\"]").
        // The float-rejection message from MoneyDeserializer is intentional and preserved
        // because PaymentMoneyTest asserts it. Generic Jackson errors fall back to a
        // safe message that does not surface package paths.
        String causeMessage = ex.getMostSpecificCause() != null
            ? ex.getMostSpecificCause().getMessage()
            : ex.getMessage();
        String safeDetail = sanitizeJacksonMessage(causeMessage);
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setType(URI.create("urn:ax:payment:validation-error"));
        pd.setTitle("Validation error");
        pd.setDetail(safeDetail);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    /**
     * Strip Jackson's "(through reference chain: ...)" suffix and any class-FQN
     * fragments to avoid leaking internal package names in 400 responses.
     * Preserves the leading human-readable message (including MoneyDeserializer
     * float-rejection text which PAYMENT-MONEY-002 asserts).
     */
    private static String sanitizeJacksonMessage(String message) {
        if (message == null || message.isBlank()) {
            return "request body is unreadable";
        }
        // Drop everything from "(through reference chain:" onward.
        int chainIdx = message.indexOf("(through reference chain");
        String head = (chainIdx >= 0) ? message.substring(0, chainIdx).trim() : message;
        // Drop "at [Source: ...]" Jackson location markers.
        int sourceIdx = head.indexOf(" at [Source:");
        if (sourceIdx >= 0) {
            head = head.substring(0, sourceIdx).trim();
        }
        return head.isEmpty() ? "request body is unreadable" : head;
    }
}
