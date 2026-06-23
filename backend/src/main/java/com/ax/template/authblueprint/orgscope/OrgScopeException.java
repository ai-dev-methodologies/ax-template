package com.ax.template.authblueprint.orgscope;

import org.springframework.http.HttpStatus;

/** Domain exception for containment-scope-authz. status + RFC 9457 type + machine-readable code. */
public class OrgScopeException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private OrgScopeException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static OrgScopeException nodeNotFound() {
        return new OrgScopeException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Org unit not found");
    }

    /** ORGSCOPE-CONTAINMENT-001 — the caller holds no grant at the node or any ancestor of it. */
    public static OrgScopeException outOfScope() {
        return new OrgScopeException(HttpStatus.FORBIDDEN,
            "urn:problem:orgscope-out-of-scope", "OUT_OF_SCOPE",
            "Caller holds no grant at the target node or any of its ancestors for the required role");
    }
}
