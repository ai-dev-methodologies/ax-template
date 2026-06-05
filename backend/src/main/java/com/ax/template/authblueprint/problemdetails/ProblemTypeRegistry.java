package com.ax.template.authblueprint.problemdetails;

import java.net.URI;
import java.util.Map;
import java.util.Set;

/**
 * problem-details-l0 PROBLEM-TYPE-001 — the CLOSED registry of every problem
 * {@code type} this reference surface can emit.
 *
 * <p>Each distinct problem condition owns a stable, dereferenceable {@code https}
 * {@code type} URI keyed by a short slug. The slug is the bounded-cardinality label
 * used by {@link ProblemMetrics} (NOT the full URI, NOT free-text title); the URI is
 * the machine identifier clients branch on. Because the set is closed and enumerated
 * here, both the metric label space and the "every emitted type is documented"
 * invariant are mechanically verifiable.
 *
 * <p>Mirrors the IANA HTTP Problem Type registry shape
 * (https://www.iana.org/assignments/http-problem-types/). Anchored to RFC 9457 §3.1.1:
 * "If the type URI is a locator (e.g., those with an 'http' or 'https' scheme),
 * dereferencing it SHOULD provide human-readable documentation for the problem type."
 *
 * <p>Spec: specs/problem-details-l0.yaml#PROBLEM-TYPE-001.
 */
public final class ProblemTypeRegistry {

    private static final String BASE = "https://errors.example.com/";

    /** Distinct slug per condition — reusing one slug for two conditions is a TYPE-001 violation. */
    public static final String INSUFFICIENT_FUNDS = "insufficient-funds";
    public static final String VALIDATION = "validation";
    public static final String SERVER_ERROR = "server-error";

    /** slug → stable dereferenceable type URI. Closed set. */
    private static final Map<String, URI> TYPES = Map.of(
            INSUFFICIENT_FUNDS, URI.create(BASE + INSUFFICIENT_FUNDS),
            VALIDATION, URI.create(BASE + VALIDATION),
            SERVER_ERROR, URI.create(BASE + SERVER_ERROR));

    private ProblemTypeRegistry() {}

    /** The enumerated slug set — the bounded label space for {@code problem_type}. */
    public static Set<String> slugs() {
        return TYPES.keySet();
    }

    /** Stable type URI for a registered slug. */
    public static URI uri(String slug) {
        URI uri = TYPES.get(slug);
        if (uri == null) {
            // A type emitted but not enumerated here is itself a TYPE-001 violation; fail loud.
            throw new IllegalArgumentException("unregistered problem type slug: " + slug);
        }
        return uri;
    }

    /** True when the slug is part of the closed registry (used to bound metric cardinality). */
    public static boolean isRegistered(String slug) {
        return TYPES.containsKey(slug);
    }
}
