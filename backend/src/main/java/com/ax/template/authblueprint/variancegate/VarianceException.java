package com.ax.template.authblueprint.variancegate;

import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

/** Domain exception for variance-tolerance-band. status + RFC 9457 type + machine-readable code. */
public class VarianceException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;
    private final transient BigDecimal variance;
    private final transient BigDecimal lowerTolerance;
    private final transient BigDecimal upperTolerance;

    private VarianceException(HttpStatus status, String type, String code, String message,
                              BigDecimal variance, BigDecimal lowerTolerance, BigDecimal upperTolerance) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
        this.variance = variance;
        this.lowerTolerance = lowerTolerance;
        this.upperTolerance = upperTolerance;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }
    public BigDecimal variance() { return variance; }
    public BigDecimal lowerTolerance() { return lowerTolerance; }
    public BigDecimal upperTolerance() { return upperTolerance; }

    public static VarianceException notFound() {
        return new VarianceException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Variance appraisal not found",
            null, null, null);
    }

    /** VG-BLOCK-001 — a dependent operation is blocked while the appraisal is OUT_OF_TOLERANCE and
     *  undisposed; the variance + band that governed the verdict are named in the problem body. */
    public static VarianceException outOfTolerance(BigDecimal variance, BigDecimal lower, BigDecimal upper) {
        return new VarianceException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:variance-out-of-tolerance", "VARIANCE_OUT_OF_TOLERANCE",
            "Variance " + variance.toPlainString() + " is out of the tolerance band [-"
                + lower.toPlainString() + ", +" + upper.toPlainString()
                + "] — record a disposition (who/when/reason) to proceed",
            variance, lower, upper);
    }

    /** VG-DISPOSE-001 — only an OUT_OF_TOLERANCE appraisal can be disposed; nothing to override. */
    public static VarianceException nothingToDispose() {
        return new VarianceException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:variance-nothing-to-dispose", "VARIANCE_NOTHING_TO_DISPOSE",
            "The appraisal is within tolerance — there is no breach to dispose",
            null, null, null);
    }

    /** VG-DISPOSE-001 — a disposition MUST record a non-blank reason. */
    public static VarianceException blankReason() {
        return new VarianceException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:variance-blank-reason", "VARIANCE_BLANK_REASON",
            "A disposition requires a non-blank reason — silent acceptance is not permitted",
            null, null, null);
    }

    /** VG-CONCURRENT-001 — exactly one disposition per appraisal (the uq backstop loser). */
    public static VarianceException alreadyDisposed() {
        return new VarianceException(HttpStatus.CONFLICT,
            "urn:problem:variance-already-disposed", "VARIANCE_ALREADY_DISPOSED",
            "That appraisal already has a disposition on record — exactly one is permitted",
            null, null, null);
    }

    /** VG-GATE-001 — tolerance magnitudes are non-negative; a negative band is unrepresentable. */
    public static VarianceException invalidBand() {
        return new VarianceException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:variance-invalid-band", "VARIANCE_INVALID_BAND",
            "lowerTolerance and upperTolerance must each be non-negative magnitudes",
            null, null, null);
    }
}
