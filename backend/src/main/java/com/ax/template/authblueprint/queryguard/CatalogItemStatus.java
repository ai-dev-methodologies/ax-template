package com.ax.template.authblueprint.queryguard;

/**
 * query-field-allowlist-l0 — the demonstrating resource's lifecycle status, an allowlisted
 * filterable/sortable field. A closed enum so an eq-filter on `status` is bounded to known
 * values and the column is never a free-text injection surface.
 */
public enum CatalogItemStatus {
    DRAFT,
    ACTIVE,
    ARCHIVED
}
