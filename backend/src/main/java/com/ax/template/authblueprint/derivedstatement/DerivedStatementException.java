package com.ax.template.authblueprint.derivedstatement;

import org.springframework.http.HttpStatus;

/** Domain exception for derived-statement-l0. status + RFC 9457 type + machine-readable code. */
public class DerivedStatementException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private DerivedStatementException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static DerivedStatementException notFound() {
        return new DerivedStatementException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Statement not found");
    }

    /** STMT-DERIVE-001 — a generate request with no basis line items has no content to hash. */
    public static DerivedStatementException emptyBasis() {
        return new DerivedStatementException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:statement-empty-basis", "STMT_EMPTY_BASIS",
            "At least one basis line item is required to generate a statement");
    }
}
