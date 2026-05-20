package com.ax.template.authblueprint.search;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Default search backend — reference workload. Uses JPA + case-insensitive LIKE
 * on the {@code search_index_documents} table.
 * <p>
 * Production-grade deployments should swap this for {@code PostgresFtsAdapter}
 * with {@code to_tsvector} / {@code ts_headline}, or for the opt-in
 * {@code MeilisearchAdapter}. The contract is captured by {@link SearchBackend}
 * so the swap is a single bean registration in
 * {@link SearchBackendConfig}.
 * <p>
 * Trace:
 * <ul>
 *   <li>SEARCH-BACKEND-001 — registered when
 *       {@code ax.search.backend} is absent or {@code postgres-fts}</li>
 *   <li>SEARCH-AUTHZ-002 — tenant filter is included in every query</li>
 * </ul>
 */
public class PostgresFtsAdapter implements SearchBackend {

    /** Hard upper bound — manifest {@code query.max_page_size}. */
    public static final int MAX_PAGE_SIZE = 100;

    private final SearchIndexDocumentRepository repository;

    public PostgresFtsAdapter(SearchIndexDocumentRepository repository) {
        this.repository = repository;
    }

    @Override
    public String name() { return "postgres-fts"; }

    @Override
    @Transactional
    public void index(SearchIndexDocument document) {
        repository.save(document);
    }

    @Override
    @Transactional
    public void delete(UUID id, String tenantId) {
        repository.findByIdAndTenantId(id, tenantId).ifPresent(repository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults search(String tenantId, String query, String domain, int page, int size) {
        int boundedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int boundedPage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(boundedPage, boundedSize);
        Page<SearchIndexDocument> result = repository.searchByContent(
            tenantId, query.trim(), domain, pageable);
        return new SearchResults(
            result.getContent(),
            result.getTotalElements(),
            result.getNumber(),
            result.getSize());
    }
}
