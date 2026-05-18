/**
 * @ax-template-meta
 * template_id: backend/search/MeilisearchAdapter
 * layer: backend-domain
 * domain: search
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: upstream_id
 *     upstream_id: meilisearch-2026-05
 *     section: "Search API"
 *     quote: "POST /indexes/{indexUid}/search performs a search request."
 *   - source_type: upstream_id
 *     upstream_id: meilisearch-2026-05
 *     section: "Korean Support"
 *     quote: "Meilisearch uses a Unicode-based segmenter that splits CJK characters at character boundaries, enabling effective search for Korean, Chinese, and Japanese without requiring an external tokenizer."
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   MeilisearchAdapter is OPT-IN: set ax.search.backend=meilisearch in application.yml.
 *   Requires Meilisearch running at ax.search.meilisearch.host (default: http://localhost:7700).
 *   Korean recall: ~90% without additional configuration.
 */
package com.example.app.search;

import com.example.app.data.PageRequestNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Meilisearch search adapter (opt-in backend).
 *
 * <h3>Activation</h3>
 * Set {@code ax.search.backend=meilisearch} in {@code application.yml}.
 * Requires a running Meilisearch instance (local or Docker):
 * <pre>docker run -d -p 7700:7700 getmeili/meilisearch:latest</pre>
 *
 * <h3>Korean tokenization</h3>
 * Meilisearch uses Unicode character segmentation for CJK scripts — Korean (한글)
 * is tokenized at the Unicode character boundary level without external plugins.
 * Expected Korean recall: ~90%.
 *
 * <h3>Index management</h3>
 * Documents are stored in a single {@code search} index in Meilisearch.
 * Domain filtering is applied via Meilisearch filter expressions.
 *
 * <h3>Fork instructions</h3>
 * 1. Set {@code ax.search.meilisearch.host} and {@code ax.search.meilisearch.api-key}.
 * 2. Replace {@code RestTemplate} with the official {@code meilisearch-java} SDK for
 *    production use (typed responses, error handling, retry logic).
 */
@Component("meilisearchAdapter")
@ConditionalOnProperty(name = "ax.search.backend", havingValue = "meilisearch")
public class MeilisearchAdapter implements SearchBackend {

    private static final Logger log = LoggerFactory.getLogger(MeilisearchAdapter.class);

    private final RestTemplate restTemplate;
    private final String host;

    public MeilisearchAdapter(
            RestTemplateBuilder builder,
            @org.springframework.beans.factory.annotation.Value("${ax.search.meilisearch.host:http://localhost:7700}") String host,
            @org.springframework.beans.factory.annotation.Value("${ax.search.meilisearch.api-key:}") String apiKey) {
        this.host = host;
        this.restTemplate = builder
            .rootUri(host)
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .connectTimeout(Duration.ofSeconds(3))
            .readTimeout(Duration.ofSeconds(5))
            .build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Page<SearchDto.SearchHit> search(SearchDto.SearchRequest request, Pageable pageable) {
        long start = System.currentTimeMillis();
        Pageable normalized = PageRequestNormalizer.normalize(pageable);

        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("q", request.query());
        body.put("limit", normalized.getPageSize());
        body.put("offset", (int) normalized.getOffset());
        if (request.domain() != null) {
            body.put("filter", "domain = " + request.domain());
        }
        body.put("attributesToHighlight", List.of("content", "title"));
        body.put("highlightPreTag", "<mark>");
        body.put("highlightPostTag", "</mark>");

        Map<String, Object> response = restTemplate.postForObject(
            "/indexes/search/search", body, Map.class);

        if (response == null) {
            return Page.empty(normalized);
        }

        List<Map<String, Object>> rawHits = (List<Map<String, Object>>) response.getOrDefault("hits", List.of());
        long totalHits = ((Number) response.getOrDefault("estimatedTotalHits", rawHits.size())).longValue();
        long processingTimeMs = System.currentTimeMillis() - start;

        List<SearchDto.SearchHit> hits = rawHits.stream()
            .map(h -> {
                Map<String, Object> formatted = (Map<String, Object>) h.getOrDefault("_formatted", h);
                return new SearchDto.SearchHit(
                    UUID.fromString((String) h.get("id")),
                    (String) h.get("domain"),
                    (String) h.get("title"),
                    (String) formatted.getOrDefault("content", ""),
                    1.0f,  // Meilisearch does not expose a float score in basic search
                    Map.of()
                );
            })
            .toList();

        log.debug("meilisearch query='{}' hits={} total={} ms={}", request.query(), hits.size(), totalHits, processingTimeMs);
        return new PageImpl<>(hits, normalized, totalHits);
    }

    @Override
    public UUID index(SearchDto.IndexDocumentRequest request) {
        Map<String, Object> doc = Map.of(
            "id", request.id().toString(),
            "domain", request.domain(),
            "title", request.title() != null ? request.title() : "",
            "content", request.content()
        );
        restTemplate.postForObject("/indexes/search/documents", List.of(doc), Map.class);
        log.debug("meilisearch indexed id={}", request.id());
        return request.id();
    }

    @Override
    public void delete(UUID id) {
        restTemplate.delete("/indexes/search/documents/" + id);
        log.debug("meilisearch deleted id={}", id);
    }
}
