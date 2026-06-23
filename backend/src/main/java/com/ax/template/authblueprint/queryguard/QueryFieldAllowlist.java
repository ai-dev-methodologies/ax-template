package com.ax.template.authblueprint.queryguard;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Sort;

/**
 * query-field-allowlist-l0 — the reusable per-resource ALLOWLIST primitive (the only
 * fields a caller may sort or filter on). A list/search endpoint that forwards a
 * client-supplied sort/filter field name unchecked into a {@code Sort.by(...)} or a
 * {@code Specification} is an injection (CWE-89) + property-enumeration
 * (OWASP API3:2023) + IDOR (CWE-639) surface at once. This component closes all three
 * BY CONSTRUCTION: a field the resource did not declare is unrepresentable in a built
 * Sort/predicate — the raw client string is never the thing handed to JPA.
 *
 * <p>A resource declares ONE binding mapping each PUBLIC field name (the name the client
 * uses) to the INTERNAL entity property (the name the persistence layer uses), separately
 * for sortable and filterable fields (QUERY-ALLOWLIST-MAPPING-001). The internal property
 * name is NEVER required from or revealed to the client.
 *
 * <h2>Usage sketch (the queryguard reference workload's CatalogItem binding)</h2>
 * <pre>{@code
 * QueryFieldAllowlist allowlist = QueryFieldAllowlist.builder()
 *     .sortable("name", "name")
 *     .sortable("createdAt", "createdAt")
 *     .sortable("status", "status")
 *     .sortable("priceMinor", "priceMinor")
 *     .filterable("name", "name")
 *     .filterable("status", "status")
 *     .filterable("priceMinor", "priceMinor")
 *     .build();
 *
 * Sort sort = allowlist.toSort("name", "asc");     // 422 if 'name' not sortable / 'asc' not a direction
 * String prop = allowlist.filterProperty("status"); // 422 if 'status' not filterable
 * }</pre>
 *
 * <p>This is a STATELESS value component — no entity, no repository, no Spring annotation;
 * it is constructed per resource and consulted before any persistence access. The maps are
 * insertion-ordered (LinkedHashMap) so the 422 detail lists fields in declaration order.
 */
public final class QueryFieldAllowlist {

    private final Map<String, String> sortable;    // public field name → internal entity property
    private final Map<String, String> filterable;  // public field name → internal entity property

    private QueryFieldAllowlist(Map<String, String> sortable, Map<String, String> filterable) {
        this.sortable = Map.copyOf(sortable);
        this.filterable = Map.copyOf(filterable);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** The declared PUBLIC sortable field names (for the 422 detail / introspection). */
    public Set<String> sortableFields() {
        return sortable.keySet();
    }

    /** The declared PUBLIC filterable field names (for the 422 detail / introspection). */
    public Set<String> filterableFields() {
        return filterable.keySet();
    }

    /**
     * QUERY-ALLOWLIST-SORT-001 / MAPPING-001 — build a Spring Data {@link Sort} from a
     * client-supplied PUBLIC field name + direction token. The field MUST be in the
     * sortable allowlist (else 422 QUERY_FIELD_NOT_SORTABLE naming the field) and the
     * direction MUST parse to {@code asc}/{@code desc} (else 422 QUERY_DIRECTION_INVALID).
     * The Sort is built ONLY from the mapped internal property — the raw client string
     * never reaches {@code Sort.by(...)}.
     */
    public Sort toSort(String publicField, String directionToken) {
        String internalProperty = sortable.get(publicField);
        if (internalProperty == null) {
            throw QueryGuardException.notSortable(publicField, sortable.keySet());
        }
        SortDirection direction = SortDirection.parse(directionToken)
            .orElseThrow(() -> QueryGuardException.directionInvalid(directionToken));
        Sort.Direction springDirection =
            direction == SortDirection.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(springDirection, internalProperty);
    }

    /**
     * QUERY-ALLOWLIST-FILTER-001 / MAPPING-001 — resolve a client-supplied PUBLIC filter
     * field name to its internal entity property. The field MUST be in the filterable
     * allowlist (else 422 QUERY_FIELD_NOT_FILTERABLE naming the field). The returned
     * property is then used to build a parameter-bound predicate — never concatenated
     * into a query string (CWE-89).
     */
    public String filterProperty(String publicField) {
        String internalProperty = filterable.get(publicField);
        if (internalProperty == null) {
            throw QueryGuardException.notFilterable(publicField, filterable.keySet());
        }
        return internalProperty;
    }

    /** Fluent builder so a resource declares its bindings in one readable block. */
    public static final class Builder {
        private final Map<String, String> sortable = new LinkedHashMap<>();
        private final Map<String, String> filterable = new LinkedHashMap<>();

        public Builder sortable(String publicField, String internalProperty) {
            sortable.put(publicField, internalProperty);
            return this;
        }

        public Builder filterable(String publicField, String internalProperty) {
            filterable.put(publicField, internalProperty);
            return this;
        }

        public QueryFieldAllowlist build() {
            return new QueryFieldAllowlist(sortable, filterable);
        }
    }
}
