/**
 * @ax-template-meta
 * template_id: backend/observability/MicrometerConfig
 * layer: backend-cross-cutting
 * anchors_rule: observability-structured-logging.md (PRACTICES-OBS-001)
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "Micrometer — facade over instrumentation clients for popular observability systems; Prometheus + OTLP exporters supported"
 *     url: "https://docs.micrometer.io/micrometer/reference/"
 *   - source_type: external
 *     citation: "Spring Boot Actuator — management.endpoints.web.exposure.include=prometheus"
 *     url: "https://docs.spring.io/spring-boot/reference/actuator/endpoints.html"
 * usage: |
 *   1. Replace 'com.example.app' with your base package.
 *   2. Add io.micrometer:micrometer-registry-prometheus to dependencies for
 *      the /actuator/prometheus endpoint.
 *   3. Add io.micrometer:micrometer-registry-otlp for OTLP push-based export.
 *   4. Configure management.otlp.metrics.export.url in application.yml.
 *   5. Spring Boot auto-configures JvmMetrics, ProcessMetrics, and CacheMetrics
 *      — no explicit registration needed.
 */
package com.example.app.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Micrometer metrics configuration: common tags, JVM binders, and registry filters.
 *
 * <p>Spring Boot auto-configures a {@code CompositeMeterRegistry} and registers
 * {@code JvmMetrics}, {@code ProcessMetrics}, {@code TomcatMetrics}, and
 * {@code CacheMetrics}. This class adds common tags and example custom filters.
 *
 * <p>Prometheus endpoint: expose via
 * {@code management.endpoints.web.exposure.include=prometheus,health,info}.
 *
 * <p>OTLP push config:
 * <pre>{@code
 * management.otlp.metrics.export.url=http://otel-collector:4318/v1/metrics
 * management.otlp.metrics.export.step=60s
 * }</pre>
 *
 * @see <a href="https://docs.micrometer.io/micrometer/reference/">Micrometer Reference</a>
 */
@Configuration
public class MicrometerConfig {

    /**
     * Attaches common tags to every meter — service name, environment, and version.
     *
     * <p>Keep tag values low-cardinality. Never use request IDs, user IDs,
     * or other unbounded values as tag values.
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> commonTags() {
        return registry -> registry.config()
                .commonTags(
                        "application", "${spring.application.name:app}",
                        "env", "${spring.profiles.active:local}");
    }

    /**
     * Optional: deny high-cardinality URI tags that Spring MVC auto-generates
     * for unmapped paths (e.g., arbitrary URL probing).
     *
     * <p>Remove or adapt this bean if all URI dimensions are low-cardinality
     * in your application.
     */
    @Bean
    public MeterFilter denyHighCardinalityUris() {
        return MeterFilter.deny(id -> {
            String uri = id.getTag("uri");
            return uri != null && uri.startsWith("/actuator") && uri.contains("...");
        });
    }

    // ── Explicit JVM binders ──────────────────────────────────────────────────
    // Spring Boot 3.x auto-registers these when spring-boot-starter-actuator is
    // present. Include explicitly only if actuator auto-config is excluded.

    @Bean(destroyMethod = "close")
    public JvmGcMetrics jvmGcMetrics() {
        return new JvmGcMetrics();
    }

    @Bean
    public JvmMemoryMetrics jvmMemoryMetrics() {
        return new JvmMemoryMetrics();
    }

    @Bean
    public JvmThreadMetrics jvmThreadMetrics() {
        return new JvmThreadMetrics();
    }

    @Bean
    public ProcessorMetrics processorMetrics() {
        return new ProcessorMetrics();
    }
}
