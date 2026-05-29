package com.ax.template.authblueprint.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Offset-pagination request validation + stable-sort helper — ships the REAL
 * reusable code for the {@code pagination-l0} catalog spec items PAGE-LIMIT-001
 * (page size bounds) and PAGE-STABLE-SORT-001 (composite tiebreaker)
 * (specs/pagination-l0.yaml).
 *
 * <p>Both contracts were prose-only and the IDW2 dogfood (2026-05-29) showed the
 * cost: each domain re-implemented page-size clamping inline (e.g. the ecommerce
 * controllers compute a local {@code safeSize}) and NONE appended the {@code id}
 * tiebreaker the spec requires. Two failure modes follow from hand-rolling this:
 *
 * <h2>Failure mode 1 — unbounded / inconsistent page size (PAGE-LIMIT-001)</h2>
 * Spec PAGE-LIMIT-001: default size 20, max 200 (absolute ceiling 1000),
 * "Server MUST NEVER honor unlimited requests" and out-of-range → 400. Inline
 * {@code Math.min(size, 100)}-style clamps silently differ per controller and
 * let {@code size <= 0} or oversized values through. {@link #clamp(int, int, int)}
 * centralises the rule and THROWS {@link IllegalArgumentException} on out-of-range
 * so a controller maps it to a 400 (PAGE_SIZE_INVALID) instead of silently
 * returning a surprise slice.
 *
 * <h2>Failure mode 2 — unstable sort (PAGE-STABLE-SORT-001)</h2>
 * Spec PAGE-STABLE-SORT-001: pagination MUST run over a stable sort with at
 * least one tiebreaker column; a single-column sort over a non-unique value
 * (e.g. {@code createdAt} alone, or {@code name} alone) can skip or duplicate
 * rows across page boundaries because the database is free to order ties
 * differently per query. {@link #stableSort(Sort)} appends an {@code id}
 * tiebreaker so the total order is deterministic.
 *
 * <h2>Usage sketch</h2>
 * <pre>{@code
 * PageRequest req = OffsetPageSupport
 *     .clamp(pageParam, sizeParam, MAX_PAGE_SIZE)               // 400 on out-of-range
 *     .withSort(OffsetPageSupport.stableSort(Sort.by(Sort.Direction.DESC, "createdAt")));
 * Page<Product> page = repository.findAll(req);
 * return PageEnvelope.from(page, ProductResponse::from);
 * }</pre>
 *
 * <p>Framework-touching by necessity (returns a Spring {@link PageRequest} /
 * {@link Sort}, which is exactly what callers need), but holds no state and has
 * no domain coupling. All methods are pure.
 */
public final class OffsetPageSupport {

    private OffsetPageSupport() {}

    /** PAGE-LIMIT-001 default page size when none specified. */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** PAGE-LIMIT-001 default maximum page size a recipe declares. */
    public static final int DEFAULT_MAX_PAGE_SIZE = 200;

    /** PAGE-LIMIT-001 absolute ceiling: {@code maxSize} may never exceed this. */
    public static final int ABSOLUTE_MAX_PAGE_SIZE = 1000;

    /** The tiebreaker column appended by {@link #stableSort(Sort)} (PAGE-STABLE-SORT-001). */
    public static final String TIEBREAKER_PROPERTY = "id";

    /**
     * Validate and clamp an offset-pagination request into a Spring
     * {@link PageRequest}, enforcing PAGE-LIMIT-001.
     *
     * <ul>
     *   <li>{@code page} must be {@code >= 0} — a negative page is rejected
     *       (offset pagination is zero-based).</li>
     *   <li>{@code size} must be within {@code [1, maxSize]} — {@code size <= 0}
     *       (the "unlimited" smell the spec forbids) and {@code size > maxSize}
     *       are both rejected. The default size of {@value #DEFAULT_PAGE_SIZE}
     *       is the recipe-level default and is applied by the CALLER before this
     *       method; {@code clamp} itself never silently substitutes a default,
     *       it fails loudly so the 400 carries an actionable hint.</li>
     *   <li>{@code maxSize} must be within {@code [1, }{@value #ABSOLUTE_MAX_PAGE_SIZE}{@code ]}
     *       — a recipe cannot raise its ceiling above the absolute cap.</li>
     * </ul>
     *
     * <p>On any out-of-range input this THROWS {@link IllegalArgumentException}
     * with a hint, which a controller maps to {@code 400 PAGE_SIZE_INVALID}
     * (RFC 9457 problem detail). The returned {@code PageRequest} carries no
     * sort — chain {@code .withSort(stableSort(...))} to satisfy
     * PAGE-STABLE-SORT-001.
     *
     * @param page    zero-based page index requested by the client
     * @param size    page size requested by the client
     * @param maxSize the recipe's declared maximum page size
     * @return an unsorted {@link PageRequest} for {@code (page, size)}
     * @throws IllegalArgumentException when any argument is out of range
     */
    public static PageRequest clamp(int page, int size, int maxSize) {
        if (maxSize < 1 || maxSize > ABSOLUTE_MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "maxSize must be within [1, " + ABSOLUTE_MAX_PAGE_SIZE + "]: maxSize=" + maxSize);
        }
        if (page < 0) {
            throw new IllegalArgumentException(
                    "page must be >= 0 (offset pagination is zero-based): page=" + page);
        }
        if (size < 1 || size > maxSize) {
            throw new IllegalArgumentException(
                    "size must be within [1, " + maxSize + "]: size=" + size
                            + " (server never honors unlimited requests)");
        }
        return PageRequest.of(page, size);
    }

    /**
     * Append the {@value #TIEBREAKER_PROPERTY} tiebreaker to a primary sort so
     * the total order is deterministic across page boundaries (PAGE-STABLE-SORT-001).
     *
     * <p>If the caller's sort already orders by {@value #TIEBREAKER_PROPERTY},
     * it is returned unchanged (no duplicate tiebreaker). The appended
     * tiebreaker is ascending — direction is irrelevant to stability, only its
     * presence matters, and ASC keeps the order predictable. A {@code null} or
     * empty primary sort yields a bare {@value #TIEBREAKER_PROPERTY}-ascending
     * sort.
     *
     * @param primary the caller's intended sort (may be {@code null} / unsorted)
     * @return a sort guaranteed to include the {@value #TIEBREAKER_PROPERTY} tiebreaker
     */
    public static Sort stableSort(Sort primary) {
        if (primary == null || primary.isUnsorted()) {
            return Sort.by(Sort.Order.asc(TIEBREAKER_PROPERTY));
        }
        boolean alreadyHasTiebreaker = primary.stream()
                .anyMatch(order -> TIEBREAKER_PROPERTY.equals(order.getProperty()));
        if (alreadyHasTiebreaker) {
            return primary;
        }
        return primary.and(Sort.by(Sort.Order.asc(TIEBREAKER_PROPERTY)));
    }
}
