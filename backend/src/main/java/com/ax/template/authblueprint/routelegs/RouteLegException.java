package com.ax.template.authblueprint.routelegs;

import org.springframework.http.HttpStatus;

/** Domain exception for route-leg-contiguity. status + RFC 9457 type + machine code. */
public class RouteLegException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private RouteLegException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static RouteLegException notFound() {
        return new RouteLegException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Route (or leg) not found");
    }

    /** LEG-SEQUENCE-001 — a leg's origin/dest does not match the neighbor it would sit beside. */
    public static RouteLegException sequenceViolation() {
        return new RouteLegException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:route-leg-sequence-violation", "ROUTE_LEG_SEQUENCE_VIOLATION",
            "This leg's origin/destination does not match the adjoining leg's destination/origin");
    }

    /** LEG-GAP-001 — an out-of-range position, or a reorder whose new sequence is not contiguous. */
    public static RouteLegException gapViolation() {
        return new RouteLegException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:route-leg-gap-violation", "ROUTE_LEG_GAP_VIOLATION",
            "This mutation would leave a gap, overlap, or non-contiguous ordinal sequence");
    }

    /** LEG-MUTATE-001 — the loser of a concurrent mutation on the same route (CWE-362). */
    public static RouteLegException concurrentModification() {
        return new RouteLegException(HttpStatus.CONFLICT,
            "urn:problem:route-concurrent-modification", "ROUTE_CONCURRENT_MODIFICATION",
            "This route was concurrently modified — reload and retry");
    }
}
