package com.ax.template.authblueprint.payment;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin-only endpoints for the Payment blueprint.
 *
 * <p>{@code /api/admin/**} is restricted to ROLE_ADMIN by SecurityConfig
 * (PAYMENT-AUTHZ-004). Force-void records an ADMIN_OVERRIDE ledger event for
 * non-repudiation under PCI-DSS audit scope. {@code /reconciliation/run} drives
 * a synchronous drift scan used by the observability test suite.
 *
 * <p>All repository, ledger, and metrics access is routed through
 * {@link PaymentService} — the controller is a thin routing layer and holds no
 * direct dependency on any {@code *Repository} (layer-boundary discipline).
 */
@RestController
@RequestMapping("/api/admin")
public class PaymentAdminController {

    private final PaymentService paymentService;
    private final JdbcTemplate jdbcTemplate;

    public PaymentAdminController(PaymentService paymentService,
                                  JdbcTemplate jdbcTemplate) {
        this.paymentService = paymentService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/payments/{id}/force-void")
    public Map<String, Object> forceVoid(
        @PathVariable UUID id,
        @RequestBody(required = false) Map<String, String> body,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @AuthenticationPrincipal Jwt jwt) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        String justification = body == null ? null : body.get("justification");
        Payment p = paymentService.adminForceVoid(id, adminId, justification);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", p.getId().toString());
        result.put("state", p.getState().name());
        result.put("justification", justification);
        return result;
    }

    @GetMapping("/payments/{id}/events")
    public List<Map<String, Object>> events(@PathVariable UUID id) {
        return paymentService.listEvents(id);
    }

    /**
     * Runs an inline reconciliation check across all payments. For each payment,
     * compares stored {@link Payment#getBalance()} against the ledger-derived value;
     * increments {@code recon_drift_detected_total} per divergence. The scan itself
     * lives in {@link PaymentService#runReconciliation()}.
     */
    @PostMapping("/reconciliation/run")
    public ResponseEntity<Map<String, Object>> runReconciliation() {
        PaymentService.ReconciliationResult result = paymentService.runReconciliation();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("paymentsScanned", result.paymentsScanned());
        body.put("driftDetected", result.driftDetected());
        return ResponseEntity.status(HttpStatus.OK).body(body);
    }
}
