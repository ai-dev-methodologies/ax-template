package com.ax.template.authblueprint.provisionalattestation;

import org.springframework.http.HttpStatus;

/** Domain exception for provisional-attestation-l0. status + RFC 9457 type + machine-readable code. */
public class ProvisionalAttestationException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private ProvisionalAttestationException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static ProvisionalAttestationException notFound() {
        return new ProvisionalAttestationException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Provisional record not found");
    }

    /** PATT-LIFECYCLE-001 — an attest/edit call on an already-ATTESTED (terminal) record. */
    public static ProvisionalAttestationException illegalTransition() {
        return new ProvisionalAttestationException(HttpStatus.CONFLICT,
            "urn:problem:provisional-illegal-transition", "PATT_ILLEGAL_TRANSITION",
            "ATTESTED is terminal — no further transition or edit is legal");
    }

    /** PATT-DISTINCT-002 — the attestor MUST differ from the author. */
    public static ProvisionalAttestationException attestorMustDifferFromAuthor() {
        return new ProvisionalAttestationException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:provisional-self-attestation", "PATT_SELF_ATTESTATION",
            "The attestor must differ from the author — self-attestation is rejected");
    }

    /** PATT-FREEZE-003 — only the author may edit a PROVISIONAL record. */
    public static ProvisionalAttestationException editNotAuthor() {
        return new ProvisionalAttestationException(HttpStatus.FORBIDDEN,
            "urn:problem:provisional-edit-not-author", "PATT_EDIT_NOT_AUTHOR",
            "Only the authoring principal may edit a PROVISIONAL record");
    }

    /** PATT-FREEZE-003 — integrity verification is only meaningful once attested. */
    public static ProvisionalAttestationException notYetAttested() {
        return new ProvisionalAttestationException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:provisional-not-yet-attested", "PATT_NOT_YET_ATTESTED",
            "The record has not been attested — there is no attested content-hash to verify against");
    }
}
