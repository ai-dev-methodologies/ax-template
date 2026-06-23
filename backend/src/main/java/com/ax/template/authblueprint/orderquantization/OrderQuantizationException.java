package com.ax.template.authblueprint.orderquantization;

import org.springframework.http.HttpStatus;

/** Domain exception for order-multiple-quantization. status + RFC 9457 type + machine-readable code. */
public class OrderQuantizationException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private OrderQuantizationException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static OrderQuantizationException notFound() {
        return new OrderQuantizationException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Order quantization not found");
    }

    /** ORDERQUANT-CONSTRAINT-001 — MOQ and the order multiple must be positive; required non-negative. */
    public static OrderQuantizationException invalidConstraint(String detail) {
        return new OrderQuantizationException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:orderquant-invalid-constraint", "ORDERQUANT_INVALID_CONSTRAINT", detail);
    }
}
