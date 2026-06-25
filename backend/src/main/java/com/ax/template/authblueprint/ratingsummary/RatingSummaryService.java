package com.ax.template.authblueprint.ratingsummary;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import com.ax.template.authblueprint.ratingsummary.RatingSummaryDtos.ReviewResponse;
import com.ax.template.authblueprint.ratingsummary.RatingSummaryDtos.SummaryResponse;
import com.ax.template.authblueprint.ratingsummary.RatingSummaryExceptions.InvalidStarsException;
import com.ax.template.authblueprint.ratingsummary.RatingSummaryExceptions.ReviewNotFoundException;

/**
 * Sole mutator for the ratingsummary domain.
 *
 * <p>Invariant traces:
 * <ul>
 *   <li>DERIVED-AGG-CONSISTENCY-001 — every state change (approve/reject) calls
 *       {@link #recompute(UUID)} in the SAME transaction, so the stored aggregate
 *       always equals the independent repo AVG/COUNT derivation.</li>
 *   <li>DERIVED-AGG-ELIGIBILITY-001 — only APPROVED reviews are loaded for recompute.</li>
 *   <li>DERIVED-AGG-EMPTY-001 — delegated to {@link RatingSummary#recomputeFrom}.</li>
 * </ul>
 */
@Service
public class RatingSummaryService {

    private final ReviewRepository reviewRepository;
    private final RatingSummaryRepository summaryRepository;

    public RatingSummaryService(ReviewRepository reviewRepository,
                                RatingSummaryRepository summaryRepository) {
        this.reviewRepository = reviewRepository;
        this.summaryRepository = summaryRepository;
    }

    /**
     * Persists a new PENDING review. PENDING reviews do NOT trigger a recompute
     * (DERIVED-AGG-ELIGIBILITY-001 — not eligible until APPROVED), and deliberately
     * do NOT touch the RatingSummary aggregate: a pending review cannot move the
     * aggregate, so this method writes ONLY its own (Review) aggregate. The summary
     * row materializes lazily in {@link #recompute} on the first approval; an absent
     * summary reads as the empty sentinel via {@link #getSummary}. (DDD: a single
     * @Transactional method must not write two aggregate roots — this keeps add-review
     * single-aggregate, leaving the cross-aggregate recompute to approve/reject.)
     */
    @Transactional
    public ReviewResponse addReview(UUID productId, int stars) {
        if (stars < 1 || stars > 5) {
            throw new InvalidStarsException(stars);
        }
        Review review = Review.create(productId, stars);
        return ReviewResponse.from(reviewRepository.save(review));
    }

    /**
     * Transitions a review to APPROVED and immediately recomputes the aggregate
     * in the SAME transaction (DERIVED-AGG-CONSISTENCY-001).
     */
    @Transactional
    public ReviewResponse approveReview(UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ReviewNotFoundException(reviewId));
        review.approve();
        reviewRepository.save(review);
        recompute(review.getProductId());
        return ReviewResponse.from(review);
    }

    /**
     * Transitions a review to REJECTED and immediately recomputes the aggregate
     * in the SAME transaction (DERIVED-AGG-CONSISTENCY-001).
     */
    @Transactional
    public ReviewResponse rejectReview(UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ReviewNotFoundException(reviewId));
        review.reject();
        reviewRepository.save(review);
        recompute(review.getProductId());
        return ReviewResponse.from(review);
    }

    @Transactional(readOnly = true)
    public SummaryResponse getSummary(UUID productId) {
        RatingSummary summary = summaryRepository.findById(productId)
            .orElseGet(() -> RatingSummary.empty(productId));
        return SummaryResponse.from(summary);
    }

    /**
     * Loads the current APPROVED reviews and recomputes the aggregate within the
     * calling transaction (DERIVED-AGG-CONSISTENCY-001).
     */
    private void recompute(UUID productId) {
        RatingSummary summary = summaryRepository.findByIdForUpdate(productId)
            .orElseGet(() -> summaryRepository.save(RatingSummary.empty(productId)));
        List<Review> eligible = reviewRepository.findByProductIdAndStatus(productId, ReviewStatus.APPROVED);
        summary.recomputeFrom(eligible);
        summaryRepository.save(summary);
    }
}
