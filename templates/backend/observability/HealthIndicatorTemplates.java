/**
 * @ax-template-meta
 * template_id: backend/observability/HealthIndicatorTemplates
 * layer: backend-cross-cutting
 * anchors_rule: observability-structured-logging.md (PRACTICES-OBS-001)
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "Spring Boot Actuator — HealthIndicator SPI; /actuator/health endpoint shows UP/DOWN per component"
 *     url: "https://docs.spring.io/spring-boot/reference/actuator/endpoints.html#actuator.endpoints.health"
 *   - source_type: external
 *     citation: "OpenTelemetry Java — Traces, Metrics, Logs all Stable"
 *     url: "https://opentelemetry.io/docs/languages/java/"
 * usage: |
 *   1. Replace 'com.example.app' with your base package.
 *   2. Copy the relevant inner class(es) to your observability package.
 *   3. Each HealthIndicator is a Spring @Component — it is automatically
 *      included in /actuator/health when spring-boot-starter-actuator is present.
 *   4. Expose health details selectively:
 *      management.endpoint.health.show-details=when-authorized
 */
package com.example.app.observability;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Reference implementations of Spring Boot {@code HealthIndicator} for common
 * infrastructure dependencies: database, Redis, and external HTTP endpoints.
 *
 * <p>Each indicator contributes a named component to the
 * {@code /actuator/health} response. Kubernetes liveness and readiness probes
 * consume these endpoints:
 * <ul>
 *   <li>{@code /actuator/health/liveness} — is the process alive?</li>
 *   <li>{@code /actuator/health/readiness} — is the process ready to serve traffic?</li>
 * </ul>
 *
 * @see <a href="https://docs.spring.io/spring-boot/reference/actuator/endpoints.html">Spring Boot Actuator Endpoints</a>
 */
public class HealthIndicatorTemplates {

    /**
     * Database health indicator — verifies JDBC connectivity with a lightweight
     * validation query.
     *
     * <p>Spring Boot auto-configures a {@code DataSourceHealthIndicator} when
     * {@code spring-boot-starter-data-jpa} is on the classpath. Use this
     * custom implementation only when the auto-configured indicator is disabled
     * or when custom validation logic is required.
     */
    @Component("db")
    public static class DatabaseHealthIndicator implements HealthIndicator {

        private final JdbcTemplate jdbcTemplate;

        public DatabaseHealthIndicator(DataSource dataSource) {
            this.jdbcTemplate = new JdbcTemplate(dataSource);
        }

        @Override
        public Health health() {
            try {
                jdbcTemplate.queryForObject("SELECT 1", Integer.class);
                return Health.up()
                        .withDetail("database", "reachable")
                        .build();
            } catch (Exception ex) {
                return Health.down(ex)
                        .withDetail("database", "unreachable")
                        .build();
            }
        }
    }

    /**
     * External HTTP service health indicator — verifies connectivity to a
     * downstream dependency.
     *
     * <p>Replace {@code EXTERNAL_SERVICE_URL} with your dependency's health URL.
     * The check uses a short connect timeout to avoid blocking the health endpoint.
     */
    @Component("externalHttp")
    public static class ExternalHttpHealthIndicator implements HealthIndicator {

        /** Replace with the URL of the external service health endpoint. */
        private static final String EXTERNAL_SERVICE_URL =
                System.getenv().getOrDefault("EXTERNAL_SERVICE_HEALTH_URL",
                        "http://external-service/actuator/health");

        @Override
        public Health health() {
            try {
                var connection = new java.net.URL(EXTERNAL_SERVICE_URL).openConnection();
                connection.setConnectTimeout(2_000);
                connection.setReadTimeout(2_000);
                connection.connect();
                return Health.up()
                        .withDetail("url", EXTERNAL_SERVICE_URL)
                        .build();
            } catch (Exception ex) {
                return Health.down(ex)
                        .withDetail("url", EXTERNAL_SERVICE_URL)
                        .build();
            }
        }
    }
}
