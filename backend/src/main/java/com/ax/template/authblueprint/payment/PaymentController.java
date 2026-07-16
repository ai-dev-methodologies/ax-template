package com.ax.template.authblueprint.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Payment REST endpoints. See contracts/payment-openapi.yaml for the wire format.
 *
 * <p>All endpoints require authentication (PAYMENT-AUTHZ-001 — SecurityConfig).
 * IDOR-safe: cross-user lookups return 404, never 403 (PAYMENT-AUTHZ-003).
 * Mutations require {@code Idempotency-Key} header (PAYMENT-IDEMP-001).
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    static final String FAILURE_MODE_HEADER = "X-Test-Provider-Mode";
    static final String CAPTURED_AT_OVERRIDE_HEADER = "X-Test-CapturedAt";

    private final PaymentService paymentService;
    private final RefundService refundService;

    public PaymentController(PaymentService paymentService, RefundService refundService) {
        this.paymentService = paymentService;
        this.refundService = refundService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
        @Valid @RequestBody CreatePaymentRequest request,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @RequestHeader(value = FAILURE_MODE_HEADER, required = false) String failureModeHeader,
        @RequestHeader(value = CAPTURED_AT_OVERRIDE_HEADER, required = false) String capturedAtOverride,
        @AuthenticationPrincipal Jwt jwt) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new PaymentValidationException("Idempotency-Key header is required");
        }
        UUID userId = UUID.fromString(jwt.getSubject());
        PaymentProvider.FailureMode failureMode = resolveFailureMode(failureModeHeader, request.mockFailureMode());
        Instant overrideCapturedAt = parseInstant(capturedAtOverride);

        PaymentService.PaymentOutcome outcome = paymentService.createPayment(
            userId, idempotencyKey, request, failureMode, overrideCapturedAt);

        Map<String, Object> body = paymentBody(outcome.payment());
        HttpStatus status = chooseStatus(outcome);
        return ResponseEntity.status(status).body(body);
    }

    @GetMapping
    public Map<String, Object> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        int safeSize = Math.min(size, 100);
        Page<Payment> result = paymentService.list(userId,
            PageRequest.of(page, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return Map.of(
            "content", result.getContent().stream().map(PaymentController::paymentBody).toList(),
            "page", result.getNumber(),
            "size", result.getSize(),
            "totalElements", result.getTotalElements(),
            "totalPages", result.getTotalPages()
        );
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        Payment payment = paymentService.getPayment(id, userId);
        return paymentBody(payment);
    }

    @PostMapping("/{id}/authorize")
    public Map<String, Object> authorize(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return paymentBody(paymentService.authorize(id, userId));
    }

    @PostMapping("/{id}/capture")
    public Map<String, Object> capture(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return paymentBody(paymentService.capture(id, userId));
    }

    @PostMapping("/{id}/void")
    public Map<String, Object> voidPayment(
        @PathVariable UUID id,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @AuthenticationPrincipal Jwt jwt) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new PaymentValidationException("Idempotency-Key header is required");
        }
        UUID userId = UUID.fromString(jwt.getSubject());
        return paymentBody(paymentService.voidPayment(id, userId));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<RefundResponse> refund(
        @PathVariable UUID id,
        @Valid @RequestBody(required = false) RefundRequest request,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @AuthenticationPrincipal Jwt jwt) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new PaymentValidationException("Idempotency-Key header is required");
        }
        UUID userId = UUID.fromString(jwt.getSubject());
        RefundRequest req = request == null ? new RefundRequest() : request;
        // RefundRequest is a record with an explicit no-arg constructor that yields
        // (amount=null, reason=null) — semantics: "refund full captured amount".
        Refund refund = refundService.refund(id, userId, req, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(RefundResponse.from(refund));
    }

    /**
     * PAYMENT-SPLIT-001: POST /api/payments/coverage/confirm
     *
     * <p>Returns 200 with a coverage result when the sum of active authorized tenders
     * (AUTHORIZED + CAPTURED) for the given orderId and currency covers the orderTotal.
     * Returns 422 PAYMENT_TENDERS_UNDERFUNDED with the residual shortfall otherwise.
     *
     * <p>Coverage is intentionally NOT user-scoped: split tenders for an order may
     * originate from multiple payment instruments / payers (e.g. gift card + card).
     * Authentication is still required via the /api/payments/** security rule.
     */
    @PostMapping("/coverage/confirm")
    public ResponseEntity<Map<String, Object>> confirmCoverage(
        @Valid @RequestBody CoverageConfirmRequest request) {

        PaymentService.CoverageResult result =
            paymentService.confirmCoverage(request.orderId(), request.currency(), request.orderTotal());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderId", request.orderId());
        body.put("currency", request.currency());
        body.put("orderTotal", result.orderTotal());
        body.put("covered", result.covered());
        return ResponseEntity.ok(body);
    }

    /**
     * Request DTO for {@code POST /api/payments/coverage/confirm}. Bounds mirror the hardened
     * sibling {@link CreatePaymentRequest}: ISO-4217 currency is exactly 3 chars, and orderId is
     * free-text with a generous cap — both fast-reject an oversized (~1MB) value at bean-validation
     * BEFORE it can reach the service and be echoed into an error (response-amplification defense).
     */
    record CoverageConfirmRequest(
        @Size(max = 200) String orderId,
        @Size(max = 3) String currency,
        java.math.BigDecimal orderTotal) {}

    private static Map<String, Object> paymentBody(Payment p) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", p.getId() == null ? null : p.getId().toString());
        body.put("paymentId", p.getId() == null ? null : p.getId().toString());
        body.put("orderId", p.getOrderId());
        body.put("amount", canonicalize(p.getAmount()));
        body.put("capturedAmount", canonicalize(p.getCapturedAmount()));
        body.put("balance", canonicalize(p.getBalance()));
        body.put("currency", p.getCurrency());
        body.put("status", p.getState().name());
        body.put("state", p.getState().name());
        body.put("declineReason", p.getDeclineReason());
        body.put("createdAt", p.getCreatedAt());
        body.put("updatedAt", p.getUpdatedAt());
        return body;
    }

    /**
     * Strip trailing zeros so JSON serialization yields a clean canonical form
     * (e.g., {@code 7000} not {@code 7000.00000000}). Refund balance assertions
     * compare against the canonical form via {@code path("balance").toString()}.
     */
    private static BigDecimal canonicalize(BigDecimal v) {
        if (v == null) return null;
        BigDecimal stripped = v.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }

    private static HttpStatus chooseStatus(PaymentService.PaymentOutcome outcome) {
        if (outcome.replay()) {
            return HttpStatus.OK;
        }
        return switch (outcome.payment().getState()) {
            case UNKNOWN -> HttpStatus.ACCEPTED;
            case FAILED -> outcome.payment().getDeclineReason() != null
                && outcome.payment().getDeclineReason().equals("INSUFFICIENT_FUNDS")
                ? HttpStatus.UNPROCESSABLE_ENTITY
                : HttpStatus.CREATED;
            default -> HttpStatus.CREATED;
        };
    }

    private static PaymentProvider.FailureMode resolveFailureMode(String header, String bodyValue) {
        String value = header != null && !header.isBlank() ? header : bodyValue;
        if (value == null || value.isBlank()) {
            // Ordinary success path: no failure mode requested → APPROVED.
            return PaymentProvider.FailureMode.APPROVED;
        }
        // "DECLINE" is a tested alias for HTTP_4XX_DECLINE ("any failure" coverage).
        if (value.equalsIgnoreCase("DECLINE")) {
            return PaymentProvider.FailureMode.HTTP_4XX_DECLINE;
        }
        try {
            // Locale.ROOT: a default-locale fold mis-maps ASCII under some locales (Turkish
            // 'I'→'ı'), so a valid mode like "timeout" would become "TİMEOUT", miss the enum, and —
            // under the old fail-OPEN — silently downgrade a requested provider FAILURE to APPROVED.
            return PaymentProvider.FailureMode.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            // Fail CLOSED: an explicit but unrecognized failure mode must NOT become APPROVED (that
            // would flip a requested FAILURE into a SUCCESS on a payment path). Reject with 400.
            // The (unrecognized) value is NOT echoed back.
            throw new PaymentValidationException("Unknown provider failure mode");
        }
    }

    private static Instant parseInstant(String input) {
        if (input == null || input.isBlank()) return null;
        try {
            return Instant.parse(input);
        } catch (Exception ignored) {
            return null;
        }
    }
}
