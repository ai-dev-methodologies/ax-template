/**
 * @ax-template-meta
 * template_id: backend/search/SearchController
 * layer: backend-domain
 * domain: search
 * anchors_rule: testing-archunit-layer-boundary.md
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "Spring MVC Reference — @RestController combines @Controller and @ResponseBody; @RequestMapping declares the base path"
 *     url: "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html"
 *   - source_type: external
 *     citation: "Spring Data Commons Reference — Pageable is automatically resolved from request parameters page, size, sort when @EnableSpringDataWebSupport is configured"
 *     url: "https://docs.spring.io/spring-data/commons/reference/repositories/query-methods-details.html#repositories.special-parameters"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   SearchController delegates all logic to SearchIndexService.
 *   PageRequestNormalizer.normalize() is called in the service layer — not here.
 *   All endpoints require JWT authentication (configured in SecurityConfig).
 */
package com.example.app.search;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * REST controller for the search domain.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code POST /api/v1/search} — full-text search (SEARCH-QUERY-001/002/003)
 *   <li>{@code POST /api/v1/search/index} — index a document (SEARCH-INDEX-001)
 *   <li>{@code DELETE /api/v1/search/index/{id}} — remove from index (SEARCH-INDEX-002)
 * </ul>
 *
 * <p>All business logic is delegated to {@link SearchIndexService}.
 * No business logic lives in this controller.
 */
@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchIndexService searchIndexService;

    public SearchController(SearchIndexService searchIndexService) {
        this.searchIndexService = searchIndexService;
    }

    /**
     * Full-text search.
     *
     * <p>Accepts {@link SearchDto.SearchRequest} in the request body.
     * Page parameters are extracted from the body's {@code page} and {@code size} fields;
     * {@code Pageable} is used for repository-layer compatibility only.
     * {@code PageRequestNormalizer} enforces max size = 100 in the service layer.
     *
     * @param request  validated search request
     * @param pageable Spring-resolved pageable (size/page defaults applied from @PageableDefault)
     * @return 200 with paginated {@link SearchDto.SearchResultPage}
     */
    @PostMapping
    public ResponseEntity<SearchDto.SearchResultPage> search(
            @Valid @RequestBody SearchDto.SearchRequest request,
            @PageableDefault(size = 20) Pageable pageable) {

        long start = System.currentTimeMillis();
        Page<SearchDto.SearchHit> page = searchIndexService.search(request, pageable);
        long processingTimeMs = System.currentTimeMillis() - start;

        SearchDto.SearchResultPage result = new SearchDto.SearchResultPage(
            page.getContent(),
            page.getTotalElements(),
            page.getNumber(),
            page.getSize(),
            processingTimeMs
        );

        return ResponseEntity.ok(result);
    }

    /**
     * Index a document.
     *
     * @param request the document to index
     * @return 201 Created with the indexed document id
     */
    @PostMapping("/index")
    public ResponseEntity<SearchDto.IndexDocumentResponse> index(
            @Valid @RequestBody SearchDto.IndexDocumentRequest request) {

        UUID id = searchIndexService.index(request);
        return ResponseEntity
            .created(URI.create("/api/v1/search/index/" + id))
            .body(new SearchDto.IndexDocumentResponse(id, true));
    }

    /**
     * Remove a document from the search index.
     *
     * @param id the document UUID to remove
     * @return 204 No Content
     */
    @DeleteMapping("/index/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        searchIndexService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
