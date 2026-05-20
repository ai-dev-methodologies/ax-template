package com.ax.template.authblueprint.search;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BACKEND family — SEARCH-BACKEND-001. Asserts:
 *  • the active {@link SearchBackend} bean is the default postgres-fts adapter
 *    (no override property set in the reference workload), AND
 *  • {@code blueprints/search-manifest.yaml#backend.default_backend} declares
 *    {@code postgres-fts}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SearchBackendTest {

    @Autowired SearchBackend searchBackend;

    @Test
    @Tag("search")
    @Tag("SEARCH-BACKEND-001")
    @SuppressWarnings("unchecked")
    void backend_001_defaultIsPostgresFts() throws IOException {
        // Active bean — should be the postgres-fts default.
        assertThat(searchBackend.name())
            .as("SEARCH-BACKEND-001 — default backend bean must be postgres-fts")
            .isEqualTo("postgres-fts");

        // Manifest declares default_backend: postgres-fts.
        Path manifest = Path.of("..", "blueprints", "search-manifest.yaml");
        assertThat(Files.exists(manifest))
            .as("blueprints/search-manifest.yaml must exist")
            .isTrue();

        try (InputStream in = Files.newInputStream(manifest)) {
            Map<String, Object> doc = new Yaml().load(in);
            Map<String, Object> backend = (Map<String, Object>) doc.get("backend");
            assertThat(backend.get("default_backend"))
                .as("blueprints/search-manifest.yaml#backend.default_backend")
                .isEqualTo("postgres-fts");
        }
    }
}
