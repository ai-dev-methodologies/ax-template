package com.ax.template.authblueprint.billing;

import tools.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/**
 * Wire-format DTOs for the billing domain.
 * <p>BILLING-CUR-001 — {@code amount} uses {@link MinorUnitAmountDeserializer}
 * so Jackson rejects float JSON values with HTTP 400 (handled by
 * {@link BillingAdminController#handleUnreadable}). String inputs are also
 * rejected so the wire contract stays type-disciplined.
 */
public final class BillingDto {

    private BillingDto() {}

    public record CreatePlanRequest(
        @NotBlank @Size(max = 120) String name,
        @JsonDeserialize(using = MinorUnitAmountDeserializer.class)
        @NotNull @Min(0) Long amount,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency,
        @NotNull BillingCycle billingCycle
    ) {}

    public record PlanResponse(
        String id,
        String name,
        long amount,
        String currency,
        BillingCycle billingCycle,
        Instant createdAt
    ) {
        public static PlanResponse from(Plan p) {
            return new PlanResponse(p.getId(), p.getName(), p.getAmount(), p.getCurrency(),
                p.getBillingCycle(), p.getCreatedAt());
        }
    }

    public record CreateSubscriptionRequest(
        @NotBlank String planId,
        @NotBlank String provider
    ) {}

    public record SubscriptionResponse(
        String id,
        String userId,
        String planId,
        SubscriptionStatus status,
        String provider,
        long amount,
        String currency,
        Instant startedAt,
        Instant currentPeriodEnd
    ) {
        public static SubscriptionResponse from(Subscription s) {
            return new SubscriptionResponse(s.getId(), s.getUserId(), s.getPlanId(),
                s.getStatus(), s.getProvider(), s.getAmount(), s.getCurrency(),
                s.getStartedAt(), s.getCurrentPeriodEnd());
        }
    }

    public record SubscriptionList(List<SubscriptionResponse> items, long totalElements) {}
}
