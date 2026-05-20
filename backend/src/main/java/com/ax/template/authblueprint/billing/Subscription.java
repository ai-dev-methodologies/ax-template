package com.ax.template.authblueprint.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Customer's subscription instance.
 * <p>Trace:
 * <ul>
 *   <li>BILLING-AUTHZ-002 — owner is {@code userId}; cross-user lookup returns 404.</li>
 *   <li>BILLING-STATE-001 — {@link #setStatus(SubscriptionStatus)} is package-private,
 *       enforced by ArchUnit ({@link SubscriptionStateMachineOnlyMutatorTest}). The
 *       state machine is the sole caller.</li>
 *   <li>BILLING-CUR-001 — {@code amount} is integer minor units.</li>
 * </ul>
 */
@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @Column(name = "id", length = 36, updatable = false)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36, updatable = false)
    private String userId;

    @Column(name = "plan_id", nullable = false, length = 36)
    private String planId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private SubscriptionStatus status;

    @Column(name = "provider", nullable = false, length = 32)
    private String provider;

    @Column(name = "amount", nullable = false)
    private long amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Subscription() {}

    private Subscription(String id, String userId, String planId, SubscriptionStatus status,
                         String provider, long amount, String currency,
                         Instant startedAt, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.planId = Objects.requireNonNull(planId);
        this.status = Objects.requireNonNull(status);
        this.provider = Objects.requireNonNull(provider);
        this.amount = amount;
        this.currency = Objects.requireNonNull(currency);
        this.startedAt = startedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Subscription createTrial(String userId, Plan plan, String provider) {
        Instant now = Instant.now();
        return new Subscription(UUID.randomUUID().toString(), userId, plan.getId(),
            SubscriptionStatus.TRIAL, provider, plan.getAmount(), plan.getCurrency(),
            now, now, now);
    }

    public static Subscription createActive(String userId, Plan plan, String provider) {
        Instant now = Instant.now();
        return new Subscription(UUID.randomUUID().toString(), userId, plan.getId(),
            SubscriptionStatus.ACTIVE, provider, plan.getAmount(), plan.getCurrency(),
            now, now, now);
    }

    /**
     * BILLING-STATE-001: mutator is package-private. Only
     * {@link SubscriptionStateMachine} may call it (ArchUnit enforced).
     */
    void setStatus(SubscriptionStatus next) {
        this.status = Objects.requireNonNull(next);
        this.updatedAt = Instant.now();
    }

    void setCurrentPeriodEnd(Instant end) {
        this.currentPeriodEnd = end;
        this.updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getPlanId() { return planId; }
    public SubscriptionStatus getStatus() { return status; }
    public String getProvider() { return provider; }
    public long getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCurrentPeriodEnd() { return currentPeriodEnd; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
}
