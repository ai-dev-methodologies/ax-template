package com.ax.template.authblueprint.commerceorder;

import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.Locale;

/**
 * Domain exception for commerceorder — carries a stable {@code code} for client branching.
 *
 * <p>Codes:
 * <ul>
 *   <li>{@code ORDER_NOT_EDITABLE} 409 — add/update/remove while not IN_PROCESS</li>
 *   <li>{@code ORDER_INVALID_TRANSITION} 409 — FSM illegal state change</li>
 *   <li>{@code ORDER_FULFILLMENT_NOT_CONSERVED} 422 — Σ(group-item qty) != item.quantity</li>
 *   <li>{@code ORDER_NOT_FOUND} 404 — IDOR-safe not-found (user-scoped)</li>
 *   <li>{@code ORDER_ITEM_NOT_FOUND} 404 — item id not in this order</li>
 *   <li>{@code ORDER_INVALID_INPUT} 422 — bad input (qty ≤ 0, etc.)</li>
 * </ul>
 */
public class CommerceOrderException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    public CommerceOrderException(String code, int status, String detail) {
        super(detail);
        this.code = code;
        this.httpStatus = HttpStatus.valueOf(status);
    }

    public String getCode() { return code; }
    public HttpStatus getHttpStatus() { return httpStatus; }

    public URI type() {
        return URI.create("urn:problem:commerce-order:" + code.toLowerCase(Locale.ROOT).replace('_', '-'));
    }
}
