package com.ax.template.authblueprint.queryguard;

import com.ax.template.authblueprint.common.OffsetPageSupport;
import com.ax.template.authblueprint.common.PageEnvelope;

import jakarta.persistence.criteria.Expression;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * query-field-allowlist-l0 sole orchestrator. The list path NEVER hands a client-supplied
 * field name to JPA: it consults the per-resource {@link QueryFieldAllowlist} first, building
 * a Spring Data {@link Sort} and a {@link Specification} ONLY from the allowlisted INTERNAL
 * property the public field maps to (QUERY-ALLOWLIST-MAPPING-001) + a parameter-bound value.
 * A sort/filter naming a non-allowlisted field is a 422 that NAMES the offending field
 * (QUERY-ALLOWLIST-SORT/FILTER/KEYSTONE-001) — raised BEFORE any persistence access; the
 * query is never executed, and the raw string never reaches the Spring Data sort builder or a predicate.
 */
@Service
public class QueryGuardService {

    /** The CatalogItem resource's allowlist binding — its FOUR public fields are the entire
     *  sortable/filterable surface; every other property is unrepresentable in a sort/filter. */
    static final QueryFieldAllowlist CATALOG_ITEM_ALLOWLIST = QueryFieldAllowlist.builder()
        .sortable("name", "name")
        .sortable("createdAt", "createdAt")
        .sortable("status", "status")
        .sortable("priceMinor", "priceMinor")
        .filterable("name", "name")
        .filterable("status", "status")
        .filterable("priceMinor", "priceMinor")
        .build();

    static final int MAX_PAGE_SIZE = 100;
    static final String DEFAULT_SORT_FIELD = "createdAt";
    static final String DEFAULT_SORT_DIRECTION = "desc";

    private final CatalogItemRepository items;
    private final QueryGuardMetrics metrics;
    private final Clock clock;

    public QueryGuardService(CatalogItemRepository items, QueryGuardMetrics metrics, Clock clock) {
        this.items = items;
        this.metrics = metrics;
        this.clock = clock;
    }

    /** Seed a row (the controller's create path) — exercises the resource the list pages over. */
    @Transactional
    public CatalogItem create(String name, CatalogItemStatus status, long priceMinor, String internalNotes) {
        CatalogItem item = new CatalogItem(UUID.randomUUID(), name, status, priceMinor,
            internalNotes, Instant.now(clock));
        return items.save(item);
    }

    /**
     * QUERY-ALLOWLIST-SORT/FILTER/MAPPING/PAGE/KEYSTONE-001 — list CatalogItems applying a
     * client-supplied sort + optional filter, both bounded by {@link #CATALOG_ITEM_ALLOWLIST}.
     * The {@code sortField}/{@code direction}/{@code filter} are validated against the allowlist
     * BEFORE the query is built; any non-allowlisted field is a 422 (named field), never a
     * pass-through. The result is a bounded {@link PageEnvelope} over a stable sort.
     */
    @Transactional(readOnly = true)
    public PageEnvelope<CatalogItemDto> list(String sortField, String direction, String filter,
                                             int page, int size) {
        String effectiveField = (sortField == null || sortField.isBlank()) ? DEFAULT_SORT_FIELD : sortField;
        String effectiveDir = (direction == null || direction.isBlank()) ? DEFAULT_SORT_DIRECTION : direction;

        // QUERY-ALLOWLIST-SORT-001 — Sort built ONLY from the allowlisted internal property.
        Sort sort;
        try {
            sort = CATALOG_ITEM_ALLOWLIST.toSort(effectiveField, effectiveDir);
        } catch (QueryGuardException ex) {
            metrics.record("list", outcomeOf(ex));
            throw ex;
        }

        // QUERY-ALLOWLIST-FILTER-001 — Specification built ONLY from allowlisted property + bound value.
        Specification<CatalogItem> spec;
        try {
            spec = (filter == null || filter.isBlank()) ? null : buildFilterSpec(filter);
        } catch (QueryGuardException ex) {
            metrics.record("list", outcomeOf(ex));
            throw ex;
        }

        // QUERY-ALLOWLIST-PAGE-001 — clamped size + stable-sort tiebreaker + bounded PageEnvelope.
        PageRequest request = OffsetPageSupport.clamp(page, size, MAX_PAGE_SIZE)
            .withSort(OffsetPageSupport.stableSort(sort));
        Page<CatalogItem> result = items.findAll(spec, request);
        metrics.record("list", "ok");
        return PageEnvelope.from(result, CatalogItemDto::of);
    }

    /**
     * QUERY-ALLOWLIST-FILTER-001 — parse a {@code field:operator:value} expression and build a
     * parameter-bound predicate from the ALLOWLISTED internal property + a typed operator. The
     * field name is resolved through the allowlist (422 if not filterable); the operator parses
     * to the closed {@link FilterOperator} set (422 if unknown). The value is bound via the
     * criteria API — NEVER concatenated into a query string (CWE-89).
     */
    private Specification<CatalogItem> buildFilterSpec(String filter) {
        String[] parts = filter.split(":", 3);
        if (parts.length != 3) {
            throw QueryGuardException.filterMalformed(filter);
        }
        String publicField = parts[0];
        String operatorToken = parts[1];
        String rawValue = parts[2];

        String property = CATALOG_ITEM_ALLOWLIST.filterProperty(publicField);   // 422 if not filterable
        FilterOperator operator = FilterOperator.parse(operatorToken)
            .orElseThrow(() -> QueryGuardException.operatorInvalid(operatorToken));

        return (root, query, cb) -> {
            // EQ/NE accept any property type; the value is coerced to the property's Java type
            // and BOUND via the criteria API — never interpolated into a query string (CWE-89).
            if (operator == FilterOperator.EQ) {
                return cb.equal(root.get(property), coerce(property, rawValue));
            }
            if (operator == FilterOperator.NE) {
                return cb.notEqual(root.get(property), coerce(property, rawValue));
            }
            if (operator == FilterOperator.LIKE) {
                return cb.like(cb.lower(stringPath(root, property)),
                    "%" + rawValue.toLowerCase(Locale.ROOT) + "%");
            }
            // Ordered comparisons apply only to the numeric property (priceMinor); a gt/gte/lt/lte
            // on a non-ordered property is rejected as an invalid operator FOR THAT FIELD (422),
            // never a 500.
            if (!"priceMinor".equals(property)) {
                throw QueryGuardException.operatorInvalid(operatorToken);
            }
            Expression<Long> num = comparable(root, property);
            long value = parseLong(rawValue);
            return switch (operator) {
                case GT -> cb.greaterThan(num, value);
                case GTE -> cb.greaterThanOrEqualTo(num, value);
                case LT -> cb.lessThan(num, value);
                case LTE -> cb.lessThanOrEqualTo(num, value);
                default -> throw QueryGuardException.operatorInvalid(operatorToken); // unreachable
            };
        };
    }

    // ── typed value coercion: the value is bound, never interpolated; a bad value is a 422 (never 500) ──

    private Object coerce(String property, String rawValue) {
        return switch (property) {
            case "status" -> parseStatus(rawValue);
            case "priceMinor" -> parseLong(rawValue);
            default -> rawValue;   // name (String)
        };
    }

    private static CatalogItemStatus parseStatus(String rawValue) {
        try {
            return CatalogItemStatus.valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException bad) {
            throw QueryGuardException.valueInvalid(rawValue);
        }
    }

    private static long parseLong(String rawValue) {
        try {
            return Long.parseLong(rawValue.trim());
        } catch (NumberFormatException bad) {
            throw QueryGuardException.valueInvalid(rawValue);
        }
    }

    private Expression<Long> comparable(jakarta.persistence.criteria.Root<CatalogItem> root, String property) {
        return root.<Long>get(property);
    }

    private Expression<String> stringPath(jakarta.persistence.criteria.Root<CatalogItem> root, String property) {
        return root.<String>get(property);
    }

    private static String outcomeOf(QueryGuardException ex) {
        return switch (ex.code()) {
            case "QUERY_FIELD_NOT_SORTABLE" -> "not_sortable";
            case "QUERY_FIELD_NOT_FILTERABLE" -> "not_filterable";
            case "QUERY_DIRECTION_INVALID" -> "direction_invalid";
            case "QUERY_OPERATOR_INVALID" -> "operator_invalid";
            case "QUERY_FILTER_MALFORMED" -> "filter_malformed";
            case "QUERY_VALUE_INVALID" -> "value_invalid";
            default -> "invalid";
        };
    }

    /** Transport DTO — the FOUR exposed fields only; internalNotes is never carried out. */
    public record CatalogItemDto(UUID id, String name, CatalogItemStatus status, long priceMinor,
                                 Instant createdAt) {
        static CatalogItemDto of(CatalogItem c) {
            return new CatalogItemDto(c.getId(), c.getName(), c.getStatus(), c.getPriceMinor(),
                c.getCreatedAt());
        }
    }
}
