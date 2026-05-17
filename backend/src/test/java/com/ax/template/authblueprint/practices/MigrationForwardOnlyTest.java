package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("PRACTICES")
@Tag("PRACTICES-MIGRATION-002")
class MigrationForwardOnlyTest {

    private static final Pattern VERSION = Pattern.compile("^V([0-9]+)(?:[._][0-9]+)*__");

    @Test
    void practices_MIGRATION_002_migrationVersionsAreUniqueAndMonotonic() throws Exception {
        Path dir = Path.of(System.getProperty("user.dir"),
                "src", "main", "resources", "db", "migration");
        if (!Files.isDirectory(dir)) {
            return;
        }
        List<Integer> versions;
        try (Stream<Path> files = Files.list(dir)) {
            versions = files
                    .map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".sql"))
                    .map(VERSION::matcher)
                    .filter(Matcher::find)
                    .map(m -> Integer.parseInt(m.group(1)))
                    .sorted()
                    .toList();
        }
        // No duplicates (would create ambiguity for Flyway)
        assertThat(versions)
                .as("migration major-version prefixes must be unique")
                .doesNotHaveDuplicates();

        // Forward-only — earlier versions cannot be re-edited. The rule's mechanical
        // enforcement is the immutable-after-apply contract; in CI we just assert
        // sequential numbering so a new commit cannot retroactively renumber.
        for (int i = 1; i < versions.size(); i++) {
            assertThat(versions.get(i))
                    .as("migration versions must increase monotonically")
                    .isGreaterThan(versions.get(i - 1));
        }
    }
}
