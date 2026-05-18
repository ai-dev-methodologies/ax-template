/**
 * @ax-template-meta
 * template_id: backend/billing/BillingService
 * layer: backend-domain
 * domain: billing
 * anchors_rule: billing-event-idempotent.md, subscription-state-machine-explicit.md
 * provenance_class: internal_design
 * evidence:
 *   - source_type: upstream_id
 *     upstream_id: stripe-billing-2026-05
 *     section: "Idempotency"
 *     quote: "Retrying the same key within the window returns the original response without creating a duplicate resource."
 *   - source_type: external
 *     citation: "OWASP ASVS V4.2.1 — Access control verifies user owns the resource (IDOR prevention)"
 *     url: "https://owasp.org/www-project-application-security-verification-standard/"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   All status transitions go through SubscriptionStateMachine — never setStatus() directly.
 *   IDOR-safe: subscription lookup always filters by userId (404 on miss, never 403).
 *   Boundary: BillingService is in billing domain. Never import from payment domain.
 */
package com.example.app.billing;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BillingService — application service orchestrating subscription lifecycle.
 *
 * <p>All Subscription status mutations go through {@link SubscriptionStateMachine}.
 * Direct calls to {@code Subscription.setStatus()} are prohibited and enforced
 * by ArchUnit rule {@code subscription-state-machine-explicit}.
 *
 * <p>IDOR protection: all subscription lookups filter by userId — callers
 * receive 404 on missing or cross-user access, never 403 (per BILLING-AUTHZ-002).
 *
 * <p>Boundary: BillingService is in billing domain. No import from payment domain.
 */
@Service
@Transactional(readOnly = true)
public class BillingService {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final InvoiceRepository invoiceRepository;
    private final BillingEventRepository billingEventRepository;
    private final SubscriptionStateMachine stateMachine;
    private final BillingProvider billingProvider;
    private final MeterRegistry meterRegistry;

    public BillingService(
            SubscriptionRepository subscriptionRepository,
            PlanRepository planRepository,
            InvoiceRepository invoiceRepository,
            BillingEventRepository billingEventRepository,
            SubscriptionStateMachine stateMachine,
            BillingProvider billingProvider,
            MeterRegistry meterRegistry) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.invoiceRepository = invoiceRepository;
        this.billingEventRepository = billingEventRepository;
        this.stateMachine = stateMachine;
        this.billingProvider = billingProvider;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Creates a new subscription for the authenticated user.
     *
     * @param userId         the authenticated user's UUID (from JWT)
     * @param planId         target plan UUID
     * @param currency       ISO 4217 currency code
     * @param trialDays      trial period override (0 = use plan default)
     * @param idempotencyKey caller-supplied idempotency key
     * @return created subscription
     */
    @Transactional
    public Subscription createSubscription(
            UUID userId,
            UUID planId,
            String currency,
            int trialDays,
            String idempotencyKey) {
        Plan plan = planRepository.findByIdAndActiveTrue(planId)
            .orElseThrow(() -> new PlanNotFoundException(planId));

        Subscription sub = Subscription.create(userId, plan, currency);
        subscriptionRepository.save(sub);

        // Register with provider
        int effectiveTrialDays = trialDays > 0 ? trialDays : plan.getTrialDays();
        BillingProvider.ProviderSubscriptionResult providerResult = billingProvider.createSubscription(
            sub.getId(),
            plan.getAmount(),
            currency,
            plan.getIntervalDays(),
            effectiveTrialDays,
            idempotencyKey
        );
        sub.setProviderSubscriptionId(providerResult.providerSubscriptionId());

        // Record SUBSCRIPTION_CREATED event
        billingEventRepository.save(
            BillingEvent.createInternal(sub.getId(), BillingEvent.BillingEventType.SUBSCRIPTION_CREATED, null));

        meterRegistry.counter("billing.subscription.lifecycle_transition",
            "from", "NONE", "to", sub.getStatus().name(), "trigger", "CREATED").increment();

        return sub;
    }

    /**
     * Cancels a subscription. IDOR-safe: 404 if not owned by userId.
     */
    @Transactional
    public Subscription cancelSubscription(
            UUID userId,
            UUID subscriptionId,
            String reason,
            boolean cancelAtPeriodEnd,
            String idempotencyKey) {
        Subscription sub = findByIdAndUserId(subscriptionId, userId);
        billingProvider.cancelSubscription(
            sub.getProviderSubscriptionId(),
            cancelAtPeriodEnd,
            idempotencyKey
        );
        stateMachine.transition(sub, SubscriptionStateMachine.Trigger.USER_CANCEL,
            "{\"reason\":\"" + escapeJson(reason) + "\"}");
        return sub;
    }

    public Subscription getSubscription(UUID userId, UUID subscriptionId) {
        return findByIdAndUserId(subscriptionId, userId);
    }

    public Page<Subscription> listSubscriptions(UUID userId, Pageable pageable) {
        return subscriptionRepository.findByUserId(userId, pageable);
    }

    public Page<Invoice> listInvoices(UUID userId, UUID subscriptionId, Pageable pageable) {
        if (subscriptionId != null) {
            findByIdAndUserId(subscriptionId, userId); // IDOR check
            return invoiceRepository.findBySubscriptionId(subscriptionId, pageable);
        }
        return invoiceRepository.findByUserId(userId, pageable);
    }

    public Page<BillingEvent> listBillingEvents(UUID userId, UUID subscriptionId, Pageable pageable) {
        if (subscriptionId != null) {
            findByIdAndUserId(subscriptionId, userId); // IDOR check
            return billingEventRepository.findBySubscriptionId(subscriptionId, pageable);
        }
        return billingEventRepository.findBySubscriptionUserId(userId, pageable);
    }

    // ─── Admin operations ─────────────────────────────────────────────────────

    @PreAuthorize("hasRole('ADMIN')")
    public List<Plan> listAllPlans() {
        return planRepository.findAll();
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Plan createPlan(BillingDto.CreatePlanRequest req, String idempotencyKey) {
        Plan plan = Plan.create(
            req.name(), req.description(),
            req.amount(), req.currency(),
            req.intervalDays(), req.trialDays(),
            req.features()
        );
        return planRepository.save(plan);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /**
     * IDOR-safe lookup: returns 404 if subscriptionId doesn't exist or doesn't
     * belong to userId. Never returns 403 (prevents resource enumeration).
     */
    private Subscription findByIdAndUserId(UUID subscriptionId, UUID userId) {
        return subscriptionRepository.findByIdAndUserId(subscriptionId, userId)
            .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId));
    }

    private static String escapeJson(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }

    // ─── Domain exceptions ─────────────────────────────────────────────────────

    public static class SubscriptionNotFoundException extends RuntimeException {
        public SubscriptionNotFoundException(UUID id) {
            super("Subscription not found: " + id);
        }
    }

    public static class PlanNotFoundException extends RuntimeException {
        public PlanNotFoundException(UUID id) {
            super("Plan not found or inactive: " + id);
        }
    }
}
