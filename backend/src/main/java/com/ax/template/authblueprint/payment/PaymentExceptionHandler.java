package com.ax.template.authblueprint.payment;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * RFC 7807 ProblemDetail responses for payment exceptions. Stable {@code type} URIs
 * enable programmatic handling per the OpenAPI contract.
 */
@ControllerAdvice(basePackages = "com.ax.template.authblueprint.payment")
@Order(0)
public class PaymentExceptionHandler {

    /** Max length of any client-derived detail echoed into a 400 body (response-amplification cap). */
    private static final int MAX_DETAIL_LEN = 200;

    /** Max number of field errors joined into a bean-validation 400 body (bounds many-field amplification). */
    private static final int MAX_FIELD_ERRORS = 10;

    @ExceptionHandler(PaymentValidationException.class)
    public ResponseEntity<ProblemDetail> handleValidation(PaymentValidationException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setType(URI.create("urn:ax:payment:validation-error"));
        pd.setTitle("Validation error");
        // Amplification guard: a service-thrown validation message can embed a client value
        // (e.g. "unsupported currency: <value>"); cap it so it can never inflate the 400 body.
        pd.setDetail(truncate(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    /**
     * Bean-validation (@Valid) failures on the request body. TOTAL amplification defense: EVERY
     * reflected field error is bounded — the raw rejected value is never echoed, and each field's
     * message is truncated to {@link #MAX_DETAIL_LEN} — so no single field (regardless of which one)
     * can inflate the 400 body. A tight @Size(max=3) on {@code currency} fast-rejects an oversized
     * (~1MB) value here before it ever reaches the service layer.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleBeanValidation(MethodArgumentNotValidException ex) {
        List<String> fieldMessages = new ArrayList<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            String msg = (fe.getDefaultMessage() != null && !fe.getDefaultMessage().isBlank())
                ? fe.getDefaultMessage()
                : "invalid value";
            // Deliberately DO NOT echo fe.getRejectedValue() — that is the untrusted client input.
            fieldMessages.add(truncate(fe.getField() + ": " + msg));
            if (fieldMessages.size() >= MAX_FIELD_ERRORS) {
                break;
            }
        }
        String detail = fieldMessages.isEmpty()
            ? "request validation failed"
            : truncate(String.join("; ", fieldMessages));
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setType(URI.create("urn:ax:payment:validation-error"));
        pd.setTitle("Validation error");
        pd.setDetail(detail);
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

    @ExceptionHandler(TendersUnderfundedException.class)
    public ResponseEntity<ProblemDetail> handleUnderfunded(TendersUnderfundedException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        pd.setType(URI.create("urn:ax:payment:tenders-underfunded"));
        pd.setTitle("Tenders underfunded");
        pd.setDetail("Active tenders do not cover the order total; shortfall=" + ex.getShortfall());
        // Amplification guard: orderId is client-derived; bound it like every other reflected value
        // (truncate() only wrapped setDetail before — this closes the setProperty leak).
        pd.setProperty("orderId", truncate(ex.getOrderId()));
        pd.setProperty("orderTotal", ex.getOrderTotal());
        pd.setProperty("covered", ex.getCovered());
        pd.setProperty("shortfall", ex.getShortfall());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(pd);
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
        if (head.isEmpty()) {
            return "request body is unreadable";
        }
        // Amplification guard: a generic Jackson error can embed the offending value; never reflect
        // unbounded client input into the 400 body. Cap to a short, human-readable prefix. (Jackson 3
        // raised the default max string length 20M→100M; a large value could otherwise echo back.)
        return truncate(head);
    }

    /**
     * Cap any client-derived string to at most {@link #MAX_DETAIL_LEN} characters INCLUDING the
     * ellipsis, so the promised 200-char bound is exact (was off-by-one: substring(0,200)+"…"=201).
     */
    private static String truncate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() <= MAX_DETAIL_LEN ? s : s.substring(0, MAX_DETAIL_LEN - 1) + "…";
    }
}
