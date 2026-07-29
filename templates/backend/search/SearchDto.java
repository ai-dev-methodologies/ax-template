/**
 * @ax-template-meta
 * template_id: backend/search/SearchDto
 * layer: backend-domain
 * domain: search
 * anchors_rule: lang-records-for-dtos.md
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "JEP 395 — Records: immutable data carrier classes with canonical constructors; ideal for DTOs where mutation is never needed"
 *     url: "https://openjdk.org/jeps/395"
 *   - source_type: external
 *     citation: "OWASP Mass Assignment Cheat Sheet — use explicit DTOs at API boundaries to prevent mass assignment vulnerabilities from auto-binding entity fields"
 *     url: "https://cheatsheetseries.owasp.org/cheatsheets/Mass_Assignment_Cheat_Sheet.html"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   All API boundary types are Java Records for immutability.
 *   SearchRequest uses @NotBlank to reject empty queries (SEARCH-QUERY-003).
 */
package com.example.app.search;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Max;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO namespace for the search domain.
 *
 * <p>All types are Java Records (JEP 395) — immutable data carriers with generated
 * canonical constructors, accessors, equals, hashCode, and toString.
 */
public final class SearchDto {

    private SearchDto() {}

    // ─── Request types ────────────────────────────────────────────────────────

    /**
     * Inbound search request from the REST client.
     *
     * @param query  non-blank search string; supports Korean/English
     * @param domain optional domain filter (payment, notification, etc.); null = all domains
     * @param page   zero-based page number; normalized server-side
     * @param size   page size; normalized server-side (max 100)
     */
    public record SearchRequest(
        @NotBlank(message = "Search query must not be blank")
        String query,

        String domain,

        int page,

        @Max(value = 100, message = "Page size must not exceed 100")
        @Positive(message = "Page size must be positive")
        int size
    ) {
        public SearchRequest {
            if (page < 0) page = 0;
            if (size <= 0) size = 20;
            if (size > 100) size = 100;
        }
    }

    /**
     * Document to be indexed.
     *
     * @param id       document UUID (stable; used for dedup + delete)
     * @param domain   owning domain (payment, notification, etc.)
     * @param title    optional display title
     * @param content  plain text content to full-text index
     * @param metadata arbitrary key-value pairs stored alongside the document
     */
    public record IndexDocumentRequest(
        UUID id,
        @NotBlank String domain,
        String title,
        @NotBlank String content,
        Map<String, Object> metadata
    ) {}

    // ─── Response types ───────────────────────────────────────────────────────

    /**
     * Single search result hit.
     *
     * @param id       document UUID
     * @param domain   owning domain
     * @param title    document title (may be null)
     * @param snippet  highlighted snippet with {@code <mark>} tags around matched terms
     * @param score    relevance score from the backend
     * @param metadata arbitrary key-value pairs from the index
     */
    public record SearchHit(
        UUID id,
        String domain,
        String title,
        String snippet,
        float score,
        Map<String, Object> metadata
    ) {}

    /**
     * Paginated search result page returned to the REST client.
     *
     * @param hits             list of matching documents (may be empty)
     * @param totalHits        total matching count across all pages
     * @param page             current zero-based page
     * @param size             page size used for this response
     * @param processingTimeMs backend search latency in milliseconds
     */
    public record SearchResultPage(
        List<SearchHit> hits,
        long totalHits,
        int page,
        int size,
        long processingTimeMs
    ) {}

    /**
     * Response after successfully indexing a document.
     *
     * @param id      the indexed document UUID
     * @param indexed always {@code true} on 201 responses
     */
    public record IndexDocumentResponse(UUID id, boolean indexed) {}
}
