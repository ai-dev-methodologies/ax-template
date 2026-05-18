/**
 * @ax-template-meta
 * template_id: backend/search/SearchBackend
 * layer: backend-domain
 * domain: search
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "Spring Framework — @ConditionalOnProperty selects a bean based on a configuration property value, enabling pluggable backend adapters without code changes"
 *     url: "https://docs.spring.io/spring-boot/docs/current/api/org/springframework/boot/autoconfigure/condition/ConditionalOnProperty.html"
 *   - source_type: external
 *     citation: "Effective Java 3rd ed. §Item 20 — Prefer interfaces to abstract classes; interfaces enable multiple implementations to be swapped at configuration time"
 *     url: "https://www.pearson.com/en-us/subject-catalog/p/effective-java/P200000000138"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Two implementations ship: PostgresFtsAdapter (default) and MeilisearchAdapter (opt-in).
 *   The active implementation is selected via ax.search.backend (see SearchBackendConfig).
 */
package com.example.app.search;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Port interface for search backend adapters.
 *
 * <p>Implementations:
 * <ul>
 *   <li>{@link PostgresFtsAdapter} — default; uses PostgreSQL full-text search with 'simple' dictionary.
 *   <li>{@link MeilisearchAdapter} — opt-in; requires {@code ax.search.backend=meilisearch} + running instance.
 * </ul>
 *
 * <p>Activation: {@code @ConditionalOnProperty(name="ax.search.backend", havingValue="meilisearch")}
 * selects MeilisearchAdapter; absence of the property (or any other value) selects PostgresFtsAdapter.
 */
public interface SearchBackend {

    /**
     * Executes a full-text search query.
     *
     * @param request  the normalized search request
     * @param pageable normalized pageable (already passed through PageRequestNormalizer)
     * @return paginated search results with snippets and scores
     */
    Page<SearchDto.SearchHit> search(SearchDto.SearchRequest request, Pageable pageable);

    /**
     * Indexes a document so it becomes searchable.
     *
     * @param request the document to index
     * @return the indexed document's id
     */
    UUID index(SearchDto.IndexDocumentRequest request);

    /**
     * Removes a document from the search index.
     *
     * @param id the document id to remove
     */
    void delete(UUID id);
}
