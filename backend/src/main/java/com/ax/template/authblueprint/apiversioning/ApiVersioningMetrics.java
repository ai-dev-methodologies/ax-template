package com.ax.template.authblueprint.apiversioning;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * VERSION-OBSERVABILITY-001 — EXACTLY 3 canonical Micrometer metrics with bounded-cardinality labels:
 * <ul>
 *   <li>{@code api_requests_by_version_total{version, status}} — counter; {@code version} from the
 *       CLOSED advertised-version set ({@code v1}/{@code v2}), {@code status} ∈
 *       {current, deprecated, sunset};</li>
 *   <li>{@code api_deprecated_version_calls_total{version}} — counter, incremented per request served
 *       from a deprecated version, {@code version} from the same fixed set;</li>
 *   <li>{@code api_version_sunset_breach_total{version}} — counter, incremented when a request arrives
 *       for a version whose sunset instant has passed (served 410), {@code version} from the same set.</li>
 * </ul>
 * Labels MUST exclude client id / user id / route / api key / IP / any high-cardinality or PII value.
 * The {@code version} label is safe ONLY because the advertised set is small and fixed
 * ({@link ApiVersionCatalog}); {@code status} is a fixed enum.
 *
 * <p>Spec: specs/api-versioning-l0.yaml#VERSION-OBSERVABILITY-001.
 */
@Component
public class ApiVersioningMetrics {

    public static final String REQUESTS_BY_VERSION = "api_requests_by_version_total";
    public static final String DEPRECATED_CALLS = "api_deprecated_version_calls_total";
    public static final String SUNSET_BREACH = "api_version_sunset_breach_total";

    static final String TAG_VERSION = "version";
    static final String TAG_STATUS = "status";

    private final MeterRegistry registry;

    public ApiVersioningMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** One request served from {@code version} (e.g. "v2") whose status is one of {current,deprecated,sunset}. */
    public void requestServed(String version, ApiVersionCatalog.Status status) {
        Counter.builder(REQUESTS_BY_VERSION)
                .tag(TAG_VERSION, version)
                .tag(TAG_STATUS, status.name().toLowerCase(Locale.ROOT))
                .register(registry).increment();
    }

    /** One request served from a DEPRECATED version. */
    public void deprecatedCall(String version) {
        Counter.builder(DEPRECATED_CALLS).tag(TAG_VERSION, version).register(registry).increment();
    }

    /** One request for a version whose sunset instant has already passed (served 410 / restricted). */
    public void sunsetBreach(String version) {
        Counter.builder(SUNSET_BREACH).tag(TAG_VERSION, version).register(registry).increment();
    }
}
