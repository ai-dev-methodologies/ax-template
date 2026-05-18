/**
 * @ax-template-meta
 * template_id: backend/observability/OtelTracerConfig
 * layer: backend-cross-cutting
 * anchors_rule: observability-structured-logging.md (PRACTICES-OBS-001)
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "OpenTelemetry Java — Traces Stable; W3C Trace Context propagation"
 *     url: "https://opentelemetry.io/docs/languages/java/"
 *   - source_type: external
 *     citation: "W3C Trace Context — traceparent header format"
 *     url: "https://www.w3.org/TR/trace-context/#traceparent-header"
 * usage: |
 *   1. Replace 'com.example.app' with your base package.
 *   2. Add io.opentelemetry:opentelemetry-api and
 *      io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter to
 *      your dependencies, or use the OpenTelemetry Java agent (zero-code).
 *   3. Configure OTEL_EXPORTER_OTLP_ENDPOINT and OTEL_SERVICE_NAME env vars
 *      for the OTLP gRPC/HTTP exporter.
 *   4. This bean is optional when using the Java agent — the agent auto-configures
 *      the SDK, W3C propagator, and tracer provider.
 */
package com.example.app.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Minimal OpenTelemetry SDK configuration for W3C Trace Context propagation.
 *
 * <p>This configuration is suitable for applications that manage the SDK
 * programmatically rather than using the OpenTelemetry Java agent.
 *
 * <p>For production workloads, configure an OTLP exporter:
 * <pre>{@code
 * management.otlp.tracing.endpoint=http://otel-collector:4318/v1/traces
 * }</pre>
 *
 * <p>The W3C Trace Context propagator ({@code traceparent} / {@code tracestate} headers)
 * is the standard propagation format for distributed tracing across services.
 *
 * @see <a href="https://opentelemetry.io/docs/languages/java/">OpenTelemetry Java</a>
 * @see <a href="https://www.w3.org/TR/trace-context/">W3C Trace Context</a>
 */
@Configuration
public class OtelTracerConfig {

    private static final String INSTRUMENTATION_SCOPE = "com.example.app";
    private static final String INSTRUMENTATION_VERSION = "1.0.0";

    /**
     * Creates a tracer with W3C Trace Context propagation.
     *
     * <p>Replace with your service name and version.
     * When using the Spring Boot OpenTelemetry starter or Java agent,
     * this bean is auto-configured — remove this class in that case.
     */
    @Bean
    public OpenTelemetry openTelemetry() {
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                // Add OTLP exporter in production:
                // .addSpanProcessor(BatchSpanProcessor.builder(
                //     OtlpGrpcSpanExporter.builder()
                //         .setEndpoint(System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT"))
                //         .build()).build())
                .build();

        return OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setPropagators(ContextPropagators.create(
                        W3CTraceContextPropagator.getInstance()))
                .build();
    }

    /**
     * Convenience bean — inject {@code Tracer} directly where manual instrumentation
     * is needed.
     */
    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer(INSTRUMENTATION_SCOPE, INSTRUMENTATION_VERSION);
    }
}
