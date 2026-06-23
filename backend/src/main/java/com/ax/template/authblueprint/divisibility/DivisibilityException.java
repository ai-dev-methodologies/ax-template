package com.ax.template.authblueprint.divisibility;

import org.springframework.http.HttpStatus;

/** Domain exception for material-divisibility-constraint. status + RFC 9457 type + machine-readable code. */
public class DivisibilityException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private DivisibilityException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static DivisibilityException notFound() {
        return new DivisibilityException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "No divisibility policy declared for that material");
    }

    /** DIV-INTEGRAL-001 — a fractional quantity against an INTEGER_ONLY material is rejected, NEVER rounded. */
    public static DivisibilityException nonIntegral(String materialRef, String submittedQuantity) {
        return new DivisibilityException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:divisibility-non-integral-quantity", "NON_INTEGRAL_QUANTITY",
            "Material '" + materialRef + "' is INTEGER_ONLY — quantity " + submittedQuantity
                + " has a non-zero fractional part and is rejected (it is NOT rounded to a whole unit)");
    }

    /** DIV-PRECISION-001 — a quantity above a FRACTIONAL material's max scale is rejected, NEVER truncated. */
    public static DivisibilityException excessPrecision(String materialRef, String submittedQuantity, int maxScale) {
        return new DivisibilityException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:divisibility-excess-precision", "EXCESS_PRECISION",
            "Material '" + materialRef + "' admits at most " + maxScale + " decimal place(s) — quantity "
                + submittedQuantity + " carries more precision and is rejected (it is NOT truncated)");
    }

    /** DIV-PRECISION-001 — a FRACTIONAL policy must declare a non-negative maximum decimal scale. */
    public static DivisibilityException invalidMaxScale(int maxScale) {
        return new DivisibilityException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:divisibility-invalid-max-scale", "DIVISIBILITY_INVALID_MAX_SCALE",
            "A FRACTIONAL policy requires a non-negative maxScale (got " + maxScale + ")");
    }
}
