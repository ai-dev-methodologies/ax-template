package com.ax.template.authblueprint.common;

import java.util.List;

import com.ax.template.authblueprint.common.PageEnvelope.Pagination;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit coverage for {@link PageEnvelope} + {@link OffsetPageSupport} — closes the
 * zero-code gap of the {@code pagination-l0} spec PAGE-OFFSET-001 /
 * PAGE-LIMIT-001 / PAGE-STABLE-SORT-001 (specs/pagination-l0.yaml).
 *
 * <p>Pins the canonical envelope shape the IDW2 dogfood proved every domain
 * diverged from: {@code {data, pagination:{page,pageSize,totalElements,
 * totalPages,hasMore}}}. Framework-clean enough to run under the default
 * {@code test} task (uses Spring Data's {@link PageImpl} value type only — no
 * application context boots).
 */
@Tag("COMMON_PAGE_ENVELOPE")
class PageEnvelopeTest {

    // ─── from(Page) field correctness (PAGE-OFFSET-001) ───────────────────

    @Test
    void from_mapsContentAndFillsPaginationFromSpringPage() {
        // 137 total rows, page 1 (zero-based) of size 20 → 7 pages, has next.
        List<String> slice = List.of("e21", "e22", "e23");
        Page<String> springPage = new PageImpl<>(
                slice,
                PageRequest.of(1, 20, Sort.by(Sort.Direction.DESC, "createdAt")),
                137L);

        PageEnvelope<Integer> envelope = PageEnvelope.from(springPage, s -> Integer.parseInt(s.substring(1)));

        // data is the MAPPED content, in order
        assertThat(envelope.data()).containsExactly(21, 22, 23);

        Pagination p = envelope.pagination();
        assertThat(p.page()).isEqualTo(1);
        assertThat(p.pageSize()).isEqualTo(20);
        assertThat(p.totalElements()).isEqualTo(137L);
        assertThat(p.totalPages()).isEqualTo(7);     // ceil(137 / 20)
        assertThat(p.hasMore()).isTrue();            // page 1 of 0..6 → next exists
    }

    @Test
    void from_lastPageReportsNoMore() {
        // page 6 (zero-based) is the final page of 7 (indices 0..6).
        Page<String> lastPage = new PageImpl<>(
                List.of("tail"),
                PageRequest.of(6, 20),
                137L);

        PageEnvelope<String> envelope = PageEnvelope.from(lastPage, s -> s);

        assertThat(envelope.pagination().page()).isEqualTo(6);
        assertThat(envelope.pagination().totalPages()).isEqualTo(7);
        assertThat(envelope.pagination().hasMore()).isFalse();
    }

    @Test
    void from_emptyResultYieldsEmptyDataAndZeroTotals() {
        Page<String> empty = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0L);

        PageEnvelope<String> envelope = PageEnvelope.from(empty, s -> s);

        assertThat(envelope.data()).isEmpty();
        assertThat(envelope.pagination().totalElements()).isZero();
        assertThat(envelope.pagination().totalPages()).isZero();
        assertThat(envelope.pagination().hasMore()).isFalse();
    }

    @Test
    void from_rejectsNullPageOrMapper() {
        Page<String> page = new PageImpl<>(List.of("a"));
        assertThatThrownBy(() -> PageEnvelope.from(null, s -> s))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PageEnvelope.from(page, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ─── of() non-Spring overload (PAGE-OFFSET-001) ───────────────────────

    @Test
    void of_derivesTotalPagesAndHasMoreLikeSpring() {
        // 45 total, page 0 of size 20 → 3 pages, has next.
        PageEnvelope<String> envelope = PageEnvelope.of(List.of("a", "b"), 0, 20, 45L);

        Pagination p = envelope.pagination();
        assertThat(envelope.data()).containsExactly("a", "b");
        assertThat(p.page()).isZero();
        assertThat(p.pageSize()).isEqualTo(20);
        assertThat(p.totalElements()).isEqualTo(45L);
        assertThat(p.totalPages()).isEqualTo(3);     // ceil(45 / 20)
        assertThat(p.hasMore()).isTrue();            // (0+1)*20 = 20 < 45
    }

    @Test
    void of_lastPartialPageReportsNoMore() {
        // 45 total, page 2 of size 20 → final page (rows 40..44), no next.
        PageEnvelope<String> envelope = PageEnvelope.of(List.of("last"), 2, 20, 45L);

        assertThat(envelope.pagination().totalPages()).isEqualTo(3);
        assertThat(envelope.pagination().hasMore()).isFalse();  // (2+1)*20 = 60 !< 45
    }

    @Test
    void of_exactMultipleHasNoExtraPage() {
        // 40 total, size 20 → exactly 2 pages, page 1 is last.
        assertThat(PageEnvelope.of(List.of("x"), 1, 20, 40L).pagination().totalPages()).isEqualTo(2);
        assertThat(PageEnvelope.of(List.of("x"), 1, 20, 40L).pagination().hasMore()).isFalse();
    }

    @Test
    void of_zeroPageSizeYieldsZeroTotalPages() {
        // Guard against divide-by-zero in the ceil derivation.
        PageEnvelope<String> envelope = PageEnvelope.of(List.of(), 0, 0, 0L);
        assertThat(envelope.pagination().totalPages()).isZero();
        assertThat(envelope.pagination().hasMore()).isFalse();
    }

    @Test
    void of_copiesDataDefensively() {
        // A mutation of the caller's list must not leak into the envelope.
        List<String> src = new java.util.ArrayList<>(List.of("a", "b"));
        PageEnvelope<String> envelope = PageEnvelope.of(src, 0, 20, 2L);
        src.add("mutated");
        assertThat(envelope.data()).containsExactly("a", "b");
    }

    @Test
    void of_rejectsNullDataOrNegativeCoordinates() {
        assertThatThrownBy(() -> PageEnvelope.of(null, 0, 20, 0L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PageEnvelope.of(List.of(), -1, 20, 0L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PageEnvelope.of(List.of(), 0, -1, 0L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PageEnvelope.of(List.of(), 0, 20, -1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ─── OffsetPageSupport.clamp bounds (PAGE-LIMIT-001) ──────────────────

    @Test
    void clamp_acceptsInRangeRequest() {
        PageRequest req = OffsetPageSupport.clamp(2, 50, 200);
        assertThat(req.getPageNumber()).isEqualTo(2);
        assertThat(req.getPageSize()).isEqualTo(50);
        assertThat(req.getSort().isUnsorted()).isTrue();  // sort is chained separately
    }

    @Test
    void clamp_acceptsBoundaryValues() {
        // size == 1 (lower) and size == maxSize (upper) are both in range.
        assertThat(OffsetPageSupport.clamp(0, 1, 200).getPageSize()).isEqualTo(1);
        assertThat(OffsetPageSupport.clamp(0, 200, 200).getPageSize()).isEqualTo(200);
    }

    @Test
    void clamp_rejectsNegativePage() {
        assertThatThrownBy(() -> OffsetPageSupport.clamp(-1, 20, 200))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("page");
    }

    @Test
    void clamp_rejectsZeroOrNegativeSize() {
        // size <= 0 is the "unlimited" smell PAGE-LIMIT-001 forbids → 400.
        assertThatThrownBy(() -> OffsetPageSupport.clamp(0, 0, 200))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size");
        assertThatThrownBy(() -> OffsetPageSupport.clamp(0, -5, 200))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void clamp_rejectsSizeAboveMax() {
        assertThatThrownBy(() -> OffsetPageSupport.clamp(0, 201, 200))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size");
    }

    @Test
    void clamp_rejectsMaxSizeAboveAbsoluteCeiling() {
        assertThatThrownBy(() -> OffsetPageSupport.clamp(0, 20, OffsetPageSupport.ABSOLUTE_MAX_PAGE_SIZE + 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxSize");
    }

    // ─── OffsetPageSupport.stableSort tiebreaker (PAGE-STABLE-SORT-001) ────

    @Test
    void stableSort_appendsIdTiebreaker() {
        Sort stable = OffsetPageSupport.stableSort(Sort.by(Sort.Direction.DESC, "createdAt"));
        List<String> properties = stable.stream().map(Sort.Order::getProperty).toList();
        assertThat(properties).containsExactly("createdAt", "id");
    }

    @Test
    void stableSort_nullOrUnsortedYieldsBareIdSort() {
        assertThat(OffsetPageSupport.stableSort(null).stream().map(Sort.Order::getProperty).toList())
                .containsExactly("id");
        assertThat(OffsetPageSupport.stableSort(Sort.unsorted()).stream().map(Sort.Order::getProperty).toList())
                .containsExactly("id");
    }

    @Test
    void stableSort_doesNotDuplicateExistingIdTiebreaker() {
        Sort already = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id"));
        Sort stable = OffsetPageSupport.stableSort(already);
        List<String> properties = stable.stream().map(Sort.Order::getProperty).toList();
        assertThat(properties).containsExactly("createdAt", "id");  // no second "id"
    }
}
