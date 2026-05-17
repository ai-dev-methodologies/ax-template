/**
 * @ax-template-meta
 * template_id: backend/dto/PageResponse
 * layer: backend-cross-cutting
 * anchors_rule: api-pagination-pageable.md (PRACTICES-API-001)
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "Spring Data Commons Reference — Pageable / Page<T>"
 *     url: "https://docs.spring.io/spring-data/commons/reference/repositories/core-concepts.html"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Return PageResponse<T> from all list endpoints:
 *
 *     @GetMapping
 *     public PageResponse<ItemResponse> list(@ParameterObject Pageable pageable) {
 *         return PageResponse.from(itemRepository.findAllActive(pageable)
 *                 .map(item -> new ItemResponse(item.getId(), item.getName())));
 *     }
 *
 *   Clamp size in the controller or via SpringDataWebAutoConfiguration properties:
 *     spring.data.web.pageable.max-page-size=100
 */
package com.example.app.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Serialisable pagination envelope for list API responses.
 *
 * <p>All list endpoints must return this envelope instead of a raw {@code List<T>}.
 * It communicates total count, page metadata, and the slice of items — enabling
 * clients to implement pagination UI without extra round trips.
 *
 * <p>Wire format:
 * <pre>{@code
 * {
 *   "content":       [...],
 *   "page":          0,
 *   "size":          20,
 *   "totalElements": 342,
 *   "totalPages":    18,
 *   "last":          false
 * }
 * }</pre>
 *
 * <p>Rule reference: PRACTICES-API-001 (list endpoints must use Pageable and clamp size).
 *
 * @param <T> element type
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    /**
     * Constructs a {@link PageResponse} from a Spring Data {@link Page}.
     *
     * @param springPage the Spring Data page result
     * @param <T>        element type
     * @return populated envelope
     */
    public static <T> PageResponse<T> from(Page<T> springPage) {
        return new PageResponse<>(
                springPage.getContent(),
                springPage.getNumber(),
                springPage.getSize(),
                springPage.getTotalElements(),
                springPage.getTotalPages(),
                springPage.isLast()
        );
    }
}
