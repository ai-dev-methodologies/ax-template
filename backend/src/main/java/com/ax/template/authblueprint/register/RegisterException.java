package com.ax.template.authblueprint.register;

import org.springframework.http.HttpStatus;

/**
 * Domain exception for monotone-register. status + RFC 9457 type + machine-readable code.
 */
public class RegisterException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private RegisterException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static RegisterException notFound() {
        return new RegisterException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Register not found");
    }

    public static RegisterException duplicateScope() {
        return new RegisterException(HttpStatus.CONFLICT,
            "urn:problem:register-duplicate-scope", "REGISTER_DUPLICATE_SCOPE",
            "A register with this scope key already exists");
    }

    /** REG-MONOTONE-001 — a NORMAL read below the anchor (use a governed ROLLOVER/EXCHANGE instead). */
    public static RegisterException notMonotone() {
        return new RegisterException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:register-not-monotone", "REGISTER_NOT_MONOTONE",
            "A NORMAL read must be >= the register anchor; a decrease requires a governed ROLLOVER/EXCHANGE");
    }

    /** REG-MONOTONE/ROLLOVER/EXCHANGE-001 — read outside [0, modulus), or a ROLLOVER/EXCHANGE not below the anchor. */
    public static RegisterException invalidReading() {
        return new RegisterException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:register-invalid-reading", "REGISTER_INVALID_READING",
            "Reading must be in [0, modulus); a ROLLOVER or EXCHANGE read must be below the anchor "
                + "(a baseline reset is downward; use a NORMAL read to bill an increase)");
    }

    /** REG-ROLLOVER-001 / REG-EXCHANGE-001 — the governed exceptions require a non-blank reason. */
    public static RegisterException reasonRequired() {
        return new RegisterException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:register-reason-required", "REGISTER_REASON_REQUIRED",
            "A ROLLOVER or EXCHANGE read requires a non-blank reason");
    }
}
