package com.ax.template.authblueprint.ratingsummary;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * Denormalized derived aggregate: cached averageRating + reviewCount for a product.
 *
 * <p>Invariant traces:
 * <ul>
 *   <li>DERIVED-AGG-CONSISTENCY-001 — only mutated via {@code recomputeFrom}; no public setter</li>
 *   <li>DERIVED-AGG-EMPTY-001 — empty eligible set yields average=0.00, reviewCount=0 (sentinel)</li>
 * </ul>
 *
 * <p>Public setters for {@code average} and {@code reviewCount} are intentionally absent.
 * The only mutation path is {@link #recomputeFrom(List)}, which is package-private and
 * called exclusively by {@link RatingSummaryService} within the same transaction.
 */
@AggregateRoot
@Entity
@Table(name = "rating_summaries")
public class RatingSummary {

    @Id
    @Column(name = "product_id", updatable = false, nullable = false)
    private UUID productId;

    @Column(name = "average", nullable = false, precision = 10, scale = 2)
    private BigDecimal average;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected RatingSummary() {}

    static RatingSummary empty(UUID productId) {
        RatingSummary s = new RatingSummary();
        s.productId = productId;
        s.average = BigDecimal.ZERO.setScale(2);
        s.reviewCount = 0;
        return s;
    }

    public UUID getProductId() { return productId; }
    public BigDecimal getAverage() { return average; }
    public int getReviewCount() { return reviewCount; }

    /**
     * Recomputes aggregate from the current eligible (APPROVED) review set.
     *
     * <p>DERIVED-AGG-EMPTY-001: empty list → average=0.00, reviewCount=0 — no divide-by-zero.
     * Package-private to prevent callers outside the domain from bypassing the service.
     */
    void recomputeFrom(List<Review> eligible) {
        this.reviewCount = eligible.size();
        if (eligible.isEmpty()) {
            this.average = BigDecimal.ZERO.setScale(2);
        } else {
            int sum = eligible.stream().mapToInt(Review::getStars).sum();
            this.average = BigDecimal.valueOf(sum)
                .divide(BigDecimal.valueOf(eligible.size()), 2, RoundingMode.HALF_UP);
        }
    }
}
