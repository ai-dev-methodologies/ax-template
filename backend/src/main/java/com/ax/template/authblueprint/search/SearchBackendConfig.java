package com.ax.template.authblueprint.search;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SEARCH-BACKEND-001 — registers the default {@link PostgresFtsAdapter} unless
 * the application overrides it via {@code ax.search.backend} (e.g.
 * {@code meilisearch} → fork-receiver supplies their own
 * {@code MeilisearchAdapter} bean).
 * <p>
 * The default condition is "property absent or {@code postgres-fts}". When
 * fork-receivers supply an adapter for another engine, they should set
 * {@code ax.search.backend=meilisearch} (or similar) which disables this
 * default-bean condition.
 */
@Configuration
public class SearchBackendConfig {

    @Bean
    @ConditionalOnMissingBean(SearchBackend.class)
    @ConditionalOnProperty(
        prefix = "ax.search",
        name = "backend",
        havingValue = "postgres-fts",
        matchIfMissing = true
    )
    public SearchBackend postgresFtsSearchBackend(SearchIndexDocumentRepository repository) {
        return new PostgresFtsAdapter(repository);
    }
}
