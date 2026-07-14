package com.ax.template.authblueprint.rangeownership;

import org.springframework.http.HttpStatus;

/** Domain exception for range-ownership. status + RFC 9457 type + machine-readable code. */
public class RangeOwnershipException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private RangeOwnershipException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static RangeOwnershipException notFound() {
        return new RangeOwnershipException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Range-ownership resource not found");
    }

    /** RNG-NONOVERLAP-002 — a registered range block overlaps an existing one. */
    public static RangeOwnershipException blockOverlap() {
        return new RangeOwnershipException(HttpStatus.CONFLICT,
            "urn:problem:range-block-overlap", "RANGE_BLOCK_OVERLAP",
            "The range block overlaps an existing block");
    }

    /** RNG-CONTAINMENT/PORT-001/003 — the identifier is not contained by any block the owner owns. */
    public static RangeOwnershipException notOwned() {
        return new RangeOwnershipException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:range-not-owned", "RANGE_NOT_OWNED",
            "The identifier does not fall inside any range block owned by that owner");
    }

    /** RNG-CONTAINMENT-001 — the identifier already has an active assignment. */
    public static RangeOwnershipException alreadyAssigned() {
        return new RangeOwnershipException(HttpStatus.CONFLICT,
            "urn:problem:identifier-already-assigned", "IDENTIFIER_ALREADY_ASSIGNED",
            "The identifier already has an assignment — use port to reassign it");
    }

    /** A blank/invalid range block submitted as a domain 422 (not a Bean-Validation 400). */
    public static RangeOwnershipException invalidBlock(String detail) {
        return new RangeOwnershipException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:range-block-invalid", "RANGE_BLOCK_INVALID", detail);
    }
}
