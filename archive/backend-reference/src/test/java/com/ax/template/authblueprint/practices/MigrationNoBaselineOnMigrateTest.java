package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("PRACTICES")
@Tag("PRACTICES-MIGRATION-003")
class MigrationNoBaselineOnMigrateTest {

    @Test
    void practices_MIGRATION_003_baselineOnMigrateIsNotEnabledInBaseYaml() throws Exception {
        // spring.flyway.baseline-on-migrate=true tells Flyway to silently mark the
        // current schema as the baseline if the migration history table is missing.
        // In production that means a forgotten flyway_schema_history table is silently
        // re-created, every prior migration is "considered applied", and the next
        // schema change runs against an undocumented state. The setting is useful
        // exactly once — initial adoption of Flyway on an existing database — and must
        // be removed (or pinned to a non-prod profile) the moment baseline is taken.
        Path yaml = Path.of(System.getProperty("user.dir"),
                "src", "main", "resources", "application.yml");
        if (!Files.exists(yaml)) {
            return;
        }
        String src = Files.readString(yaml);
        assertThat(src)
                .as("application.yml must NOT enable spring.flyway.baseline-on-migrate by default")
                .doesNotContain("baseline-on-migrate: true")
                .doesNotContain("baselineOnMigrate: true");
    }
}
