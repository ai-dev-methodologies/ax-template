package com.ax.template.authblueprint.tagcategorization;

/** TAG-CRUD-001 — mapped to HTTP 400 DUPLICATE_SLUG. */
public class DuplicateSlugException extends RuntimeException {
    public DuplicateSlugException(String detail) {
        super(detail);
    }
}
