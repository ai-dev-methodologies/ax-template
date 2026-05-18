/**
 * @ax-template-meta
 * template_id: backend/billing/BillingDto
 * layer: backend-domain
 * domain: billing
 * anchors_rule: currency-amount-precision-explicit.md
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "JEP 395 — Records as transparent data carriers (Java 16+)"
 *     url: "https://openjdk.org/jeps/395"
 *   - source_type: external
 *     citation: "ISO 4217 — KRW amounts as integer won"
 *     url: "https://www.iso.org/iso-4217-currency-codes.html"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   All monetary amounts use long (integer minor units).
 *   Records follow lang-records-for-dtos.md (PRACTICES-LANG-001).
 */
package com.example.app.billing;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * BillingDto — data transfer objects for the billing domain.
 *
 * <p>All monetary amounts are {@code long} in integer minor currency units.
 * KRW: integer won (e.g., 9900 = ₩9,900). USD: integer cents.
 * BigDecimal and float monetary fields violate rule {@code currency-amount-precision-explicit}.
 *
 * <p>Boundary: BillingDto belongs to billing domain. No import from payment domain.
 */
public final class BillingDto {

    private BillingDto() {}

    // ─── Request DTOs ──────────────────────────────────────────────────────────

    public record CreateSubscriptionRequest(
        @JsonProperty(required = true) UUID planId,
        @JsonProperty(required = true) String currency,
        int trialDays
    ) {}

    public record CancelSubscriptionRequest(
        String reason,
        boolean cancelAtPeriodEnd
    ) {}

    public record CreatePlanRequest(
        @JsonProperty(required = true) String name,
        String description,
        /** Amount in integer minor currency units (KRW: won, USD: cents). */
        @JsonProperty(required = true) long amount,
        @JsonProperty(required = true) String currency,
        @JsonProperty(required = true) int intervalDays,
        int trialDays,
        List<String> features
    ) {}

    // ─── Response DTOs ─────────────────────────────────────────────────────────

    public record SubscriptionResponse(
        UUID id,
        UUID planId,
        String planName,
        String status,
        String currency,
        LocalDate currentPeriodStart,
        LocalDate currentPeriodEnd,
        LocalDate trialEnd,
        Instant canceledAt,
        Instant createdAt
    ) {}

    public record InvoiceResponse(
        UUID id,
        UUID subscriptionId,
        /** Amount due in integer minor currency units. */
        long amountDue,
        /** Amount paid in integer minor currency units. */
        long amountPaid,
        String currency,
        String status,
        Instant issuedAt,
        Instant paidAt,
        LocalDate periodStart,
        LocalDate periodEnd,
        String providerInvoiceId
    ) {}

    public record BillingEventResponse(
        UUID id,
        UUID subscriptionId,
        String eventType,
        String idempotencyKey,
        String providerEventId,
        Instant occurredAt
    ) {}

    public record PlanResponse(
        UUID id,
        String name,
        String description,
        /** Price in integer minor currency units. */
        long amount,
        String currency,
        int intervalDays,
        int trialDays,
        boolean active,
        List<String> features
    ) {}

    // ─── Paginated response wrapper ────────────────────────────────────────────

    public record PageResponse<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int page,
        int size
    ) {}
}
