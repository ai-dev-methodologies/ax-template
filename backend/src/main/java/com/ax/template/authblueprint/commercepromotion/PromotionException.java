package com.ax.template.authblueprint.commercepromotion;

import org.springframework.http.HttpStatus;

/** Domain exception for the commerce-promotion engine. status + RFC 9457 type + machine-readable code. */
public class PromotionException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private PromotionException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    /** PROMO-MAXUSES-001 — the offer has reached its global max-uses cap. */
    public static PromotionException maxUsesExceeded() {
        return new PromotionException(HttpStatus.CONFLICT,
            "urn:problem:promo-max-uses-exceeded", "PROMO_MAX_USES_EXCEEDED",
            "This offer has reached its maximum global use limit");
    }

    /** PROMO-MAXUSES-001 — the customer has reached per-customer max-uses for this offer. */
    public static PromotionException maxUsesPerCustomerExceeded() {
        return new PromotionException(HttpStatus.CONFLICT,
            "urn:problem:promo-max-uses-per-customer-exceeded", "PROMO_MAX_USES_PER_CUSTOMER_EXCEEDED",
            "You have reached the maximum uses of this offer");
    }

    /** PROMO-MAXUSES-001 — idempotent: same (offer, order_ref) already redeemed. */
    public static PromotionException duplicateRedemption() {
        return new PromotionException(HttpStatus.CONFLICT,
            "urn:problem:promo-duplicate-redemption", "PROMO_DUPLICATE_REDEMPTION",
            "This offer has already been redeemed for this order (duplicate redemption)");
    }

    public static PromotionException offerNotFound(String id) {
        return new PromotionException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "PROMO_OFFER_NOT_FOUND",
            "Offer not found: " + id);
    }

    public static PromotionException codeNotFound(String code) {
        return new PromotionException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "PROMO_OFFER_NOT_FOUND",
            "Offer code not found: " + code);
    }

    /** PROMO-CLAMP-001 — invalid offer configuration (e.g. discount_value out of range). */
    public static PromotionException invalidOffer(String detail) {
        return new PromotionException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:promo-invalid-offer", "PROMO_INVALID_OFFER", detail);
    }
}
