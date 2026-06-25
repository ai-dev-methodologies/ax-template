package com.ax.template.authblueprint.ratingsummary;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.Check;

import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * A product review with a star rating and eligibility status.
 *
 * <p>Invariant traces:
 * <ul>
 *   <li>DERIVED-AGG-CONSISTENCY-001 — stars value drives the recompute in the same tx</li>
 *   <li>DERIVED-AGG-ELIGIBILITY-001 — status = APPROVED required to contribute</li>
 * </ul>
 */
@AggregateRoot
@Entity
@Table(name = "rating_reviews")
@Check(constraints = "stars >= 1 AND stars <= 5")
public class Review {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(name = "stars", nullable = false, updatable = false)
    private int stars;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ReviewStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Review() {}

    static Review create(UUID productId, int stars) {
        Review r = new Review();
        r.id = UUID.randomUUID();
        r.productId = productId;
        r.stars = stars;
        r.status = ReviewStatus.PENDING;
        return r;
    }

    public UUID getId() { return id; }
    public UUID getProductId() { return productId; }
    public int getStars() { return stars; }
    public ReviewStatus getStatus() { return status; }

    // Package-private — service is the only mutator (DERIVED-AGG-CONSISTENCY-001).
    void approve() { this.status = ReviewStatus.APPROVED; }
    void reject() { this.status = ReviewStatus.REJECTED; }
}
