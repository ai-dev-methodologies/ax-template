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
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * Subscription plan template (e.g., "Basic 9900 KRW/MONTH").
 * <p>Trace: BILLING-AUTHZ-003 — admin-managed; BILLING-CUR-001 — amount in
 * integer minor units (won for KRW, cents for USD).
 */
@AggregateRoot
@Entity
@Table(name = "plans")
public class Plan {

    @Id
    @Column(name = "id", length = 36, updatable = false)
    private String id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    /** Integer minor units. KRW: won; USD: cents. Float JSON rejected at deserializer (BILLING-CUR-001). */
    @Column(name = "amount", nullable = false)
    private long amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 16)
    private BillingCycle billingCycle;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Plan() {}

    private Plan(String id, String name, long amount, String currency, BillingCycle cycle,
                 Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.amount = amount;
        this.currency = Objects.requireNonNull(currency);
        this.billingCycle = Objects.requireNonNull(cycle);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Plan create(String name, long amount, String currency, BillingCycle cycle) {
        Instant now = Instant.now();
        return new Plan(UUID.randomUUID().toString(), name, amount, currency, cycle, now, now);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public long getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public BillingCycle getBillingCycle() { return billingCycle; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
}
