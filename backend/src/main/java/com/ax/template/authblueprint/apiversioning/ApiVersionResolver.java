package com.ax.template.authblueprint.apiversioning;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * The SINGLE version-negotiation mechanism (VERSION-NEGOTIATION-001 notes: "Implement via a single
 * resolver; do not scatter version branching through controllers"). It implements the catalog-default
 * {@code url-path} strategy: the major version is the first path segment ({@code /v1/...},
 * {@code /v2/...}). It also:
 * <ul>
 *   <li>rejects a malformed version token → {@link ApiVersioningException.Kind#MALFORMED} (400);</li>
 *   <li>rejects mixing a path version with a conflicting {@code X-API-Version} header →
 *       {@link ApiVersioningException.Kind#CONFLICT} (400) — never silently resolved;</li>
 *   <li>rejects a syntactically valid but unsupported version under url-path →
 *       {@link ApiVersioningException.Kind#UNSUPPORTED} (404);</li>
 *   <li>resolves the deterministic default when no path version is present (VERSION-DEFAULT-001);</li>
 *   <li>returns 410 for a version past its sunset instant (VERSION-DEPRECATION-001);</li>
 *   <li>builds the {@code Deprecation} / {@code Sunset} / {@code Link} response headers (RFC 8594 +
 *       the Deprecation header draft) for a deprecated version (VERSION-DEPRECATION/MIGRATION-001).</li>
 * </ul>
 *
 * <p>Spec: specs/api-versioning-l0.yaml.
 */
@Component
public class ApiVersionResolver {

    /** A well-formed url-path major token: lowercase 'v' + a positive integer with no leading zero. */
    private static final Pattern VERSION_TOKEN = Pattern.compile("v(0|[1-9][0-9]*)");
    /** RFC 8594 Sunset / RFC 9110 §5.6.7 HTTP-date is an IMF-fixdate (RFC 1123, GMT). */
    private static final DateTimeFormatter IMF_FIXDATE =
            DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC);

    private final ApiVersionCatalog catalog;
    private final ApiVersioningMetrics metrics;

    public ApiVersionResolver(ApiVersionCatalog catalog, ApiVersioningMetrics metrics) {
        this.catalog = catalog;
        this.metrics = metrics;
    }

    /**
     * Resolve the {@link ApiVersionCatalog.Version} a request gets.
     *
     * @param pathToken     the first-path-segment version token ({@code "v1"}, {@code "v2"}), or null
     *                      when the client supplied no selector (default-policy path).
     * @param conflictingHeader the {@code X-API-Version} header value if present, else null — a value
     *                      here while {@code pathToken} is also present is a strategy conflict.
     */
    public ApiVersionCatalog.Version resolve(String pathToken, String conflictingHeader) {
        if (pathToken == null) {
            // VERSION-DEFAULT-001 — no selector → deterministic default. A stray X-API-Version with no
            // path version is still a second strategy; reject rather than silently honour it.
            if (conflictingHeader != null) {
                throw new ApiVersioningException(ApiVersioningException.Kind.CONFLICT,
                        "url-path is the active strategy; do not also send X-API-Version");
            }
            return catalog.resolveDefault();
        }

        if (!VERSION_TOKEN.matcher(pathToken).matches()) {
            throw new ApiVersioningException(ApiVersioningException.Kind.MALFORMED,
                    "malformed API version token");
        }
        int major = Integer.parseInt(pathToken.substring(1));

        // VERSION-NEGOTIATION-001 — mixing two strategies (path /vN + X-API-Version) MUST be rejected.
        if (conflictingHeader != null) {
            String trimmed = conflictingHeader.trim();
            boolean sameMajor = trimmed.equals(String.valueOf(major)) || trimmed.equals(pathToken);
            if (!sameMajor) {
                throw new ApiVersioningException(ApiVersioningException.Kind.CONFLICT,
                        "conflicting API version selectors in one request");
            }
        }

        ApiVersionCatalog.Version v = catalog.byMajor(major)
                .orElseThrow(() -> new ApiVersioningException(ApiVersioningException.Kind.UNSUPPORTED,
                        "unsupported API version"));

        // VERSION-DEPRECATION-001 — a version past its sunset instant MAY return 410 Gone.
        if (v.isSunsetExpired(Instant.now())) {
            metrics.sunsetBreach(v.label());
            throw new ApiVersioningException(ApiVersioningException.Kind.SUNSET,
                    "this API version has been sunset");
        }
        return v;
    }

    /**
     * Record the per-version observability counters and return the response headers that MUST/SHOULD
     * accompany a served response: always {@code X-API-Version} (so the client observes what it got —
     * VERSION-DEFAULT-001); for a deprecated version additionally {@code Deprecation}, {@code Sunset}
     * (RFC 8594 IMF-fixdate), and a {@code Link} with relation {@code deprecation} pointing at the
     * version-specific migration guide (VERSION-DEPRECATION/MIGRATION-001).
     */
    public HttpHeaders responseHeaders(ApiVersionCatalog.Version v) {
        metrics.requestServed(v.label(), v.status());

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-API-Version", v.label());

        if (v.isDeprecated()) {
            metrics.deprecatedCall(v.label());
            // Deprecation header draft: a deprecated resource carries this field. We emit the deprecation
            // instant as an IMF-fixdate value (the draft permits an HTTP-date).
            headers.add("Deprecation", IMF_FIXDATE.format(v.deprecatedAt()));
            // RFC 8594 — the Sunset field is an HTTP-date at the planned unresponsive instant.
            headers.add("Sunset", IMF_FIXDATE.format(v.sunsetAt()));
            // The `deprecation` Link relation, reused for the migration guide (absolute, version-specific).
            headers.add(HttpHeaders.LINK,
                    "<" + v.migrationGuide() + ">; rel=\"deprecation\"; type=\"text/html\"");
        }
        return headers;
    }
}
