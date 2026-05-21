package com.ax.template.authblueprint.tagcategorization;

/** TAG-HIER-003 — mapped to HTTP 409 TAG_HAS_CHILDREN. */
public class TagHasChildrenException extends RuntimeException {
    public TagHasChildrenException(String detail) {
        super(detail);
    }
}
