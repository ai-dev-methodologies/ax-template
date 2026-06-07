package com.ax.template.authblueprint.transformation;

import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

/**
 * Domain exception for transformation-conservation. Carries HTTP status, RFC 9457 problem {@code type},
 * and a machine-readable {@code code}. NOTE: a non-zero residual is NOT an error (it is the whole point);
 * the rejections are a failed conservation, an unclassified residual, mixed units, or a malformed qty.
 */
public class TransformationException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private TransformationException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static TransformationException notFound() {
        return new TransformationException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Transformation not found");
    }

    /** XFORM-ACCOUNTED-LOSS-001 — Σ(input) != Σ(good) + Σ(residual). */
    public static TransformationException notConserved(BigDecimal in, BigDecimal good, BigDecimal residual) {
        return new TransformationException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:transformation-not-conserved", "XFORM_NOT_CONSERVED",
            "Not conserved: input=" + in + " good=" + good + " residual=" + residual
                + " (good+residual=" + good.add(residual) + ")");
    }

    /** XFORM-RESIDUAL-CLASSIFIED-001 — a residual quantity with no governed disposition. */
    public static TransformationException unclassifiedResidual() {
        return new TransformationException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:transformation-unclassified-residual", "XFORM_UNCLASSIFIED_RESIDUAL",
            "Every residual leg must carry a governed disposition (no miscellaneous bucket)");
    }

    /** XFORM-DIMENSION-001 — legs mix physical units with no pinned conversion. */
    public static TransformationException mixedUnit() {
        return new TransformationException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:transformation-mixed-unit", "XFORM_MIXED_UNIT",
            "All legs of one transformation must share one base unit (or a pinned conversion)");
    }

    /** A disposition supplied on a non-RESIDUAL leg (disposition is present IFF the leg is residual). */
    public static TransformationException dispositionNotAllowed() {
        return new TransformationException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:transformation-disposition-not-allowed", "XFORM_DISPOSITION_NOT_ALLOWED",
            "A disposition may only be set on a RESIDUAL leg");
    }

    /** A negative/missing quantity, over-scale qty, empty/too-many legs, or no output leg. */
    public static TransformationException invalidAmount() {
        return new TransformationException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:transformation-invalid-amount", "XFORM_INVALID_AMOUNT",
            "Quantities must be non-negative and a transformation must have at least one input and output leg");
    }
}
