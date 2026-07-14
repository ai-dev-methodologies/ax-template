package com.ax.template.authblueprint.facetcount;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * facet-count-l0 sole orchestrator. {@link #facets} NEVER hands the client-supplied field name
 * to a dynamically-built query: {@link FacetFieldAllowlist#resolve} validates it FIRST
 * (422 FACET_FIELD_NOT_ALLOWED, fail-closed, before any repository access), then a fixed switch
 * selects ONE of two pre-written parameterized GROUP BY queries — both scoped to the caller's
 * OWN rows (ownerId), the identical scope the list endpoint would use (FACET-COUNT-001). The
 * bucket list is bounded top-K by count with an explicit otherCount remainder (FACET-BOUND-003).
 */
@Service
public class FacetCountService {

    /** FACET-BOUND-003 policy constant — at most this many buckets are ever returned. */
    static final int MAX_BUCKETS = 5;

    private final FacetableItemRepository items;
    private final FacetCountMetrics metrics;
    private final Clock clock;

    public FacetCountService(FacetableItemRepository items, FacetCountMetrics metrics, Clock clock) {
        this.items = items;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public FacetableItem create(String ownerId, String category, ItemStatus status) {
        FacetableItem item = new FacetableItem(UUID.randomUUID(), ownerId, category, status,
            Instant.now(clock));
        metrics.record("create", "ok");
        return items.save(item);
    }

    /**
     * FACET-COUNT/ALLOWLIST/BOUND-001..003 — bucket counts for {@code publicField}, scoped
     * to {@code ownerId}'s own rows. The field is resolved through the allowlist BEFORE any
     * query runs; the result is top-K by count plus an otherCount remainder such that
     * {@code Σ(bucket counts) + otherCount == total scoped row count}.
     */
    @Transactional(readOnly = true)
    public FacetCountResponse facets(String ownerId, String publicField) {
        String internal;
        try {
            internal = FacetFieldAllowlist.resolve(publicField);   // 422 before any repository access
        } catch (FacetCountException ex) {
            metrics.record("facets", "not_allowed");
            throw ex;
        }

        List<Object[]> rows = switch (internal) {
            case "category" -> items.countsByCategoryForOwner(ownerId);
            case "status" -> items.countsByStatusForOwner(ownerId);
            default -> throw FacetCountException.notAllowed(publicField, FacetFieldAllowlist.allowed());
        };

        List<FacetBucket> buckets = new ArrayList<>();
        for (Object[] row : rows) {
            buckets.add(new FacetBucket(String.valueOf(row[0]), (Long) row[1]));
        }
        buckets.sort(Comparator.comparingLong(FacetBucket::count).reversed());

        long total = items.countByOwnerId(ownerId);
        List<FacetBucket> topK = buckets.size() > MAX_BUCKETS ? buckets.subList(0, MAX_BUCKETS) : buckets;
        long returnedCount = topK.stream().mapToLong(FacetBucket::count).sum();
        long otherCount = total - returnedCount;                  // conservation: Σ(buckets) + otherCount == total

        metrics.record("facets", "ok");
        return new FacetCountResponse(publicField, List.copyOf(topK), otherCount);
    }

    public record FacetBucket(String value, long count) {}

    public record FacetCountResponse(String field, List<FacetBucket> buckets, long otherCount) {}
}
