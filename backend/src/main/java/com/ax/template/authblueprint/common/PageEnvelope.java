package com.ax.template.authblueprint.common;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * Canonical offset-pagination response envelope — ships the REAL reusable code
 * for the {@code pagination-l0} catalog spec item PAGE-OFFSET-001
 * (specs/pagination-l0.yaml).
 *
 * <p>The spec's PAGE-OFFSET-001 contract was prose-only, and the IDW2 dogfood
 * (2026-05-29) proved the consequence: <em>every</em> domain re-typed the page
 * response and the shapes DIVERGED. The ecommerce reference workload returns
 * {@code {content, totalElements}} (see {@code EcommerceDto.ProductList}); other
 * domains hand-rolled ad-hoc page DTOs; NONE emit the canonical shape the spec
 * pins. A fork-receiver who follows one domain literally produces a payload that
 * is off-contract with the next domain — the exact drift this primitive kills.
 *
 * <h2>Canonical shape (PAGE-OFFSET-001 — the IDW1 3/3-converged shape)</h2>
 * <pre>{@code
 * {
 *   "data": [ ... ],
 *   "pagination": {
 *     "page": 0,
 *     "pageSize": 20,
 *     "totalElements": 137,
 *     "totalPages": 7,
 *     "hasMore": true
 *   }
 * }
 * }</pre>
 *
 * The member names are fixed by the spec ({@code data} + {@code pagination} with
 * exactly {@code page / pageSize / totalElements / totalPages / hasMore}) so the
 * serialized JSON is byte-stable across every list/collection API in a
 * fork-receiver. Do NOT rename these fields per domain — that is the divergence
 * this record exists to prevent.
 *
 * <h2>Usage sketch</h2>
 * <pre>{@code
 * // Spring Data source — the common case:
 * Page<Product> page = repository.findAll(
 *     OffsetPageSupport.clamp(pageParam, sizeParam, MAX_PAGE_SIZE)
 *         .withSort(OffsetPageSupport.stableSort(Sort.by(Sort.Direction.DESC, "createdAt"))));
 * return PageEnvelope.from(page, ProductResponse::from);   // maps + fills pagination
 *
 * // Non-Spring source (in-memory slice, external API page, etc.):
 * return PageEnvelope.of(items, page, pageSize, totalElements);
 * }</pre>
 *
 * <p>This record is a transport DTO only: it carries no Spring annotations and
 * is built by the static factories below. {@link Pagination#totalPages} and
 * {@link Pagination#hasMore} are DERIVED, never client-supplied, so they can
 * never disagree with {@code totalElements} / {@code pageSize}.
 *
 * @param <T> the element type after mapping (the response DTO, not the entity)
 */
public record PageEnvelope<T>(List<T> data, Pagination pagination) {

    /** {@code pagination-l0} default page size (PAGE-LIMIT-001: default 20). */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * The pagination metadata block — exactly the five members PAGE-OFFSET-001
     * pins, in the canonical order.
     *
     * @param page          zero-based page index of this slice
     * @param pageSize      the requested (clamped) page size
     * @param totalElements total matching rows across all pages
     * @param totalPages    derived count of pages: {@code ceil(total / size)}
     * @param hasMore       whether a next page exists after this one
     */
    public record Pagination(
            int page,
            int pageSize,
            long totalElements,
            int totalPages,
            boolean hasMore) {}

    /**
     * Build the canonical envelope from a Spring Data {@link Page}, mapping each
     * entity to its response DTO. Every pagination field is read straight from
     * the {@code Page} so it cannot drift from the persisted result:
     * {@code hasMore = page.hasNext()} and {@code totalPages = page.getTotalPages()}
     * (Spring already computes both — re-deriving them by hand is where the
     * hand-rolled DTOs went wrong).
     *
     * @param page   the Spring Data result page (must be non-null)
     * @param mapper entity → DTO mapper applied to {@link Page#getContent()}
     * @param <E>    the entity type held by the {@code Page}
     * @param <T>    the mapped DTO type carried in {@link #data}
     * @return the canonical {@code {data, pagination}} envelope
     */
    public static <E, T> PageEnvelope<T> from(Page<E> page, Function<E, T> mapper) {
        if (page == null) {
            throw new IllegalArgumentException("page must be non-null");
        }
        if (mapper == null) {
            throw new IllegalArgumentException("mapper must be non-null");
        }
        List<T> mapped = page.getContent().stream().map(mapper).toList();
        Pagination pagination = new Pagination(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext());
        return new PageEnvelope<>(mapped, pagination);
    }

    /**
     * Build the canonical envelope from a non-Spring source — an already-mapped
     * slice plus the offset coordinates. {@code totalPages} and {@code hasMore}
     * are DERIVED here exactly as Spring Data derives them, so a hand-built
     * envelope is indistinguishable from {@link #from(Page, Function)}:
     * <ul>
     *   <li>{@code totalPages = pageSize == 0 ? 0 : ceil(totalElements / pageSize)}</li>
     *   <li>{@code hasMore   = (page + 1) < totalPages}</li>
     * </ul>
     *
     * @param data          the page slice (already mapped to DTOs); must be non-null
     * @param page          zero-based page index ({@code >= 0})
     * @param pageSize      the page size ({@code >= 0})
     * @param totalElements total matching rows ({@code >= 0})
     * @param <T>           the DTO type carried in {@link #data}
     * @return the canonical {@code {data, pagination}} envelope
     */
    public static <T> PageEnvelope<T> of(List<T> data, int page, int pageSize, long totalElements) {
        if (data == null) {
            throw new IllegalArgumentException("data must be non-null");
        }
        if (page < 0 || pageSize < 0 || totalElements < 0) {
            throw new IllegalArgumentException(
                    "page, pageSize, totalElements must be non-negative: "
                            + "page=" + page + " pageSize=" + pageSize + " totalElements=" + totalElements);
        }
        int totalPages = pageSize == 0
                ? 0
                : (int) ((totalElements + pageSize - 1) / pageSize);
        boolean hasMore = (long) (page + 1) * pageSize < totalElements;
        return new PageEnvelope<>(
                List.copyOf(data),
                new Pagination(page, pageSize, totalElements, totalPages, hasMore));
    }
}
