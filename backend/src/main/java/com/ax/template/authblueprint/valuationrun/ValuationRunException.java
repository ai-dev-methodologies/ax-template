package com.ax.template.authblueprint.valuationrun;

import org.springframework.http.HttpStatus;

/** Domain exception for valuation-run-projection. status + RFC 9457 type + machine-readable code. */
public class ValuationRunException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private ValuationRunException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static ValuationRunException notFound() {
        return new ValuationRunException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Valuation subject or run not found");
    }

    /** VALRUN-ASOF-001 — no run exists with an as-of ≤ the queried time. */
    public static ValuationRunException noRunAsOf() {
        return new ValuationRunException(HttpStatus.NOT_FOUND,
            "urn:problem:valuation-no-run-as-of", "VALRUN_NO_RUN_AS_OF",
            "No valuation run exists with an as-of at or before the requested time");
    }

    /** VALRUN-FANOUT-001 — the per-position outputs do not sum to the run total. */
    public static ValuationRunException fanOutNotConserved() {
        return new ValuationRunException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:valuation-fan-out-not-conserved", "VALRUN_FANOUT_NOT_CONSERVED",
            "The per-position outputs do not sum to the run total — a partial fan-out is unrepresentable");
    }

    /** VALRUN-FANOUT-001 — a fan-out must declare at least one position. */
    public static ValuationRunException emptyFanOut() {
        return new ValuationRunException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:valuation-empty-fan-out", "VALRUN_EMPTY_FAN_OUT",
            "A valuation run must fan out to at least one position");
    }

    /** VALRUN-REBASE-001 — a rebase must start from the current head version. */
    public static ValuationRunException notCurrent() {
        return new ValuationRunException(HttpStatus.CONFLICT,
            "urn:problem:valuation-not-current", "VALRUN_NOT_CURRENT",
            "Rebase must start from the current head version — the run chain stays linear");
    }

    /** VALRUN-CONCURRENT-001 — the uq(subject_id, run_version) backstop loser of a residual race. */
    public static ValuationRunException versionConflict() {
        return new ValuationRunException(HttpStatus.CONFLICT,
            "urn:problem:valuation-version-conflict", "VALRUN_VERSION_CONFLICT",
            "Another recompute or rebase created this version first — exactly one new version wins");
    }

    /** VALRUN-FALLBACK-001 — none of the caller's configured sources has a qualifying as-of point. */
    public static ValuationRunException noQualifyingSource() {
        return new ValuationRunException(HttpStatus.NOT_FOUND,
            "urn:problem:valuation-no-qualifying-source", "VALRUN_NO_QUALIFYING_SOURCE",
            "No configured source has a run with an as-of at or before the requested time");
    }
}
