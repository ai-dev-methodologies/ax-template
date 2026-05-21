package com.ax.template.authblueprint.tagcategorization;

import java.util.UUID;

/** TAG-HIER-002 — mapped to HTTP 400 PARENT_NOT_FOUND. */
public class ParentTagNotFoundException extends RuntimeException {
    public ParentTagNotFoundException(UUID parentId) {
        super("parent tag not found: " + parentId);
    }
}
