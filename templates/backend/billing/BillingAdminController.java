/**
 * @ax-template-meta
 * template_id: backend/billing/BillingAdminController
 * layer: backend-domain
 * domain: billing
 * anchors_rule: billing-event-idempotent.md
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "OWASP ASVS V4.2.1 — Admin operations must be role-gated; ROLE_ADMIN enforcement"
 *     url: "https://owasp.org/www-project-application-security-verification-standard/"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   All admin endpoints are under /api/admin/billing/** and require ROLE_ADMIN.
 *   @RequireIdempotencyKey on POST mutations.
 *   Boundary: BillingAdminController is in billing domain. Never import from payment domain.
 */
package com.example.app.billing;

import com.example.app.common.idempotency.RequireIdempotencyKey;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * BillingAdminController — admin-only billing management endpoints.
 *
 * <p>All methods require {@code ROLE_ADMIN}; non-admin access returns HTTP 403.
 * Documented in spec: BILLING-AUTHZ-003.
 *
 * <p>Boundary: BillingAdminController is in billing domain. No import from payment domain.
 */
@RestController
@RequestMapping("/api/admin/billing")
@PreAuthorize("hasRole('ADMIN')")
public class BillingAdminController {

    private final BillingService billingService;
    private final BillingMapper billingMapper;

    public BillingAdminController(BillingService billingService, BillingMapper billingMapper) {
        this.billingService = billingService;
        this.billingMapper = billingMapper;
    }

    @GetMapping("/plans")
    public ResponseEntity<List<BillingDto.PlanResponse>> listPlans() {
        return ResponseEntity.ok(
            billingService.listAllPlans().stream().map(billingMapper::toPlanResponse).toList());
    }

    @RequireIdempotencyKey
    @PostMapping("/plans")
    public ResponseEntity<BillingDto.PlanResponse> createPlan(
            @RequestBody BillingDto.CreatePlanRequest req,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        Plan plan = billingService.createPlan(req, idempotencyKey);
        return ResponseEntity
            .created(URI.create("/api/admin/billing/plans/" + plan.getId()))
            .body(billingMapper.toPlanResponse(plan));
    }
}
