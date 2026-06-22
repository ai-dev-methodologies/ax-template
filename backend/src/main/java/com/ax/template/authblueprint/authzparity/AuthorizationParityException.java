package com.ax.template.authblueprint.authzparity;

import org.springframework.http.HttpStatus;

/** Domain exception for authorization-parity. status + RFC 9457 type + machine-readable code. */
public class AuthorizationParityException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private AuthorizationParityException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static AuthorizationParityException notFound() {
        return new AuthorizationParityException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Action not found");
    }

    /** AUTHZPARITY-EXEC-001 — the execution parameters do not re-hash to the authorized envelope. */
    public static AuthorizationParityException parityMismatch() {
        return new AuthorizationParityException(HttpStatus.CONFLICT,
            "urn:problem:authzparity-parity-mismatch", "PARITY_MISMATCH",
            "Execution parameters do not match the authorized envelope — execution blocked");
    }

    /** AUTHZPARITY-EXEC-001/CONCURRENT-001 — an action executes exactly once. */
    public static AuthorizationParityException alreadyExecuted() {
        return new AuthorizationParityException(HttpStatus.CONFLICT,
            "urn:problem:authzparity-already-executed", "ALREADY_EXECUTED",
            "The action is already executed — an action executes once");
    }

    /** AUTHZPARITY-FOUREYES-001 — the requester cannot sign off their own action. */
    public static AuthorizationParityException selfSignoff() {
        return new AuthorizationParityException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:authzparity-self-signoff", "SELF_SIGNOFF",
            "The requester cannot sign off their own action (two-person rule)");
    }

    /** AUTHZPARITY-FOUREYES-001 — a distinct approver cannot sign off twice. */
    public static AuthorizationParityException duplicateSignoff() {
        return new AuthorizationParityException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:authzparity-duplicate-signoff", "DUPLICATE_SIGNOFF",
            "This approver has already signed off — two DISTINCT approvers are required");
    }

    /** AUTHZPARITY-FOUREYES-001 — a high-value action needs two distinct signoffs before execute. */
    public static AuthorizationParityException insufficientSignoffs() {
        return new AuthorizationParityException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:authzparity-insufficient-signoffs", "INSUFFICIENT_SIGNOFFS",
            "A high-value action requires two distinct approver signoffs before execution");
    }

    /** AUTHZPARITY-GATES-001 — a declared mandatory companion gate is not yet satisfied. */
    public static AuthorizationParityException missingCompanionGate(String gateKey) {
        return new AuthorizationParityException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:authzparity-missing-companion-gate", "MISSING_COMPANION_GATE",
            "A mandatory companion gate is not satisfied: " + gateKey);
    }

    /** AUTHZPARITY-GATES-001 — only declared gate keys can be satisfied. */
    public static AuthorizationParityException unknownGate(String gateKey) {
        return new AuthorizationParityException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:authzparity-unknown-gate", "UNKNOWN_GATE",
            "This action did not declare the gate: " + gateKey);
    }

    /** AUTHZPARITY-GATES-001 — a gate is satisfied once; re-satisfaction is rejected. */
    public static AuthorizationParityException gateAlreadySatisfied(String gateKey) {
        return new AuthorizationParityException(HttpStatus.CONFLICT,
            "urn:problem:authzparity-gate-already-satisfied", "GATE_ALREADY_SATISFIED",
            "This gate is already satisfied: " + gateKey);
    }
}
