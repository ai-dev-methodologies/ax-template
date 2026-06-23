package com.ax.template.authblueprint.queryguard;

/**
 * query-field-allowlist-l0 — the closed set of permitted sort directions
 * (QUERY-ALLOWLIST-SORT-001). A client-supplied direction outside this set is a
 * 422 QUERY_DIRECTION_INVALID; a raw client string can never reach Spring Data's
 * {@code Sort.Direction}.
 */
public enum SortDirection {
    ASC,
    DESC;

    /** Parse a case-insensitive client token; null/unknown ⇒ empty (the caller raises 422). */
    static java.util.Optional<SortDirection> parse(String token) {
        if (token == null) {
            return java.util.Optional.empty();
        }
        return switch (token.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "asc" -> java.util.Optional.of(ASC);
            case "desc" -> java.util.Optional.of(DESC);
            default -> java.util.Optional.empty();
        };
    }
}
