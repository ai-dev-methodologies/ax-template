package com.ax.template.authblueprint.search;

import java.util.List;

/**
 * Tenant-scoped search result page returned by {@link SearchBackend#search}.
 * <p>
 * Trace: SEARCH-QUERY-001 — exposes {@code totalHits}, {@code page}, {@code size}
 * plus the raw documents.
 */
public record SearchResults(
    List<SearchIndexDocument> hits,
    long totalHits,
    int page,
    int size
) {
    public SearchResults {
        hits = (hits == null) ? List.of() : List.copyOf(hits);
    }
}
