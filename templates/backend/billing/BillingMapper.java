/**
 * @ax-template-meta
 * template_id: backend/billing/BillingMapper
 * layer: backend-domain
 * domain: billing
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "Spring Framework Reference — Component-based Spring beans as pure mapping helpers"
 *     url: "https://docs.spring.io/spring-framework/reference/core/beans/classpath-scanning.html"
 */
package com.example.app.billing;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * BillingMapper — converts domain entities to DTO response records.
 *
 * <p>Keeps mapping logic out of service and controller classes.
 */
@Component
public class BillingMapper {

    public BillingDto.SubscriptionResponse toResponse(Subscription s) {
        return new BillingDto.SubscriptionResponse(
            s.getId(),
            s.getPlan().getId(),
            s.getPlan().getName(),
            s.getStatus().name(),
            s.getCurrency(),
            s.getCurrentPeriodStart(),
            s.getCurrentPeriodEnd(),
            s.getTrialEnd(),
            s.getCanceledAt(),
            s.getCreatedAt()
        );
    }

    public BillingDto.InvoiceResponse toInvoiceResponse(Invoice inv) {
        return new BillingDto.InvoiceResponse(
            inv.getId(),
            inv.getSubscription().getId(),
            inv.getAmountDue(),
            inv.getAmountPaid(),
            inv.getCurrency(),
            inv.getStatus().name(),
            inv.getIssuedAt(),
            inv.getPaidAt(),
            inv.getPeriodStart(),
            inv.getPeriodEnd(),
            inv.getProviderInvoiceId()
        );
    }

    public BillingDto.BillingEventResponse toBillingEventResponse(BillingEvent e) {
        return new BillingDto.BillingEventResponse(
            e.getId(),
            e.getSubscriptionId(),
            e.getEventType().name(),
            e.getIdempotencyKey(),
            e.getProviderEventId(),
            e.getOccurredAt()
        );
    }

    public BillingDto.PlanResponse toPlanResponse(Plan p) {
        return new BillingDto.PlanResponse(
            p.getId(),
            p.getName(),
            p.getDescription(),
            p.getAmount(),
            p.getCurrency(),
            p.getIntervalDays(),
            p.getTrialDays(),
            p.isActive(),
            p.getFeatures()
        );
    }

    public <E, R> BillingDto.PageResponse<R> toPageResponse(Page<E> page, Function<E, R> mapper) {
        List<R> content = page.getContent().stream().map(mapper).toList();
        return new BillingDto.PageResponse<>(
            content,
            page.getTotalElements(),
            page.getTotalPages(),
            page.getNumber(),
            page.getSize()
        );
    }
}
