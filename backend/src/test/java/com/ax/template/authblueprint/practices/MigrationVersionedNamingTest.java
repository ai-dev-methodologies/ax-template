package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("PRACTICES")
@Tag("PRACTICES-MIGRATION-001")
class MigrationVersionedNamingTest {

    /**
     * Flyway naming convention: V{version}__{description}.sql
     *   - 'V' prefix (uppercase)
     *   - version: digits, optionally separated by dots / underscores
     *   - DOUBLE underscore separator
     *   - description: non-empty
     *   - .sql extension
     * Repeatable migrations (R__) and undo (U__) are out of scope for this rule.
     */
    private static final Pattern VERSIONED_FLYWAY = Pattern.compile(
            "^V[0-9]+(?:[._][0-9]+)*__[A-Za-z0-9_]+\\.sql$");

    @Test
    void practices_MIGRATION_001_everyMigrationFollowsFlywayVersionedNaming() throws Exception {
        Path dir = Path.of(System.getProperty("user.dir"),
                "src", "main", "resources", "db", "migration");
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> files = Files.list(dir)) {
            List<String> offenders = files
                    .map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".sql"))
                    .filter(n -> !VERSIONED_FLYWAY.matcher(n).matches())
                    .toList();
            assertThat(offenders)
                    .as("every SQL migration in db/migration/ must match V{version}__{description}.sql")
                    .isEmpty();
        }
    }
}
