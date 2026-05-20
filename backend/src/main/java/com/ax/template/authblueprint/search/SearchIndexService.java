package com.ax.template.authblueprint.search;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Application service for the search domain.
 * <p>
 * Trace:
 * <ul>
 *   <li>SEARCH-AUTHZ-002 — every read/write filters on {@code tenantId = caller}</li>
 *   <li>SEARCH-INDEX-001 — {@link #index(String, UUID, String, String, String)}</li>
 *   <li>SEARCH-INDEX-002 — {@link #delete(UUID, String)}</li>
 *   <li>SEARCH-QUERY-001/002 — {@link #search(String, String, String, int, int)}</li>
 *   <li>SEARCH-QUERY-003 — blank query handled by Bean Validation at the controller</li>
 *   <li>SEARCH-BACKEND-001 — delegates to the configured {@link SearchBackend}</li>
 * </ul>
 */
@Service
public class SearchIndexService {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    private final SearchBackend backend;

    public SearchIndexService(SearchBackend backend) {
        this.backend = backend;
    }

    /** SEARCH-INDEX-001 — upsert a document scoped to the caller's tenant. */
    public UUID index(String tenantId, UUID id, String domain, String content, String metadata) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content is required");
        }
        SearchIndexDocument doc = SearchIndexDocument.builder()
            .id(id)
            .tenantId(tenantId)
            .domain(domain)
            .content(content)
            .metadata(metadata)
            .indexedAt(Instant.now())
            .build();
        backend.index(doc);
        return doc.getId();
    }

    /** SEARCH-INDEX-002 — delete by id; no-op when absent or cross-tenant. */
    public void delete(UUID id, String tenantId) {
        backend.delete(id, tenantId);
    }

    /** SEARCH-QUERY-001/002 — tenant-scoped paginated search. */
    public SearchDto.SearchResultPage search(String tenantId, String query, String domain, int page, int size) {
        int boundedSize = (size <= 0) ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        int boundedPage = Math.max(page, 0);
        long start = System.nanoTime();
        SearchResults result = backend.search(tenantId, query, domain, boundedPage, boundedSize);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;

        return new SearchDto.SearchResultPage(
            result.hits().stream().map(SearchDto.SearchHit::from).toList(),
            result.totalHits(),
            result.page(),
            result.size(),
            elapsedMs
        );
    }

    /** Diagnostic — returns the active backend's name. */
    public String activeBackendName() {
        return backend.name();
    }
}
