package com.ax.template.authblueprint.commercepromotion;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Root repository for PromoOffer aggregate. Member entities (PromoOfferCode, PromoOfferRedemption)
 * are read via JPQL through-root queries here — they own no repositories (AX-DDD-MEMBER-REPO).
 * JPQL entity names match the @Entity(name=...) declarations.
 */
public interface PromoOfferRepository extends JpaRepository<PromoOffer, UUID> {

    /** PROMO-MAXUSES-001 — acquire the offer row under PESSIMISTIC_WRITE before redemption. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM PromoOffer o WHERE o.id = :id")
    Optional<PromoOffer> findByIdForUpdate(@Param("id") UUID id);

    // ── through-root member reads ──────────────────────────────────────────────────

    @Query("SELECT c FROM PromoOfferCode c WHERE c.code = :code")
    Optional<PromoOfferCode> findCodeByCode(@Param("code") String code);

    @Query("SELECT c FROM PromoOfferCode c WHERE c.offerId = :offerId")
    List<PromoOfferCode> findCodesByOfferId(@Param("offerId") UUID offerId);

    @Query("SELECT COUNT(r) FROM PromoOfferRedemption r WHERE r.offerId = :offerId")
    long countRedemptionsByOfferId(@Param("offerId") UUID offerId);

    @Query("SELECT COUNT(r) FROM PromoOfferRedemption r WHERE r.offerId = :offerId AND r.customerId = :customerId")
    long countRedemptionsByOfferIdAndCustomerId(@Param("offerId") UUID offerId, @Param("customerId") String customerId);

    @Query("SELECT r FROM PromoOfferRedemption r WHERE r.offerId = :offerId AND r.orderRef = :orderRef")
    Optional<PromoOfferRedemption> findRedemption(@Param("offerId") UUID offerId, @Param("orderRef") String orderRef);
}
