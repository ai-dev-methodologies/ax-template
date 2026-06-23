package com.ax.template.authblueprint.queryguard;

import java.util.Locale;
import java.util.Optional;

/**
 * query-field-allowlist-l0 — the closed safe set of filter operators
 * (QUERY-ALLOWLIST-FILTER-001). A predicate is built ONLY from one of these
 * operators plus a parameter-bound value and an allowlisted property; an
 * unrecognized operator token is a 422 QUERY_OPERATOR_INVALID. No operator token
 * is ever interpolated into a query string (CWE-89) — the enum selects a typed
 * {@code jakarta.persistence.criteria} predicate factory instead.
 */
public enum FilterOperator {
    EQ,
    NE,
    GT,
    GTE,
    LT,
    LTE,
    LIKE;

    /** Parse a case-insensitive client token; null/unknown ⇒ empty (the caller raises 422). */
    static Optional<FilterOperator> parse(String token) {
        if (token == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(FilterOperator.valueOf(token.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException unknown) {
            return Optional.empty();
        }
    }
}
