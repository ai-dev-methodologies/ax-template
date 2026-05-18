/**
 * @ax-template-meta
 * template_id: backend/observability/StructuredLoggingConfig
 * layer: backend-cross-cutting
 * anchors_rule: observability-structured-logging.md (PRACTICES-OBS-001)
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "Logback — JsonLayout and logstash-logback-encoder for structured JSON output; MDC fields auto-included"
 *     url: "https://logback.qos.ch/manual/layouts.html"
 *   - source_type: external
 *     citation: "OWASP Logging Cheat Sheet — do not log sensitive data; use structured logging for SIEM integration"
 *     url: "https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html"
 * usage: |
 *   This file documents Logback JSON structured logging configuration.
 *   Actual configuration is in logback-spring.xml — see the XML example below.
 *
 *   Dependencies (add to build.gradle.kts):
 *     implementation("net.logstash.logback:logstash-logback-encoder:7.4")
 *
 *   Place logback-spring.xml in src/main/resources/ with the content documented
 *   in the Javadoc of this class.
 */
package com.example.app.observability;

/**
 * Documents the recommended Logback structured JSON logging configuration.
 *
 * <p>Logback is configured via {@code logback-spring.xml} (not programmatically).
 * This class exists as a documentation anchor for the {@code @ax-template-meta}
 * block — the actual wiring is in XML.
 *
 * <p>Recommended {@code logback-spring.xml}:
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <configuration>
 *
 *   <springProperty scope="context" name="APP_NAME"
 *                   source="spring.application.name" defaultValue="app"/>
 *
 *   <appender name="JSON_STDOUT" class="ch.qos.logback.core.ConsoleAppender">
 *     <encoder class="net.logstash.logback.encoder.LogstashEncoder">
 *       <!-- MDC keys traceId and correlationId are included automatically -->
 *       <fieldNames>
 *         <timestamp>timestamp</timestamp>
 *         <message>message</message>
 *         <logger>logger</logger>
 *         <thread>thread</thread>
 *         <level>level</level>
 *       </fieldNames>
 *       <customFields>{"application":"${APP_NAME}"}</customFields>
 *     </encoder>
 *   </appender>
 *
 *   <!-- Dev profile: human-readable console output -->
 *   <springProfile name="!prod">
 *     <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
 *       <encoder>
 *         <pattern>%d{HH:mm:ss.SSS} %-5level [%X{traceId}] %logger{36} - %msg%n</pattern>
 *       </encoder>
 *     </appender>
 *     <root level="INFO">
 *       <appender-ref ref="STDOUT"/>
 *     </root>
 *   </springProfile>
 *
 *   <!-- Prod profile: structured JSON for log aggregation -->
 *   <springProfile name="prod">
 *     <root level="INFO">
 *       <appender-ref ref="JSON_STDOUT"/>
 *     </root>
 *   </springProfile>
 *
 * </configuration>
 * }</pre>
 *
 * <p>Security note: never log passwords, tokens, PII, or card numbers.
 * MDC keys set by {@link MdcCorrelationIdInterceptor} (traceId, correlationId)
 * are included automatically in all log events via logstash-logback-encoder.
 *
 * @see <a href="https://logback.qos.ch/manual/layouts.html">Logback Layouts</a>
 * @see <a href="https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html">OWASP Logging Cheat Sheet</a>
 * @see MdcCorrelationIdInterceptor
 */
public final class StructuredLoggingConfig {

    private StructuredLoggingConfig() {
        // Documentation-only class — not instantiated
    }
}
