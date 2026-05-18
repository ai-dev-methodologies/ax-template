/**
 * @ax-template-meta
 * template_id: backend/data/FlywayConfig
 * layer: backend-infrastructure
 * domain: data
 * anchors_rule: migration-no-baseline-on-migrate.md (PRACTICES-MIGRATION-001)
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "Spring Boot Reference — Flyway: Spring Boot auto-configures Flyway when flyway-core is on the classpath; customise via spring.flyway.* properties or a FlywayConfigurationCustomizer bean"
 *     url: "https://docs.spring.io/spring-boot/reference/data/sql.html#data.sql.flyway"
 *   - source_type: external
 *     citation: "Flyway Documentation — baselineOnMigrate: Sets the default schema version on an existing non-empty schema; must be false in production to detect schema drift"
 *     url: "https://documentation.red-gate.com/fd/baseline-on-migrate-184127624.html"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Add flyway-core to your build dependencies.
 *   Place migration scripts in src/main/resources/db/migration/ named V{N}__{description}.sql.
 *   Set spring.datasource.url to your PostgreSQL connection string in application.properties.
 *   Do NOT set spring.jpa.hibernate.ddl-auto to anything other than 'validate' when Flyway is active.
 */
package com.example.app.data;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flyway migration configuration for the application.
 *
 * <p>Provides explicit control over the Flyway migration lifecycle:
 * <ul>
 *   <li>{@code baselineOnMigrate = false} — enforced to catch schema drift on existing
 *       databases. If Flyway finds an unapplied migration on a non-empty schema it will
 *       fail loudly instead of silently skipping (PRACTICES-MIGRATION-001).</li>
 *   <li>{@code outOfOrder = false} — migrations must run in version order. Out-of-order
 *       application indicates a branching/merging error and must not be silenced.</li>
 *   <li>{@code validateOnMigrate = true} (Flyway default) — checksums are verified at
 *       every startup; applied migrations that were modified after apply will cause startup
 *       failure (PRACTICES-MIGRATION-002).</li>
 * </ul>
 *
 * <p>The {@link FlywayMigrationStrategy} bean replaces Spring Boot's default no-op
 * strategy so that migration failures surface as application context startup failures,
 * not silent deferred errors.
 *
 * <p>Migration scripts live in {@code src/main/resources/db/migration/} and follow the
 * naming convention {@code V{N}__{description}.sql} where N is a monotonically increasing
 * integer. Never reuse, edit, or delete an applied migration file.
 */
@Configuration
public class FlywayConfig {

    /**
     * Override the default Flyway migration strategy to make startup failure explicit.
     *
     * <p>Spring Boot's auto-configured {@code Flyway} bean already handles migration,
     * but declaring this bean gives us a hook to add monitoring or alerting hooks
     * in production without restructuring the bootstrap.
     */
    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return Flyway::migrate;
    }
}
