package com.ax.template.authblueprint.commercepromotion;

import com.ax.template.authblueprint.common.AggregateMember;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

/**
 * A redeemable coupon code linked to a PromoOffer. The {@code code} and {@code offerId} are
 * fully immutable once created (updatable=false) — codes are never reassigned.
 * UNIQUE(code) enforces that one code maps to exactly one offer globally.
 * max_uses = 0 means unlimited redemptions via this code.
 */
@AggregateMember(root = PromoOffer.class)
@Entity(name = "PromoOfferCode")
@Table(name = "promo_offer_codes", uniqueConstraints = {
    @UniqueConstraint(name = "uq_promo_offer_code", columnNames = {"code"})
})
public class PromoOfferCode {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "offer_id", nullable = false, updatable = false)
    private UUID offerId;

    @Column(name = "code", nullable = false, updatable = false, length = 100)
    private String code;

    /** 0 = unlimited uses for this code. */
    @Column(name = "max_uses", nullable = false, updatable = false)
    private long maxUses;

    protected PromoOfferCode() {}

    public PromoOfferCode(UUID id, UUID offerId, String code, long maxUses) {
        this.id = id;
        this.offerId = offerId;
        this.code = code;
        this.maxUses = maxUses;
    }

    public UUID getId() { return id; }
    public UUID getOfferId() { return offerId; }
    public String getCode() { return code; }
    public long getMaxUses() { return maxUses; }
}
