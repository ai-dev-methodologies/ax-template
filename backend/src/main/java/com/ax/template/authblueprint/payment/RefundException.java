package com.ax.template.authblueprint.payment;

/**
 * Refund-specific failures (window expired, over-refund). Carries a stable RFC 7807
 * type URI and HTTP status so the exception handler can render without re-deriving.
 */
public class RefundException extends RuntimeException {

    private final int status;
    private final String typeUri;

    public RefundException(int status, String typeUri, String message) {
        super(message);
        this.status = status;
        this.typeUri = typeUri;
    }

    public int getStatus() { return status; }
    public String getTypeUri() { return typeUri; }
}
