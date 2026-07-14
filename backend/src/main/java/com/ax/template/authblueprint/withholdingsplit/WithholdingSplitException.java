package com.ax.template.authblueprint.withholdingsplit;

import org.springframework.http.HttpStatus;

/** Domain exception for withholding-split-l0. status + RFC 9457 type + machine-readable code. */
public class WithholdingSplitException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private WithholdingSplitException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static WithholdingSplitException invalidGrossAmount() {
        return new WithholdingSplitException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:withholding-split-invalid-gross", "WHT_INVALID_GROSS_AMOUNT",
            "grossAmount must not be zero");
    }

    public static WithholdingSplitException invalidRate() {
        return new WithholdingSplitException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:withholding-split-invalid-rate", "WHT_INVALID_RATE",
            "rate must be in [0, 1)");
    }

    public static WithholdingSplitException postingNotFound() {
        return new WithholdingSplitException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "WHT_POSTING_NOT_FOUND", "No withholding posting found for that id");
    }

    /** WHT-SPLIT-001 backstop — should be unreachable given by-construction net derivation; the
     *  pre-commit re-sum caught a residual imbalance. */
    public static WithholdingSplitException unbalancedSplit(String residual) {
        return new WithholdingSplitException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:withholding-split-unbalanced", "WHT_UNBALANCED_SPLIT",
            "Posted legs do not sum to gross (residual " + residual + ")");
    }

    public static WithholdingSplitException invalidPeriod() {
        return new WithholdingSplitException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:withholding-split-invalid-period", "WHT_INVALID_PERIOD",
            "period must be formatted YYYY-MM");
    }

    public static WithholdingSplitException remittanceNotFound() {
        return new WithholdingSplitException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "WHT_REMITTANCE_NOT_FOUND", "No remittance run found for that period");
    }
}
