package com.ax.template.authblueprint.uomconversion;

import org.springframework.http.HttpStatus;

/** Domain exception for dimensional-uom-conversion. status + RFC 9457 type + machine-readable code. */
public class UomConversionException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private UomConversionException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static UomConversionException notFound() {
        return new UomConversionException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Material or conversion not found");
    }

    /** UOMCONV-COMPAT-001 — an unknown unit code cannot be converted. */
    public static UomConversionException unknownUnit(String code) {
        return new UomConversionException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:uom-unknown-unit", "UNKNOWN_UNIT",
            "Unknown unit code: " + code);
    }

    /** UOMCONV-COMPAT-001 — a cross-dimension conversion with no recorded bridging material property. */
    public static UomConversionException incompatibleDimensions(Dimension from, Dimension to) {
        return new UomConversionException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:uom-incompatible-dimensions", "INCOMPATIBLE_DIMENSIONS",
            "Cannot convert from dimension " + from + " to dimension " + to
                + " without a recorded bridging material property (density / unit-weight)");
    }

    /** UOMCONV-VERSION-001 — a conversion was requested against a material version that does not exist. */
    public static UomConversionException unknownMaterialVersion(long version) {
        return new UomConversionException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:uom-unknown-material-version", "UNKNOWN_MATERIAL_VERSION",
            "No such material property version: " + version);
    }
}
