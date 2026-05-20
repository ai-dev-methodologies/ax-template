package com.ax.template.authblueprint.search;

import java.util.UUID;

/**
 * Search backend SPI — pluggable so fork-receivers can swap the default
 * PostgreSQL-FTS-like (LIKE-based reference) implementation for Meilisearch,
 * Elasticsearch, OpenSearch, or any other engine without touching the
 * application service.
 * <p>
 * Trace:
 * <ul>
 *   <li>SEARCH-BACKEND-001 — default is {@code postgres-fts} (reference
 *       {@link PostgresFtsAdapter}); {@code ax.search.backend=meilisearch}
 *       activates an opt-in {@code MeilisearchAdapter} per
 *       {@code blueprints/search-manifest.yaml#backend}.</li>
 *   <li>SEARCH-AUTHZ-002 — every method REQUIRES a {@code tenantId} so adapters
 *       cannot accidentally search outside the caller's tenant scope.</li>
 * </ul>
 */
public interface SearchBackend {

    /** Identifier returned by {@code /api/v1/search/backend} (advisory). */
    String name();

    /** SEARCH-INDEX-001 — upsert a document. */
    void index(SearchIndexDocument document);

    /** SEARCH-INDEX-002 — remove by id; no-op if absent. */
    void delete(UUID id, String tenantId);

    /** SEARCH-QUERY-001/002 — tenant-scoped paginated search. */
    SearchResults search(String tenantId, String query, String domain, int page, int size);
}
