package com.ax.template.authblueprint.requestvalidation;

import jakarta.validation.Valid;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * request-validation-l0 reference workload — a single command endpoint whose
 * {@link CreateOrderRequest} body carries the full declarative validation contract. Every
 * rejection (missing field, wrong type, unknown field, failed constraint, nested/collection
 * violation, cross-field rule) is raised by Spring/Jackson BEFORE this method runs and
 * shaped into RFC 9457 problem+json by {@link RequestValidationAdvice}.
 *
 * <p>On success the body is accepted and the only transform is the explicit allowlist
 * {@link NameNormalizer} (VALIDATION-SANITIZE-001) — the response echoes the normalized
 * customer so the named transform is observable, proving normalization is NOT a silent
 * cleaning of malformed input (which is rejected upstream).
 *
 * <p>Spec: specs/request-validation-l0.yaml.
 */
@RestController
@RequestMapping("/api/request-validation/orders")
public class RequestValidationDemoController {

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody CreateOrderRequest request) {
        // Reached ONLY when the declared contract fully passed (reject-not-sanitize).
        String normalizedCustomer = NameNormalizer.normalize(request.customer());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "customer", normalizedCustomer,
                "itemCount", request.items().size()));
    }
}
