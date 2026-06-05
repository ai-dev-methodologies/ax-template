package com.ax.template.authblueprint.pagination;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;

/**
 * Pagination reference workload — a single in-memory collection paginated in both canonical modes,
 * verifying every pagination-l0 contract item: offset envelope (PAGE-OFFSET), bounded page size
 * (PAGE-LIMIT), RFC 5988 Link header (PAGE-LINK), opaque HMAC cursors (PAGE-CURSOR), stable-sort
 * rejection (PAGE-STABLE-SORT), opt-in count (PAGE-COUNT), and bounded-label metrics
 * (PAGE-OBSERVABILITY). Spec: specs/pagination-l0.yaml. Tenant = the authenticated principal.
 */
@RestController
@RequestMapping("/api/pagination/items")
public class PaginationDemoController {

    private static final int TOTAL = 50;
    private static final int MAX_PAGE_SIZE = 200;
    private static final long DRIFT_THRESHOLD = 10_000;

    public record Item(long id, String name) {}

    private static final List<Item> DATA =
        LongStream.rangeClosed(1, TOTAL).mapToObj(i -> new Item(i, "item-" + i)).toList();

    private final PaginationMetrics metrics;

    public PaginationDemoController(PaginationMetrics metrics) {
        this.metrics = metrics;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(defaultValue = "offset") String mode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(name = "include_total", defaultValue = "false") boolean includeTotal,
            Authentication auth) {
        long start = System.nanoTime();
        String tenant = auth.getName();

        // PAGE-STABLE-SORT-001 — a single non-unique column is rejected; sort must carry an id tiebreaker
        if (!sort.equals("id") && !sort.endsWith(",id")) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "PAGE-STABLE-SORT-001",
                "message", "sort '" + sort + "' is not stable — declare a composite sort with an id tiebreaker"));
        }
        // PAGE-LIMIT-001 — page size is bounded; out-of-range is a 400, never an unlimited read
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "PAGE_SIZE_INVALID",
                "message", "page_size must be in [1, " + MAX_PAGE_SIZE + "]"));
        }

        HttpHeaders headers = new HttpHeaders();
        Map<String, Object> body;

        if ("cursor".equals(mode)) {
            long decoded = 0L;
            if (cursor != null && !cursor.isBlank()) {
                try {
                    decoded = CursorCodec.decode(cursor).lastId();
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(Map.of("error", "PAGE-CURSOR-001", "message", e.getMessage()));
                }
            }
            final long lastId = decoded;
            List<Item> slice = DATA.stream().filter(i -> i.id() > lastId).limit(pageSize).toList();
            boolean hasMore = !slice.isEmpty() && slice.get(slice.size() - 1).id() < TOTAL;
            Map<String, Object> pg = new LinkedHashMap<>();
            pg.put("next_cursor", hasMore
                ? CursorCodec.encode(new CursorCodec.Cursor(slice.get(slice.size() - 1).id(), pageSize)) : null);
            pg.put("prev_cursor", lastId > 0
                ? CursorCodec.encode(new CursorCodec.Cursor(Math.max(0, lastId - pageSize), pageSize)) : null);
            pg.put("page_size", pageSize);
            pg.put("has_more", hasMore);
            if (includeTotal) {
                pg.put("total_count", (long) TOTAL);
            }
            body = Map.of("data", slice, "pagination", pg);
            if (hasMore) {
                headers.add(HttpHeaders.LINK, "</api/pagination/items?mode=cursor&cursor="
                    + pg.get("next_cursor") + "&page_size=" + pageSize + ">; rel=\"next\"");
            }
            metrics.request(tenant, "cursor");
        } else {
            if ((long) page * pageSize > DRIFT_THRESHOLD) {
                metrics.driftWarning(tenant); // PAGE-CURSOR-001 rationale: deep offset drifts — warn
            }
            int from = page * pageSize;
            int to = Math.min(from + pageSize, TOTAL);
            List<Item> slice = from < TOTAL ? DATA.subList(from, to) : List.of();
            int totalPages = (TOTAL + pageSize - 1) / pageSize;
            boolean hasMore = to < TOTAL;
            Map<String, Object> pg = new LinkedHashMap<>();
            pg.put("page", page);
            pg.put("pageSize", pageSize);
            pg.put("totalPages", totalPages);
            pg.put("hasMore", hasMore);
            if (includeTotal) {
                pg.put("totalElements", (long) TOTAL); // PAGE-COUNT-001 — opt-in only (count is O(table))
            }
            body = Map.of("data", slice, "pagination", pg);
            List<String> links = new ArrayList<>();
            if (hasMore) {
                links.add("</api/pagination/items?page=" + (page + 1) + "&page_size=" + pageSize + ">; rel=\"next\"");
            }
            if (page > 0) {
                links.add("</api/pagination/items?page=" + (page - 1) + "&page_size=" + pageSize + ">; rel=\"prev\"");
            }
            if (!links.isEmpty()) {
                headers.add(HttpHeaders.LINK, String.join(", ", links)); // PAGE-LINK-001 (RFC 5988)
            }
            metrics.request(tenant, "offset");
        }

        metrics.responseTime(tenant, mode, Duration.ofNanos(System.nanoTime() - start));
        return ResponseEntity.ok().headers(headers).body(body);
    }
}
