/**
 * @ax-template-meta
 * template_id: backend/data/PageRequestNormalizer
 * layer: backend-infrastructure
 * domain: data
 * anchors_rule: api-pagination-pageable.md (PRACTICES-API-004)
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "Spring Data Commons Reference — Pageable and PageRequest: PageRequest.of(page, size, sort) constructs a pageable; size must be capped server-side to prevent unbounded result sets"
 *     url: "https://docs.spring.io/spring-data/commons/reference/repositories/query-methods-details.html#repositories.special-parameters"
 *   - source_type: external
 *     citation: "OWASP API Security Top 10 2023 — API4:2023 Unrestricted Resource Consumption: APIs that allow consumers to set page size without an upper bound are vulnerable to resource exhaustion attacks"
 *     url: "https://owasp.org/API-Security/editions/2023/en/0xa4-unrestricted-resource-consumption/"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Use PageRequestNormalizer.normalize(pageable) in service methods before passing to repositories.
 *   The normalizer caps page size to MAX_PAGE_SIZE and clamps the page number to 0 if negative.
 *   Default sort by created_at DESC is applied when no sort is requested.
 */
package com.example.app.data;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * Normalizes inbound {@link Pageable} parameters to enforce server-side limits.
 *
 * <h3>Why server-side normalization?</h3>
 * Clients that control page size directly can cause resource exhaustion by requesting
 * arbitrarily large pages. Spring's {@code @PageableDefault} annotation sets a
 * client-visible default but does not cap the maximum — a client can still override
 * it via {@code ?size=10000}. This normalizer enforces a hard ceiling regardless of
 * what the client requests (OWASP API4:2023).
 *
 * <h3>Normalization rules:</h3>
 * <ul>
 *   <li>Page number: clamped to {@code [0, MAX_PAGE_NUMBER]}</li>
 *   <li>Page size: clamped to {@code [1, MAX_PAGE_SIZE]}</li>
 *   <li>Sort: client sort is preserved if present; {@code DEFAULT_SORT} applied when absent</li>
 * </ul>
 *
 * <h3>Usage in a service:</h3>
 * <pre>
 * {@literal @}Service
 * public class NotificationService {
 *     private final PageRequestNormalizer pageable;
 *
 *     public Page{@literal <}NotificationDto.Response{@literal >} list(UUID userId, Pageable rawPageable) {
 *         Pageable page = PageRequestNormalizer.normalize(rawPageable);
 *         return repository.findByRecipientUserId(userId, page).map(NotificationDto.Response::from);
 *     }
 * }
 * </pre>
 */
@Component
public class PageRequestNormalizer {

    /** Maximum number of rows a single page request may return. */
    public static final int MAX_PAGE_SIZE = 100;

    /** Maximum page index (prevents deep pagination attacks). */
    public static final int MAX_PAGE_NUMBER = 10_000;

    /** Default sort applied when the client supplies no sort parameter. */
    public static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    /**
     * Normalizes a client-supplied {@link Pageable} by enforcing size/page limits.
     *
     * @param pageable the raw pageable from the controller (may be null)
     * @return a normalized {@link PageRequest} safe to pass to a repository
     */
    public static Pageable normalize(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            return PageRequest.of(0, 20, DEFAULT_SORT);
        }

        int page = Math.max(0, Math.min(pageable.getPageNumber(), MAX_PAGE_NUMBER));
        int size = Math.max(1, Math.min(pageable.getPageSize(), MAX_PAGE_SIZE));
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : DEFAULT_SORT;

        return PageRequest.of(page, size, sort);
    }
}
