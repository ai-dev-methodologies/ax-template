package com.ax.template.authblueprint.offereligibility;

import com.ax.template.authblueprint.common.AggregateRoot;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * An offer/discount eligibility DECLARATION — the WHO/WHICH-ITEMS applicability gate, distinct
 * from discount MATH (promotion-l0 owns the math; this aggregate owns applicability only).
 *
 * <p>An offer applies to an order ONLY when its eligibility predicate holds, evaluated
 * deterministically from these DECLARED criteria by {@link OfferEligibilityService} (the sole
 * evaluator) — two independent gates:
 * <ol>
 *   <li><b>Qualifier→target minimum-quantity (BOGO-style)</b>: the {@code targetSku}/{@code targetTag}
 *       line is discounted ONLY when the qualifying lines ({@code qualifierSku}/{@code qualifierTag})
 *       meet {@code minQualifierQty}. Below the threshold ⇒ NOT applied (not an error).</li>
 *   <li><b>Customer/segment eligibility</b>: the offer is gated to an explicit customer allow-list
 *       ({@code eligibleCustomerIds}, the customer-xref) OR a matched {@code eligibleSegment}.</li>
 * </ol>
 *
 * <p>The criteria are immutable ({@code @Column(updatable=false)}, no public setter); the
 * declaration is a pure value the evaluator reads. Any criterion may be absent — the evaluator
 * fail-closes on missing/unknown criteria (deny by default), so a mis-declared offer can never
 * reach the discount-application path.
 *
 * <p>{@code discountBasisPoints} is the DECLARED discount magnitude carried for the downstream
 * math engine; it is NOT computed here (this aggregate decides applicability, never amounts).
 */
@AggregateRoot
@Entity(name = "EligibilityOffer")
@Table(name = "eligibility_offers", uniqueConstraints = {
    @UniqueConstraint(name = "uq_eligibility_offer_name", columnNames = {"name"})
})
@Check(constraints = "min_qualifier_qty >= 1 AND discount_basis_points >= 0")
public class EligibilityOffer {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "name", nullable = false, updatable = false, length = 200)
    private String name;

    /** Qualifier criterion by SKU (nullable — qualifier may be declared by tag instead). */
    @Column(name = "qualifier_sku", updatable = false, length = 100)
    private String qualifierSku;

    /** Qualifier criterion by tag (nullable — qualifier may be declared by SKU instead). */
    @Column(name = "qualifier_tag", updatable = false, length = 100)
    private String qualifierTag;

    /** Minimum total quantity of qualifying lines required before the target is discounted (>= 1). */
    @Column(name = "min_qualifier_qty", nullable = false, updatable = false)
    private int minQualifierQty;

    /** Target criterion by SKU (nullable — target may be declared by tag instead). */
    @Column(name = "target_sku", updatable = false, length = 100)
    private String targetSku;

    /** Target criterion by tag (nullable — target may be declared by SKU instead). */
    @Column(name = "target_tag", updatable = false, length = 100)
    private String targetTag;

    /** Declared discount magnitude in basis-points, carried for the downstream math engine (>= 0). */
    @Column(name = "discount_basis_points", nullable = false, updatable = false)
    private long discountBasisPoints;

    /** Eligible customer segment (nullable — eligibility may be declared by allow-list instead). */
    @Column(name = "eligible_segment", updatable = false, length = 100)
    private String eligibleSegment;

    /** Customer-xref allow-list: the explicit set of customer ids the offer is gated to. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "eligibility_offer_customers",
        joinColumns = @JoinColumn(name = "offer_id"))
    @Column(name = "customer_id", nullable = false)
    private Set<UUID> eligibleCustomerIds = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected EligibilityOffer() {}

    public EligibilityOffer(UUID id, String name,
                            String qualifierSku, String qualifierTag, int minQualifierQty,
                            String targetSku, String targetTag, long discountBasisPoints,
                            String eligibleSegment, Set<UUID> eligibleCustomerIds, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.qualifierSku = qualifierSku;
        this.qualifierTag = qualifierTag;
        this.minQualifierQty = minQualifierQty;
        this.targetSku = targetSku;
        this.targetTag = targetTag;
        this.discountBasisPoints = discountBasisPoints;
        this.eligibleSegment = eligibleSegment;
        this.eligibleCustomerIds = eligibleCustomerIds == null
            ? new LinkedHashSet<>() : new LinkedHashSet<>(eligibleCustomerIds);
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public Long getVersion() { return version; }
    public String getName() { return name; }
    public String getQualifierSku() { return qualifierSku; }
    public String getQualifierTag() { return qualifierTag; }
    public int getMinQualifierQty() { return minQualifierQty; }
    public String getTargetSku() { return targetSku; }
    public String getTargetTag() { return targetTag; }
    public long getDiscountBasisPoints() { return discountBasisPoints; }
    public String getEligibleSegment() { return eligibleSegment; }

    /** Defensive copy — the allow-list is immutable after construction. */
    public Set<UUID> getEligibleCustomerIds() { return Set.copyOf(eligibleCustomerIds); }

    public Instant getCreatedAt() { return createdAt; }
}
