package com.ax.template.authblueprint.facetcount;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * FACET-ALLOWLIST-002 — the compile-time facet-by field allowlist, composing the
 * {@code query-field-allowlist-l0} discipline ({@code QueryFieldAllowlist}) for the
 * AGGREGATE path instead of the row Sort/Specification path: a public field name maps to
 * an internal entity property, and a name outside this map is unrepresentable in a facet
 * request. {@code ownerId} (the caller-visibility scope column, FACET-COUNT-001) is
 * deliberately absent — the keystone target a caller must not be able to facet on.
 *
 * <p>Stateless value component — no entity, no repository, no Spring annotation.
 */
public final class FacetFieldAllowlist {

    private static final Map<String, String> FACETABLE = buildFacetable();

    private FacetFieldAllowlist() {}

    private static Map<String, String> buildFacetable() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("category", "category");
        map.put("status", "status");
        return Map.copyOf(map);
    }

    /** The declared PUBLIC facetable field names (for the 422 detail / introspection). */
    public static Set<String> allowed() {
        return FACETABLE.keySet();
    }

    /**
     * Resolve a client-supplied PUBLIC facet field name to its internal entity property.
     * Throws {@link FacetCountException} BEFORE any repository access if the field is not
     * in the allowlist — the raw client string never selects or builds an aggregation query.
     */
    public static String resolve(String publicField) {
        String internal = FACETABLE.get(publicField);
        if (internal == null) {
            throw FacetCountException.notAllowed(publicField, FACETABLE.keySet());
        }
        return internal;
    }
}
