package com.ax.template.authblueprint.common;

/**
 * Cross-cutting optimistic-locking helper — ships the REAL reusable code for the
 * {@code optimistic-locking-l0} catalog spec (specs/optimistic-locking-l0.yaml).
 *
 * <p>The spec's contract (RFC 7232 conditional requests + RFC 6585 §3 428 +
 * RFC 9110 §15.5.13 412 / §15.5.10 409 + RFC 9457 Problem Details + Jakarta
 * Persistence 3.1 {@code @Version}) was previously prose-only: every dogfood
 * persona hand-rolled the ETag/If-Match plumbing and 2/3 hit the SAME bug —
 * they called {@code repository.save(entity)} and read {@code entity.getVersion()}
 * for the response ETag while the row was still flushed lazily, so the version
 * was still {@code 0} and the emitted ETag was stale. This helper centralises
 * the derivation and the precondition decision so callers cannot repeat that.
 *
 * <h2>saveAndFlush requirement (the bug this helper exists to kill)</h2>
 * The JPA {@code @Version} attribute is incremented by the persistence provider
 * <em>at flush time</em>, not when {@code save()} returns. If you derive the
 * response ETag from a version that has not been flushed, the client receives a
 * validator one generation behind the persisted state and its next conditional
 * write fails spuriously with 412. Therefore, after a mutation, callers MUST use
 * {@code repository.saveAndFlush(entity)} (or flush the {@code EntityManager})
 * BEFORE reading the version for {@link #etag(String, long)}. Anchored to Jakarta
 * Persistence 3.1 §3.4.2: "The version attribute is updated by the persistence
 * provider runtime when the object is written to the database."
 *
 * <h2>Usage sketch</h2>
 * <pre>{@code
 * // GET — emit the current strong validator
 * Product p = repository.findById(id).orElseThrow();
 * response.setHeader("ETag", OptimisticLockingSupport.etag(p.getId(), p.getVersion()));
 *
 * // PUT/PATCH/DELETE — enforce the precondition, then flush before deriving the new ETag
 * OptimisticLockingSupport.requireMatch(
 *     ifMatchHeader, current.getId(), current.getVersion());   // throws 428 / 412
 * current.apply(request);
 * Product saved = repository.saveAndFlush(current);            // <-- flush bumps @Version
 * response.setHeader("ETag", OptimisticLockingSupport.etag(saved.getId(), saved.getVersion()));
 * }</pre>
 *
 * <p>Framework-clean: no Spring, no JPA, no domain types — pure {@code String}/
 * {@code long} in, signal out. All methods are pure and side-effect-free.
 *
 * <h2>Global ProblemDetail mapping (no caller @ExceptionHandler needed)</h2>
 * The thrown signals are mapped to RFC 9457 {@code application/problem+json} by the
 * COMMON {@code GlobalProblemDetailAdvice} ({@code LOWEST_PRECEDENCE} fallback), so an
 * adopter inherits the mappings without writing any {@code @ExceptionHandler}:
 * <ul>
 *   <li>{@link PreconditionRequiredException} → {@code 428} (code {@code PRECONDITION_REQUIRED});</li>
 *   <li>{@link PreconditionFailedException} → {@code 412} (code {@code PRECONDITION_FAILED},
 *       carrying the {@code current_etag} member);</li>
 *   <li>{@code org.springframework.orm.ObjectOptimisticLockingFailureException} → {@code 409}
 *       (code {@code OPTIMISTIC_LOCK_CONFLICT}) for the concurrent-write race that surfaces at
 *       flush time after the {@code If-Match} check passed.</li>
 * </ul>
 * See {@link PreconditionRequiredException} / {@link PreconditionFailedException} for the
 * canonical {@code type} URIs and status codes. A controller may still register its own
 * {@code @ExceptionHandler} to override the fallback, but it is no longer required.
 */
public final class OptimisticLockingSupport {

    private OptimisticLockingSupport() {}

    /**
     * RFC 9457 {@code type} URI for the 428 (If-Match absent) signal.
     * Matches spec OPTLOCK-IFMATCH-001: {@code urn:problem:precondition-required}.
     */
    public static final String TYPE_PRECONDITION_REQUIRED = "urn:problem:precondition-required";

    /**
     * RFC 9457 {@code type} URI for the 412 (stale validator) signal.
     * Matches spec OPTLOCK-CONFLICT-001: {@code urn:problem:precondition-failed}.
     */
    public static final String TYPE_PRECONDITION_FAILED = "urn:problem:precondition-failed";

    /**
     * Derive the strong ETag for a versioned resource — deterministic from
     * {@code (resourceId, version)} only, so identical state yields a
     * byte-identical validator and any version bump changes it
     * (spec OPTLOCK-ETAG-001). The result is a strong validator (NO {@code W/}
     * weak prefix) and is quoted per the RFC 7232 §2.3 entity-tag grammar:
     * {@code "<id>-<version>"}.
     *
     * @param resourceId the resource identifier (entity id); must be non-null
     * @param version    the JPA {@code @Version} value of the current state
     * @return a quoted strong ETag, e.g. {@code "42-7"}
     */
    public static String etag(String resourceId, long version) {
        if (resourceId == null || resourceId.isBlank()) {
            throw new IllegalArgumentException("resourceId must be non-blank to derive an ETag");
        }
        return "\"" + resourceId + "-" + version + "\"";
    }

    /**
     * Convenience overload for numeric resource ids.
     *
     * @see #etag(String, long)
     */
    public static String etag(long resourceId, long version) {
        return etag(Long.toString(resourceId), version);
    }

    /**
     * Parse the value of an {@code If-Match} request header into the bare
     * entity-tag, stripping the surrounding RFC 7232 quotes (and a leading
     * {@code W/} weak marker, which is invalid for strong-comparison
     * preconditions and is therefore discarded so a weak validator never
     * spuriously matches a strong one).
     *
     * <p>Returns {@code null} when the header is absent / blank so the caller
     * can distinguish "no precondition supplied" (→ 428) from "precondition
     * present but stale" (→ 412).
     *
     * @param ifMatchHeader the raw {@code If-Match} header value (may be null)
     * @return the unquoted entity-tag, {@code "*"} for the wildcard, or
     *         {@code null} when absent
     */
    public static String parseIfMatch(String ifMatchHeader) {
        if (ifMatchHeader == null) {
            return null;
        }
        String trimmed = ifMatchHeader.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.equals("*")) {
            return "*";
        }
        // Discard a weak-validator prefix: "W/\"abc\"" → strong comparison can never match it.
        if (trimmed.startsWith("W/")) {
            trimmed = trimmed.substring(2).trim();
        }
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        // Tolerate an unquoted token (lenient inbound parse); compared by value below.
        return trimmed;
    }

    /**
     * Decide the precondition outcome for a mutation (PUT / PATCH / DELETE) on a
     * versioned resource, per spec OPTLOCK-IFMATCH-001 + OPTLOCK-CONFLICT-001:
     *
     * <ul>
     *   <li>{@code If-Match} absent → {@link Decision#MISSING} (caller emits 428).</li>
     *   <li>supplied validator does not equal the current strong ETag →
     *       {@link Decision#STALE} (caller emits 412 with the authoritative
     *       {@code current_etag}).</li>
     *   <li>supplied validator equals the current ETag (or wildcard, which this
     *       helper treats as no-match by default — see {@link #decide(String, String, long)}
     *       which rejects {@code *} as stale unless the caller opts in) →
     *       {@link Decision#MATCHED} (caller proceeds with the write).</li>
     * </ul>
     *
     * <p>This method does NOT throw — it returns a value so the caller can
     * branch. Use {@link #requireMatch(String, String, long)} when you want the
     * helper to raise the mapped signals directly.
     *
     * @param ifMatchHeader the raw {@code If-Match} header value (may be null)
     * @param resourceId    the resource identifier
     * @param currentVersion the current JPA {@code @Version} value
     * @return the precondition {@link PreconditionOutcome}
     */
    public static PreconditionOutcome decide(String ifMatchHeader, String resourceId, long currentVersion) {
        String currentEtag = etag(resourceId, currentVersion);
        String supplied = parseIfMatch(ifMatchHeader);
        if (supplied == null) {
            return new PreconditionOutcome(Decision.MISSING, currentEtag);
        }
        // Wildcard is NOT honoured by default (spec OPTLOCK-IFMATCH-001:
        // optlock_allow_wildcard_ifmatch defaults false). A "*" therefore
        // does not match a concrete validator → treated as stale.
        if (currentEtag.equals(quote(supplied))) {
            return new PreconditionOutcome(Decision.MATCHED, currentEtag);
        }
        return new PreconditionOutcome(Decision.STALE, currentEtag);
    }

    /**
     * Enforce the precondition, raising the RFC-mapped signal on failure:
     * {@link PreconditionRequiredException} (428) when {@code If-Match} is
     * absent, {@link PreconditionFailedException} (412, carrying the current
     * ETag) when the supplied validator is stale. Returns silently when the
     * validator matches so the caller may proceed with the write.
     *
     * @param ifMatchHeader  the raw {@code If-Match} header value (may be null)
     * @param resourceId     the resource identifier
     * @param currentVersion the current JPA {@code @Version} value
     * @throws PreconditionRequiredException when {@code If-Match} is absent (428)
     * @throws PreconditionFailedException   when the validator is stale (412)
     */
    public static void requireMatch(String ifMatchHeader, String resourceId, long currentVersion) {
        PreconditionOutcome outcome = decide(ifMatchHeader, resourceId, currentVersion);
        switch (outcome.decision()) {
            case MISSING -> throw new PreconditionRequiredException(outcome.currentEtag());
            case STALE -> throw new PreconditionFailedException(outcome.currentEtag());
            case MATCHED -> { /* proceed */ }
        }
    }

    /** Re-quote a bare entity-tag for value comparison against {@link #etag(String, long)}. */
    private static String quote(String bareTag) {
        return "\"" + bareTag + "\"";
    }

    /** Precondition decision discriminant (spec OPTLOCK-IFMATCH-001 / OPTLOCK-CONFLICT-001). */
    public enum Decision {
        /** {@code If-Match} header absent → 428 Precondition Required. */
        MISSING,
        /** Supplied validator does not match the current state → 412 Precondition Failed. */
        STALE,
        /** Supplied validator matches the current state → proceed with the write. */
        MATCHED
    }

    /**
     * Immutable result of {@link #decide(String, String, long)} carrying the
     * decision plus the authoritative current ETag (needed for the 412 body's
     * {@code current_etag} member so the client can re-GET, merge, and retry —
     * spec OPTLOCK-RETRY-001).
     */
    public record PreconditionOutcome(Decision decision, String currentEtag) {}

    /**
     * 428 Precondition Required signal (RFC 6585 §3) — raised when a mutation
     * arrives without an {@code If-Match} header. The COMMON
     * {@code GlobalProblemDetailAdvice} maps this to an RFC 9457 ProblemDetail with
     * {@code type=}{@link #TYPE_PRECONDITION_REQUIRED} (no caller handler required).
     */
    public static final class PreconditionRequiredException extends RuntimeException {
        private final String currentEtag;

        public PreconditionRequiredException(String currentEtag) {
            super("If-Match precondition required for this mutation");
            this.currentEtag = currentEtag;
        }

        /** The authoritative current ETag the client should adopt before retrying. */
        public String currentEtag() {
            return currentEtag;
        }
    }

    /**
     * 412 Precondition Failed signal (RFC 9110 §15.5.13) — raised when the
     * supplied {@code If-Match} validator is stale relative to the current
     * resource version. The COMMON {@code GlobalProblemDetailAdvice} maps this to an
     * RFC 9457 ProblemDetail with {@code type=}{@link #TYPE_PRECONDITION_FAILED} and a
     * {@code current_etag} member so the client can read-modify-write retry
     * (spec OPTLOCK-RETRY-001) — no caller handler required.
     */
    public static final class PreconditionFailedException extends RuntimeException {
        private final String currentEtag;

        public PreconditionFailedException(String currentEtag) {
            super("If-Match validator is stale; resource was modified");
            this.currentEtag = currentEtag;
        }

        /** The authoritative current ETag carried into the 412 body. */
        public String currentEtag() {
            return currentEtag;
        }
    }
}
