package com.ax.template.authblueprint.trueup;

import org.springframework.http.HttpStatus;

import java.util.List;

/** Domain exception for remeasurement-trueup. status + RFC 9457 type + machine-readable code. */
public class TrueUpException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private TrueUpException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static TrueUpException notFound() {
        return new TrueUpException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Period, reading, or run not found");
    }

    /** TUP-SEALED-001 — a sealed period accepts nothing. */
    public static TrueUpException sealed() {
        return new TrueUpException(HttpStatus.CONFLICT,
            "urn:problem:trueup-period-sealed", "TRUEUP_PERIOD_SEALED",
            "The period is sealed — readings, recomputes, and true-ups are all refused");
    }

    /** TUP-SEALED-001 — the lifecycle is one-way OPEN→CLOSED→SEALED. */
    public static TrueUpException invalidState() {
        return new TrueUpException(HttpStatus.CONFLICT,
            "urn:problem:trueup-invalid-state", "TRUEUP_INVALID_STATE",
            "The period lifecycle walks OPEN→CLOSED→SEALED one-way only");
    }

    /** TUP-SUPERSEDE-001 — estimates fill gaps; they never overwrite facts. */
    public static TrueUpException downgrade() {
        return new TrueUpException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:trueup-downgrade", "TRUEUP_DOWNGRADE",
            "An ACTUAL reading cannot be superseded by an ESTIMATED one");
    }

    /** TUP-GRID-001 — a run never silently computes over a partial grid. */
    public static TrueUpException gridIncomplete(List<Integer> missingSlots) {
        return new TrueUpException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:trueup-grid-incomplete", "TRUEUP_GRID_INCOMPLETE",
            "The grid is incomplete — missing slot(s) " + missingSlots
                + "; record readings or estimate-missing explicitly");
    }

    /** TUP-SEALED-001 — closing fixes the run-of-record; there must be one. */
    public static TrueUpException noRun() {
        return new TrueUpException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:trueup-no-run", "TRUEUP_NO_RUN",
            "The period has no settlement run — recompute before closing");
    }

    /** TUP-DELTA-001 — a closed period's correction needs an open period to land in. */
    public static TrueUpException targetRequired() {
        return new TrueUpException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:trueup-target-required", "TRUEUP_TARGET_REQUIRED",
            "Recomputing a closed period requires a target period for the true-up posting");
    }

    /** TUP-DELTA-001 — true-ups land in OPEN periods only. */
    public static TrueUpException targetNotOpen() {
        return new TrueUpException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:trueup-target-not-open", "TRUEUP_TARGET_NOT_OPEN",
            "The true-up target period is not OPEN");
    }

    /** TUP-GRID-001 — a reading lands inside the declared grid. */
    public static TrueUpException slotRange(int gridSlots) {
        return new TrueUpException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:trueup-slot-range", "TRUEUP_SLOT_RANGE",
            "slotIndex must be within the declared grid [0, " + gridSlots + ")");
    }

    /** TUP-SUPERSEDE-001 — ESTIMATED requires its method recorded; ACTUAL forbids one. */
    public static TrueUpException invalidMethod() {
        return new TrueUpException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:trueup-invalid-method", "TRUEUP_INVALID_METHOD",
            "An ESTIMATED reading requires an estimation method; an ACTUAL reading must not carry one");
    }
}
