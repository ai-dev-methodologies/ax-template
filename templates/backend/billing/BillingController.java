/**
 * @ax-template-meta
 * template_id: backend/billing/BillingController
 * layer: backend-domain
 * domain: billing
 * anchors_rule: billing-event-idempotent.md
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "RFC 7807 — Problem Details for HTTP APIs (ProblemDetail error envelope)"
 *     url: "https://datatracker.ietf.org/doc/html/rfc7807"
 *   - source_type: external
 *     citation: "IETF draft — Idempotency-Key header"
 *     url: "https://datatracker.ietf.org/doc/draft-ietf-httpapi-idempotency-key-header/"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   @RequireIdempotencyKey on all POST mutations (billing-event-idempotent rule).
 *   IDOR-safe: all lookups filtered by userId (404 on miss, never 403).
 *   Boundary: BillingController is in billing domain. Never import from payment domain.
 */
package com.example.app.billing;

import com.example.app.common.idempotency.RequireIdempotencyKey;
import java.net.URI;
import java.security.Principal;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * BillingController — REST endpoints for subscription lifecycle, invoice, and billing events.
 *
 * <p>Security: all endpoints require authentication (SecurityConfig maps
 * {@code /api/subscriptions/**} and {@code /api/billing/**} to requiresAuthentication()).
 *
 * <p>Idempotency: all POST mutation handlers carry {@code @RequireIdempotencyKey}.
 * The {@code IdempotencyFilter} intercepts and deduplicates retries within 24 h.
 *
 * <p>IDOR: subscription lookups are always filtered by the authenticated user's UUID.
 * Cross-user access returns 404, not 403.
 *
 * <p>Boundary: BillingController is in billing domain. No import from payment domain.
 */
@RestController
@RequestMapping("/api")
public class BillingController {

    private final BillingService billingService;
    private final BillingMapper billingMapper;

    public BillingController(BillingService billingService, BillingMapper billingMapper) {
        this.billingService = billingService;
        this.billingMapper = billingMapper;
    }

    // ─── Subscriptions ─────────────────────────────────────────────────────────

    @RequireIdempotencyKey
    @PostMapping("/subscriptions")
    public ResponseEntity<BillingDto.SubscriptionResponse> createSubscription(
            @RequestBody BillingDto.CreateSubscriptionRequest req,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        Subscription sub = billingService.createSubscription(
            userId, req.planId(), req.currency(), req.trialDays(), idempotencyKey);
        return ResponseEntity
            .created(URI.create("/api/subscriptions/" + sub.getId()))
            .body(billingMapper.toResponse(sub));
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<BillingDto.PageResponse<BillingDto.SubscriptionResponse>> listSubscriptions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        var pageResult = billingService.listSubscriptions(userId, PageRequest.of(page, Math.min(size, 100)));
        return ResponseEntity.ok(billingMapper.toPageResponse(pageResult, billingMapper::toResponse));
    }

    @GetMapping("/subscriptions/{id}")
    public ResponseEntity<BillingDto.SubscriptionResponse> getSubscription(
            @PathVariable UUID id,
            Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        return ResponseEntity.ok(billingMapper.toResponse(billingService.getSubscription(userId, id)));
    }

    @RequireIdempotencyKey
    @PostMapping("/subscriptions/{id}/cancel")
    public ResponseEntity<BillingDto.SubscriptionResponse> cancelSubscription(
            @PathVariable UUID id,
            @RequestBody(required = false) BillingDto.CancelSubscriptionRequest req,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        String reason = req != null ? req.reason() : null;
        boolean atPeriodEnd = req != null && req.cancelAtPeriodEnd();
        Subscription sub = billingService.cancelSubscription(userId, id, reason, atPeriodEnd, idempotencyKey);
        return ResponseEntity.ok(billingMapper.toResponse(sub));
    }

    // ─── Invoices ──────────────────────────────────────────────────────────────

    @GetMapping("/billing/invoices")
    public ResponseEntity<BillingDto.PageResponse<BillingDto.InvoiceResponse>> listInvoices(
            @RequestParam(required = false) UUID subscriptionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        var pageResult = billingService.listInvoices(userId, subscriptionId, PageRequest.of(page, size));
        return ResponseEntity.ok(billingMapper.toPageResponse(pageResult, billingMapper::toInvoiceResponse));
    }

    // ─── Billing events ────────────────────────────────────────────────────────

    @GetMapping("/billing/events")
    public ResponseEntity<BillingDto.PageResponse<BillingDto.BillingEventResponse>> listBillingEvents(
            @RequestParam(required = false) UUID subscriptionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        var pageResult = billingService.listBillingEvents(userId, subscriptionId, PageRequest.of(page, size));
        return ResponseEntity.ok(billingMapper.toPageResponse(pageResult, billingMapper::toBillingEventResponse));
    }
}
