package com.ax.template.authblueprint.dispatch;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OfferRepository extends JpaRepository<Offer, UUID> {

    /** OFFER-FSM-001 — the (at most one) outstanding PENDING offer for a request, if any. */
    Optional<Offer> findFirstByRequestIdAndStatus(UUID requestId, OfferStatus status);

    /** Next cascade ordinal source (OFFER-CASCADE-004). */
    long countByRequestId(UUID requestId);

    /** AVAIL-SWEEP-001 — due PENDING offers for the timeout sweep. Bounded batch (Pageable). */
    @Query("SELECT o.id FROM Offer o WHERE o.status = :pending AND o.expiresAt < :now ORDER BY o.expiresAt ASC")
    List<UUID> findDueOfferIds(@Param("pending") OfferStatus pending,
                               @Param("now") Instant now,
                               Pageable pageable);

    /** Providers already tried for this request (re-offer skips them). Bounded (Pageable). */
    @Query("SELECT o.providerId FROM Offer o WHERE o.requestId = :rid")
    List<UUID> findProviderIdsByRequestId(@Param("rid") UUID rid, Pageable pageable);
}
