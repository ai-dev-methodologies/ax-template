package com.ax.template.authblueprint.ratingsummary;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RatingSummaryRepository extends JpaRepository<RatingSummary, UUID> {

    /** Pessimistic write lock to prevent concurrent recompute races. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM RatingSummary s WHERE s.productId = :productId")
    Optional<RatingSummary> findByIdForUpdate(@Param("productId") UUID productId);
}
