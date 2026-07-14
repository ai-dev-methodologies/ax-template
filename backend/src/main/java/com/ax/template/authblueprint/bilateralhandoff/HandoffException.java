package com.ax.template.authblueprint.bilateralhandoff;

import org.springframework.http.HttpStatus;

/** Domain exception for bilateral-handoff. status + RFC 9457 type + machine code. */
public class HandoffException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private HandoffException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static HandoffException notFound() {
        return new HandoffException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Handoff not found");
    }

    /** BHO-BIND-001 — the caller is neither the releasor nor the receiver. */
    public static HandoffException notAParty() {
        return new HandoffException(HttpStatus.FORBIDDEN,
            "urn:problem:handoff-not-a-party", "HANDOFF_NOT_A_PARTY",
            "The caller is neither the releasor nor the receiver named on this handoff");
    }

    /** BHO-VOID-001 — a late confirm/decline on an already-VOIDED handoff. */
    public static HandoffException voided() {
        return new HandoffException(HttpStatus.CONFLICT,
            "urn:problem:handoff-voided", "HANDOFF_VOIDED",
            "This handoff was voided — it cannot be confirmed or declined");
    }

    /** A decline (or any action requiring PROPOSED) attempted on a non-PROPOSED, non-VOIDED handoff. */
    public static HandoffException notOpen(String currentStatus) {
        return new HandoffException(HttpStatus.CONFLICT,
            "urn:problem:handoff-not-open", "HANDOFF_NOT_OPEN",
            "This handoff is no longer open (status " + currentStatus + ")");
    }
}
