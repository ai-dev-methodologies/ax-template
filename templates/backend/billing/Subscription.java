/**
 * @ax-template-meta
 * template_id: backend/billing/Subscription
 * layer: backend-domain
 * domain: billing
 * anchors_rule: subscription-state-machine-explicit.md
 * provenance_class: internal_design
 * evidence:
 *   - source_type: upstream_id
 *     upstream_id: stripe-billing-2026-05
 *     section: "Subscription lifecycle"
 *     quote: "trialing — trial period active; active — subscription is current; past_due — latest invoice payment attempt failed; canceled — subscription ended"
 *   - source_type: external
 *     citation: "ISO 4217 — KRW amounts as integer won"
 *     url: "https://www.iso.org/iso-4217-currency-codes.html"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Subscription status must only be changed via SubscriptionStateMachine — never setStatus() directly.
 *   @SQLDelete required on this @Entity subclass.
 *   currentPeriodStart/End are LocalDate (date-only, no time component — no time-picker needed).
 */
package com.example.app.billing;

import com.example.app.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.SQLDelete;

/**
 * Subscription entity — recurring billing subscription for a user.
 *
 * <p>Status lifecycle (managed exclusively by {@link SubscriptionStateMachine}):
 * <pre>
 *   TRIAL → ACTIVE   (trial ends, payment succeeds)
 *   ACTIVE → PAST_DUE (payment fails)
 *   ACTIVE → CANCELLED (user cancels)
 *   PAST_DUE → ACTIVE  (payment retried successfully)
 *   PAST_DUE → CANCELLED (unpaid threshold exceeded)
 * </pre>
 *
 * <p>CRITICAL: Do NOT call {@code setStatus()} directly from service code.
 * Use {@code SubscriptionStateMachine.transition(subscription, trigger)}.
 * This is enforced by ArchUnit rule {@code OnlyStateMachineMutatesSubscriptionStatusArchTest}.
 *
 * <p>Boundary: Subscription belongs to billing domain. No import from payment domain.
 */
@Entity
@SQLDelete(sql = "UPDATE subscriptions SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Table(
    name = "subscriptions",
    indexes = {
        @Index(name = "idx_subscriptions_user_status", columnList = "user_id, status"),
        @Index(name = "idx_subscriptions_period_end", columnList = "current_period_end"),
    }
)
public class Subscription extends BaseEntity {

    /** User who owns this subscription. Resolved from JWT on every service call. */
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private SubscriptionStatus status;

    /** ISO 4217 currency code. Frozen at subscription creation. */
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    /** Billing period start (date-only — no time component). */
    @Column(name = "current_period_start", nullable = false)
    private LocalDate currentPeriodStart;

    /** Billing period end (date-only). Next invoice date. */
    @Column(name = "current_period_end", nullable = false)
    private LocalDate currentPeriodEnd;

    /** Trial end date. Null if no trial. */
    @Column(name = "trial_end")
    private LocalDate trialEnd;

    /** Cancellation timestamp. Null if not canceled. */
    @Column(name = "canceled_at")
    private Instant canceledAt;

    /** Optional: provider-assigned subscription ID (e.g., Stripe sub_xxx, Toss billingKey). */
    @Column(name = "provider_subscription_id", length = 255)
    private String providerSubscriptionId;

    protected Subscription() {}

    /**
     * Factory constructor — creates a new TRIAL subscription (if plan has trialDays > 0)
     * or ACTIVE subscription (if plan.trialDays == 0).
     */
    public static Subscription create(UUID userId, Plan plan, String currency) {
        var s = new Subscription();
        s.userId = userId;
        s.plan = plan;
        s.currency = currency;
        var today = LocalDate.now();
        s.currentPeriodStart = today;
        s.currentPeriodEnd = today.plusDays(plan.getIntervalDays());
        if (plan.getTrialDays() > 0) {
            s.status = SubscriptionStatus.TRIAL;
            s.trialEnd = today.plusDays(plan.getTrialDays());
        } else {
            s.status = SubscriptionStatus.ACTIVE;
            s.trialEnd = null;
        }
        return s;
    }

    // ─── Package-private mutators (called only by SubscriptionStateMachine) ──────

    /** Called by SubscriptionStateMachine only. Do NOT call from service code. */
    void applyStatusTransition(SubscriptionStatus newStatus) {
        this.status = newStatus;
        if (newStatus == SubscriptionStatus.CANCELLED) {
            this.canceledAt = Instant.now();
        }
    }

    /** Called by SubscriptionStateMachine to advance billing period. */
    void advanceBillingPeriod() {
        this.currentPeriodStart = this.currentPeriodEnd;
        this.currentPeriodEnd = this.currentPeriodEnd.plusDays(plan.getIntervalDays());
    }

    /** Called by SubscriptionStateMachine when provider assigns a subscription ID. */
    void setProviderSubscriptionId(String id) {
        this.providerSubscriptionId = id;
    }

    public UUID getUserId()                      { return userId; }
    public Plan getPlan()                        { return plan; }
    public SubscriptionStatus getStatus()        { return status; }
    public String getCurrency()                  { return currency; }
    public LocalDate getCurrentPeriodStart()     { return currentPeriodStart; }
    public LocalDate getCurrentPeriodEnd()       { return currentPeriodEnd; }
    public LocalDate getTrialEnd()               { return trialEnd; }
    public Instant getCanceledAt()               { return canceledAt; }
    public String getProviderSubscriptionId()    { return providerSubscriptionId; }

    public enum SubscriptionStatus {
        TRIAL, ACTIVE, PAST_DUE, CANCELLED
    }
}
