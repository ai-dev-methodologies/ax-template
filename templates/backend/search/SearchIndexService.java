/**
 * @ax-template-meta
 * template_id: backend/search/SearchIndexService
 * layer: backend-domain
 * domain: search
 * anchors_rule: api-controller-service-separation.md (PRACTICES-API-003)
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "Spring Framework Reference — @Service and transaction management: service layer owns business logic; controller delegates entirely"
 *     url: "https://docs.spring.io/spring-framework/reference/core/beans/classpath-scanning.html"
 *   - source_type: upstream_id
 *     upstream_id: postgresql-fts-2026-05
 *     section: "Spring Integration"
 *     quote: "Use @Query with native SQL or implement a custom JpaRepository method with a NativeQuery"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   SearchIndexService orchestrates search and indexing operations.
 *   Delegates to the active SearchBackend (postgres-fts or meilisearch).
 *   SearchController delegates entirely to this service — no business logic in the controller.
 */
package com.example.app.search;

import com.example.app.data.PageRequestNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Business logic for the search domain.
 *
 * <p>Operations:
 * <ul>
 *   <li>{@link #search(SearchDto.SearchRequest, Pageable)} — execute a full-text search
 *   <li>{@link #index(SearchDto.IndexDocumentRequest)} — index a document
 *   <li>{@link #delete(UUID)} — remove a document from the index
 * </ul>
 *
 * <p>The active {@link SearchBackend} implementation is injected by Spring
 * based on the {@code ax.search.backend} configuration property.
 * Default: {@link PostgresFtsAdapter}. Opt-in: {@link MeilisearchAdapter}.
 */
@Service
public class SearchIndexService {

    private static final Logger log = LoggerFactory.getLogger(SearchIndexService.class);

    private final SearchBackend backend;

    public SearchIndexService(SearchBackend backend) {
        this.backend = backend;
    }

    /**
     * Executes a full-text search.
     *
     * <p>Page parameters are normalized via {@link PageRequestNormalizer} before
     * being passed to the backend (enforces max size = 100, page ≥ 0).
     *
     * @param request  validated search request from the controller
     * @param pageable raw pageable from the request (normalized here)
     * @return paginated search result page
     */
    public Page<SearchDto.SearchHit> search(SearchDto.SearchRequest request, Pageable pageable) {
        Pageable normalized = PageRequestNormalizer.normalize(pageable);
        log.debug("search query='{}' domain='{}' page={} size={}",
            request.query(), request.domain(), normalized.getPageNumber(), normalized.getPageSize());
        return backend.search(request, normalized);
    }

    /**
     * Indexes a document for full-text search.
     *
     * @param request the document to index
     * @return the indexed document's UUID
     */
    public UUID index(SearchDto.IndexDocumentRequest request) {
        log.debug("indexing document id={} domain={}", request.id(), request.domain());
        return backend.index(request);
    }

    /**
     * Removes a document from the search index.
     *
     * @param id the document UUID to remove
     */
    public void delete(UUID id) {
        log.debug("deleting document from index id={}", id);
        backend.delete(id);
    }
}
