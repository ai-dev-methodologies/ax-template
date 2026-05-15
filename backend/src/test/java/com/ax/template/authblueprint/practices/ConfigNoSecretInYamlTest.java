package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("PRACTICES")
@Tag("PRACTICES-CONFIG-002")
class ConfigNoSecretInYamlTest {

    /**
     * Sensitive keys that must NEVER carry a hardcoded value. They must reference an
     * environment variable via the {@code ${ENV_NAME[:default]}} placeholder syntax.
     * Empty values are permitted (e.g. an empty H2 dev password) — we only reject
     * apparent literal secrets.
     */
    private static final Pattern SECRET_KEY = Pattern.compile(
            "^\\s*(?:client-secret|api[-_]?key|access[-_]?token|jwt[-_]?secret|encryption[-_]?key|webhook[-_]?secret)\\s*:\\s*(.*)$",
            Pattern.CASE_INSENSITIVE);

    @Test
    void practices_CONFIG_002_noHardcodedSecretLiteralsInApplicationYaml() throws Exception {
        Path yaml = Path.of(System.getProperty("user.dir"), "src", "main", "resources", "application.yml");
        if (!Files.exists(yaml)) {
            return; // nothing to inspect
        }
        List<String> offenders = Files.readAllLines(yaml).stream()
                .map(line -> {
                    Matcher m = SECRET_KEY.matcher(line);
                    if (!m.find()) return null;
                    String value = m.group(1).trim();
                    // Strip surrounding quotes for inspection
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    // Empty / placeholder / env-reference are all acceptable.
                    if (value.isEmpty()) return null;
                    if (value.startsWith("${")) return null;
                    if (value.startsWith("dummy-") || value.equals("changeme")) return null;
                    return line;
                })
                .filter(s -> s != null)
                .toList();
        assertThat(offenders)
                .as("application.yml must not carry hardcoded secret literals — use ${ENV[:default]} placeholders")
                .isEmpty();
    }
}
