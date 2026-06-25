package com.ax.template.authblueprint.commercepromotion;

import com.ax.template.authblueprint.common.AggregateMember;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * APPEND-ONLY redemption record — every column {@code updatable=false}, no public setter.
 * The UNIQUE(offer_id, order_ref) constraint is the ATOMIC MAX-USES BACKSTOP
 * (PROMO-MAXUSES-001): even under concurrent writes, only one redemption per offer per order
 * can exist. This directly strengthens Broadleaf's offer engine, which has a TOCTOU
 * vulnerability because it only checks max_uses count without a unique constraint.
 * The duplicate-redemption path surfaces as a {@link org.springframework.dao.DataIntegrityViolationException}
 * which {@link PromotionService} translates to a 409 PROMO_IDEMPOTENT_REDEMPTION.
 */
@AggregateMember(root = PromoOffer.class)
@Entity(name = "PromoOfferRedemption")
@Table(name = "promo_redemptions", uniqueConstraints = {
    @UniqueConstraint(name = "uq_promo_redemption_offer_order", columnNames = {"offer_id", "order_ref"})
})
public class PromoOfferRedemption {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "offer_id", nullable = false, updatable = false)
    private UUID offerId;

    @Column(name = "customer_id", nullable = false, updatable = false, length = 200)
    private String customerId;

    @Column(name = "order_ref", nullable = false, updatable = false, length = 200)
    private String orderRef;

    @Column(name = "redeemed_at", nullable = false, updatable = false)
    private Instant redeemedAt;

    protected PromoOfferRedemption() {}

    public PromoOfferRedemption(UUID id, UUID offerId, String customerId, String orderRef, Instant redeemedAt) {
        this.id = id;
        this.offerId = offerId;
        this.customerId = customerId;
        this.orderRef = orderRef;
        this.redeemedAt = redeemedAt;
    }

    public UUID getId() { return id; }
    public UUID getOfferId() { return offerId; }
    public String getCustomerId() { return customerId; }
    public String getOrderRef() { return orderRef; }
    public Instant getRedeemedAt() { return redeemedAt; }
}
