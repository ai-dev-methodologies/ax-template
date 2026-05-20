package com.ax.template.authblueprint.search;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * SearchDto — request / response shape for {@code /api/v1/search/**}.
 * <p>
 * Trace:
 * <ul>
 *   <li>SEARCH-QUERY-001 — {@link SearchRequest} carries {@code query}, {@code domain}, {@code page}, {@code size}</li>
 *   <li>SEARCH-QUERY-003 — {@code @NotBlank} on the query field → 400 on blank input</li>
 *   <li>SEARCH-INDEX-001 — {@link IndexRequest} carries the document id, domain, content, metadata</li>
 *   <li>SEARCH-INDEX-002 — id is the path variable for delete; no body needed</li>
 * </ul>
 */
public final class SearchDto {

    private SearchDto() {}

    public record SearchRequest(
        @NotBlank(message = "query is required") @Size(max = 1024) String query,
        String domain,
        Integer page,
        Integer size
    ) {}

    public record IndexRequest(
        UUID id,
        String domain,
        @NotBlank(message = "content is required") @Size(max = 4000) String content,
        String metadata
    ) {}

    public record SearchHit(
        UUID id,
        String domain,
        String content,
        String metadata,
        Instant indexedAt
    ) {
        public static SearchHit from(SearchIndexDocument doc) {
            return new SearchHit(
                doc.getId(),
                doc.getDomain(),
                doc.getContent(),
                doc.getMetadata(),
                doc.getIndexedAt()
            );
        }
    }

    public record SearchResultPage(
        List<SearchHit> hits,
        long totalHits,
        int page,
        int size,
        long processingTimeMs
    ) {}

    public record IndexResponse(UUID id) {}
}
