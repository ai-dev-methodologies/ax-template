package com.ax.template.authblueprint.apiversioning;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

/**
 * The CLOSED, fixed advertised-version set for the api-versioning-l0 reference workload, plus the
 * configuration the spec's startup invariant (VERSION-MIGRATION-001) checks. The set is small and
 * fixed BY DESIGN — it is the bound that keeps the {@code version} metric label cardinality flat
 * (VERSION-OBSERVABILITY-001) and the discovery document finite (VERSION-DISCOVERY-001).
 *
 * <p>Catalog defaults (RECIPE.md frontmatter): {@code api_version_strategy: url-path} (the only
 * PRACTICES-API-003-aligned strategy), {@code api_version_default: latest-stable} (route an
 * un-versioned client to the newest non-deprecated major — VERSION-DEFAULT-001), and a
 * {@code api_version_deprecation_window_months} of 6 (minimum 3 — VERSION-MIGRATION-001).
 *
 * <p>The reference set: {@code v1} is DEPRECATED with a future sunset (exercises DEPRECATION +
 * MIGRATION + the deprecated discovery branch); {@code v2} is CURRENT and is what the
 * latest-stable default resolves to. Both serve normally (200) — a deprecated version is NOT a 4xx
 * until its sunset instant (VERSION-DEPRECATION-001).
 *
 * <p>Spec: specs/api-versioning-l0.yaml.
 */
@Component
public class ApiVersionCatalog {

    /** RFC-9457-aligned default migration window minimum (months). Shorter MUST fail at startup. */
    static final int MIN_DEPRECATION_WINDOW_MONTHS = 3;
    static final int DEPRECATION_WINDOW_MONTHS = 6;

    /** The deterministic default-version policy (VERSION-DEFAULT-001). latest-stable is the catalog default. */
    public enum DefaultPolicy { LATEST_STABLE, PINNED }

    public enum Status { CURRENT, DEPRECATED, SUNSET }

    /**
     * One advertised major version. For a deprecated version, {@code deprecatedAt} and {@code sunsetAt}
     * are present and {@code migrationGuide} is the absolute, version-specific guide URL.
     */
    public record Version(int major,
                          Status status,
                          Instant deprecatedAt,
                          Instant sunsetAt,
                          String migrationGuide) {

        public String label() {
            return "v" + major;
        }

        public boolean isDeprecated() {
            return status == Status.DEPRECATED;
        }

        public boolean isSunsetExpired(Instant now) {
            return sunsetAt != null && !now.isBefore(sunsetAt);
        }
    }

    private final List<Version> versions;
    private final DefaultPolicy defaultPolicy = DefaultPolicy.LATEST_STABLE;
    private final int pinnedMajor = 1;

    public ApiVersionCatalog() {
        Instant deprecatedAt = Instant.now().minus(7, ChronoUnit.DAYS);
        // 6-month window (catalog default) — comfortably past the 3-month minimum and never in the past.
        Instant sunsetAt = deprecatedAt.plus(DEPRECATION_WINDOW_MONTHS * 30L, ChronoUnit.DAYS);
        this.versions = List.of(
                new Version(1, Status.DEPRECATED, deprecatedAt, sunsetAt,
                        "https://docs.example.com/api/migrate/v1-to-v2"),
                new Version(2, Status.CURRENT, null, null, null));

        // VERSION-MIGRATION-001: fail-closed at startup if Sunset - Deprecation < the configured minimum.
        for (Version v : this.versions) {
            if (v.isDeprecated()) {
                Duration window = Duration.between(v.deprecatedAt(), v.sunsetAt());
                long minDays = MIN_DEPRECATION_WINDOW_MONTHS * 30L;
                if (window.toDays() < minDays) {
                    throw new IllegalStateException(
                            "api-versioning: deprecation window for " + v.label()
                                    + " is " + window.toDays() + "d, below the configured minimum of "
                                    + minDays + "d (VERSION-MIGRATION-001 fail-closed).");
                }
                if (v.sunsetAt().isBefore(Instant.now())) {
                    throw new IllegalStateException(
                            "api-versioning: Sunset for " + v.label()
                                    + " is in the past while still served (VERSION-DEPRECATION-001).");
                }
            }
        }
    }

    public List<Version> all() {
        return versions;
    }

    public Optional<Version> byMajor(int major) {
        return versions.stream().filter(v -> v.major() == major).findFirst();
    }

    public DefaultPolicy defaultPolicy() {
        return defaultPolicy;
    }

    /**
     * VERSION-DEFAULT-001 — resolve the version a client gets when it supplies NO selector.
     * latest-stable → the newest non-deprecated major; pinned → {@code api_version_pinned}. Either way
     * the result MUST NOT be a deprecated or sunset version.
     */
    public Version resolveDefault() {
        Version resolved = switch (defaultPolicy) {
            case LATEST_STABLE -> versions.stream()
                    .filter(v -> v.status() == Status.CURRENT)
                    .max((a, b) -> Integer.compare(a.major(), b.major()))
                    .orElseThrow(() -> new IllegalStateException("no CURRENT version advertised"));
            case PINNED -> byMajor(pinnedMajor)
                    .orElseThrow(() -> new IllegalStateException("pinned major v" + pinnedMajor + " not advertised"));
        };
        if (resolved.status() != Status.CURRENT) {
            throw new IllegalStateException(
                    "default policy resolved to a non-current version (VERSION-DEFAULT-001)");
        }
        return resolved;
    }
}
