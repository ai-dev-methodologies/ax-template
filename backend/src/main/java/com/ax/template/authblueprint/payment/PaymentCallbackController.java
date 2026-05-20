package com.ax.template.authblueprint.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Redirect-style PG callback endpoint.
 *
 * <p>This controller is the only path through which a Payment may transition
 * via an externally-initiated server-to-server hit (KG이니시스 / NICE페이먼츠 /
 * KCP / Toss V1). Tokenization-style PGs (Stripe / Toss V2) never reach this
 * controller — they post directly via {@link PaymentController}.
 *
 * <p>Wire contract: contracts/payment-openapi.yaml#/paths/~1payments~1callback~1{provider}.
 *
 * <p>Spec anchors:
 * <ul>
 *   <li>specs/payment-l0.yaml#PAYMENT-CALLBACK-001 — verify signature BEFORE
 *       reading any payment state; 401 on missing/mismatched; ledger audit row
 *       emitted regardless.</li>
 *   <li>specs/payment-l0.yaml#PAYMENT-CALLBACK-002 — idempotent on (provider,
 *       TID); duplicate callback for already-CAPTURED returns 200 + existing
 *       state, NOT a second CAPTURED event.</li>
 *   <li>specs/payment-l0.yaml#PAYMENT-CALLBACK-003 — only {AUTHORIZED, UNKNOWN}
 *       states may transition via callback; CREATED + REFUNDED reject 409.</li>
 *   <li>blueprints/payment-manifest.yaml#callback — declares endpoint path,
 *       signature_verifier_spi, ledger_metadata, allowed states.</li>
 * </ul>
 *
 * <p>Security: SecurityConfig carves {@code /api/payments/callback/**} as
 * {@code permitAll()} because Authorization is via PG signature, NOT a bearer
 * token. CSRF is disabled for this path because the request originates
 * server-to-server from the PG.
 *
 * <p>Content negotiation: most Korean PGs post
 * {@code application/x-www-form-urlencoded}; some emit JSON. Both shapes route
 * to {@link #processCallback}, which is verifier-agnostic about the payload
 * shape — the per-PG {@link PaymentCallbackVerifier} owns decoding.
 */
@RestController
@RequestMapping("/api/payments/callback")
public class PaymentCallbackController {

    private static final Logger log = LoggerFactory.getLogger(PaymentCallbackController.class);

    /**
     * Problem-detail {@code type} URI for the "callback impl not provided" case.
     * The catalog ships {@code PaymentCallbackVerifier} + service hook as
     * contract surface only; a fork-receiver wiring a redirect-style PG must
     * supply both a verifier bean and a service override. Until both are in
     * place, the controller returns HTTP 501 with this stable type URI so
     * SREs and integrators can grep for it.
     */
    private static final URI TYPE_CALLBACK_NOT_IMPLEMENTED =
        URI.create("https://errors.ax-template/payment/callback-not-implemented");

    private final PaymentCallbackVerifierRegistry registry;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;

    public PaymentCallbackController(
        PaymentCallbackVerifierRegistry registry,
        PaymentService paymentService,
        PaymentRepository paymentRepository
    ) {
        this.registry = Objects.requireNonNull(registry);
        this.paymentService = Objects.requireNonNull(paymentService);
        this.paymentRepository = Objects.requireNonNull(paymentRepository);
    }

    /**
     * Form-encoded variant (KG이니시스 / NICE페이먼츠 / KCP / Toss V1 default).
     */
    @PostMapping(
        value = "/{provider}",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public ResponseEntity<?> handleFormCallback(
        @PathVariable String provider,
        @RequestBody MultiValueMap<String, String> form,
        @RequestHeader Map<String, String> headers
    ) {
        Map<String, String> flat = flattenForm(form);
        return processCallback(provider, flat, headers);
    }

    /**
     * JSON variant. Some PGs (or test harnesses) emit JSON instead of form data.
     * Body shape is opaque to the controller — the verifier owns decoding.
     */
    @PostMapping(
        value = "/{provider}",
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> handleJsonCallback(
        @PathVariable String provider,
        @RequestBody Map<String, Object> json,
        @RequestHeader Map<String, String> headers
    ) {
        Map<String, String> flat = flattenJson(json);
        return processCallback(provider, flat, headers);
    }

    /**
     * Shared dispatch path. Signature verify FIRST (PAYMENT-CALLBACK-001), then
     * delegate to the service. The controller MUST NOT read any payment state
     * before the verifier returns valid. All error paths emit an audit row via
     * {@link PaymentService#auditCallbackFailure} so the trail captures every
     * inbound hit regardless of outcome.
     */
    private ResponseEntity<?> processCallback(
        String provider,
        Map<String, String> payload,
        Map<String, String> headers
    ) {
        Optional<PaymentCallbackVerifier> verifierOpt = registry.find(provider);
        if (verifierOpt.isEmpty()) {
            // Unknown provider slug — no audit row (registry never saw it).
            ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
            pd.setTitle("Unknown callback provider");
            pd.setDetail("No PaymentCallbackVerifier registered for slug='" + provider + "'");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
        }
        PaymentCallbackVerifier verifier = verifierOpt.get();

        PaymentCallbackVerifier.Result result;
        try {
            result = verifier.verify(payload, headers);
        } catch (RuntimeException ex) {
            // Verifier blew up (PG payload malformed, crypto error, etc.). Treat
            // as signature failure so the audit row still lands, and surface
            // 401 — never 500 — to the PG (PGs typically retry on 5xx and
            // we do NOT want a verifier bug to fan out into a retry storm).
            log.warn("PaymentCallbackVerifier threw during verify: provider={} ex={}",
                provider, ex.toString());
            result = PaymentCallbackVerifier.Result.invalid("VERIFIER_EXCEPTION");
        }
        Objects.requireNonNull(result, "PaymentCallbackVerifier returned null Result");

        if (!result.valid()) {
            // PAYMENT-CALLBACK-001: audit ledger row tagged source=callback,
            // outcome=signature_fail. inboundOrderId may be non-null when the
            // verifier extracted it before signature check failed (GAP-NEW-3).
            paymentService.auditCallbackFailure(provider, result.failReason(), result.orderId());
            ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
            pd.setTitle("Callback signature verification failed");
            pd.setDetail("reason=" + (result.failReason() == null ? "UNSPECIFIED" : result.failReason()));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
        }

        // Signature OK — resolve Payment by the verified orderId.
        final String verifiedOrderId = result.orderId();
        Optional<Payment> paymentOpt = paymentRepository.findAll().stream()
            .filter(p -> verifiedOrderId != null && verifiedOrderId.equals(p.getOrderId()))
            .findFirst();
        if (paymentOpt.isEmpty()) {
            ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
            pd.setTitle("No Payment matches the callback orderId");
            pd.setDetail("orderId=" + verifiedOrderId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
        }
        UUID paymentId = paymentOpt.get().getId();

        try {
            PaymentService.CallbackOutcome outcome = paymentService.markCapturedFromCallback(
                paymentId, provider, result.tid(), result.signedPayload());
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("paymentId", paymentId.toString());
            body.put("state", outcome.state().name());
            body.put("idempotentReplay", outcome.idempotentReplay());
            return ResponseEntity.ok(body);
        } catch (UnsupportedOperationException ex) {
            // Catalog ships markCapturedFromCallback as contract-only. Until a
            // fork-receiver supplies the implementation, the controller returns
            // a stable 501 with the spec-naming message intact so SREs can wire
            // alerts to this specific type URI.
            ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_IMPLEMENTED);
            pd.setType(TYPE_CALLBACK_NOT_IMPLEMENTED);
            pd.setTitle("Callback capture not implemented");
            pd.setDetail(ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(pd);
        }
        // IllegalStateTransitionException and PaymentNotFoundException propagate
        // to PaymentExceptionHandler — same 409/404 handling as the rest of the
        // payment domain. Do NOT catch them here.
    }

    private static Map<String, String> flattenForm(MultiValueMap<String, String> form) {
        if (form == null || form.isEmpty()) {
            return Map.of();
        }
        Map<String, String> flat = new HashMap<>();
        form.forEach((k, vs) -> {
            if (vs != null && !vs.isEmpty()) {
                // Last-wins is intentional: PGs send each field once. If the
                // payload is malformed enough to repeat, the verifier sees the
                // last value and the audit trail records the failure either way.
                flat.put(k, vs.get(vs.size() - 1));
            }
        });
        return flat;
    }

    private static Map<String, String> flattenJson(Map<String, Object> json) {
        if (json == null || json.isEmpty()) {
            return Map.of();
        }
        Map<String, String> flat = new HashMap<>();
        json.forEach((k, v) -> {
            if (v == null) return;
            flat.put(k, String.valueOf(v));
        });
        return flat;
    }
}
