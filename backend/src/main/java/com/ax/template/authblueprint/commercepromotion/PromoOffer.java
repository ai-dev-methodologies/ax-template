package com.ax.template.authblueprint.commercepromotion;

import com.ax.template.authblueprint.common.AggregateRoot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.UUID;

/**
 * An offer defines a discount rule. Scope = ORDER (prorated across line items) or ITEM (per-line).
 * {@code combinable} gates whether OTHER offers may stack on the same order once this one applies;
 * {@code stackable} gates whether MULTIPLE instances of this same offer may apply.
 * These are independent axes (PROMO-STACK-001).
 * max_uses = 0 means unlimited. Enforced atomically via PESSIMISTIC_WRITE + UNIQUE(offer_id, order_ref)
 * on OfferRedemption (PROMO-MAXUSES-001 — the reference TOCTOU strengthening).
 */
@AggregateRoot
@Entity(name = "PromoOffer")
@Table(name = "promo_offers", uniqueConstraints = {
    @UniqueConstraint(name = "uq_promo_offer_name", columnNames = {"name"})
})
@Check(constraints = "discount_value >= 0 AND priority >= 0 AND max_uses >= 0 AND max_uses_per_customer >= 0")
public class PromoOffer {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, updatable = false, length = 20)
    private DiscountType discountType;

    /** Basis-points for PERCENT (1000 = 10%), minor units for FIXED. Always >= 0. */
    @Column(name = "discount_value", nullable = false)
    private long discountValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, updatable = false, length = 10)
    private OfferScope scope;

    @Column(name = "priority", nullable = false)
    private int priority;

    /** If false, once this offer applies no other offers may stack on the same order. */
    @Column(name = "combinable", nullable = false)
    private boolean combinable;

    /** If false, only one application of this specific offer is allowed per order. */
    @Column(name = "stackable", nullable = false)
    private boolean stackable;

    @Column(name = "apply_to_sale_price")
    private Boolean applyToSalePrice;

    /** 0 = unlimited global uses. */
    @Column(name = "max_uses", nullable = false)
    private long maxUses;

    /** 0 = unlimited per-customer uses. */
    @Column(name = "max_uses_per_customer", nullable = false)
    private long maxUsesPerCustomer;

    @Column(name = "active_start", nullable = false, updatable = false)
    private Instant activeStart;

    @Column(name = "active_end")
    private Instant activeEnd;

    protected PromoOffer() {}

    public PromoOffer(UUID id, String name, DiscountType discountType, long discountValue,
                 OfferScope scope, int priority, boolean combinable, boolean stackable,
                 Boolean applyToSalePrice, long maxUses, long maxUsesPerCustomer,
                 Instant activeStart, Instant activeEnd) {
        this.id = id;
        this.name = name;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.scope = scope;
        this.priority = priority;
        this.combinable = combinable;
        this.stackable = stackable;
        this.applyToSalePrice = applyToSalePrice;
        this.maxUses = maxUses;
        this.maxUsesPerCustomer = maxUsesPerCustomer;
        this.activeStart = activeStart;
        this.activeEnd = activeEnd;
    }

    public UUID getId() { return id; }
    public Long getVersion() { return version; }
    public String getName() { return name; }
    public DiscountType getDiscountType() { return discountType; }
    public long getDiscountValue() { return discountValue; }
    public OfferScope getScope() { return scope; }
    public int getPriority() { return priority; }
    public boolean isCombinable() { return combinable; }
    public boolean isStackable() { return stackable; }
    public Boolean getApplyToSalePrice() { return applyToSalePrice; }
    public long getMaxUses() { return maxUses; }
    public long getMaxUsesPerCustomer() { return maxUsesPerCustomer; }
    public Instant getActiveStart() { return activeStart; }
    public Instant getActiveEnd() { return activeEnd; }
}
