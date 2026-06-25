package com.ax.template.authblueprint.ratingsummary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findByProductIdAndStatus(UUID productId, ReviewStatus status);

    /**
     * INDEPENDENT re-derivation via JPQL AVG — a DIFFERENT code path from the in-memory fold
     * used by {@link RatingSummary#recomputeFrom}. Used by compliance tests to cross-check
     * (DERIVED-AGG-CONSISTENCY-001).
     *
     * <p>COALESCE handles the empty-set case (DERIVED-AGG-EMPTY-001): returns 0 not null.
     */
    @Query("""
        SELECT COALESCE(AVG(CAST(r.stars AS java.math.BigDecimal)), 0)
        FROM Review r
        WHERE r.productId = :productId AND r.status = :status
        """)
    BigDecimal avgStarsByProductIdAndStatus(@Param("productId") UUID productId,
                                            @Param("status") ReviewStatus status);

    long countByProductIdAndStatus(UUID productId, ReviewStatus status);
}
