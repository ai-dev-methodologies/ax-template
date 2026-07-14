package com.ax.template.authblueprint.facetcount;

import org.springframework.http.HttpStatus;

import java.util.Collection;

/** Domain exception for facet-count-l0. status + RFC 9457 type + machine-readable code. */
public class FacetCountException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private FacetCountException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    /** FACET-ALLOWLIST-002 — the requested facet field is not in the resource's allowlist. */
    public static FacetCountException notAllowed(String field, Collection<String> allowed) {
        return new FacetCountException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:facet-field-not-allowed", "FACET_FIELD_NOT_ALLOWED",
            "Field '" + field + "' is not facetable on this resource; facetable fields: " + allowed);
    }

    public static FacetCountException notFound() {
        return new FacetCountException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Item not found");
    }
}
