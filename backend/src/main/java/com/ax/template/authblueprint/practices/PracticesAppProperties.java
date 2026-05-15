package com.ax.template.authblueprint.practices;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Fixture for PRACTICES-CONFIG-001: typed @ConfigurationProperties record.
 * Binds the `practices.app.*` namespace from application.yml into an immutable record.
 * Beats scattered @Value injections because (a) one source of truth for the contract,
 * (b) immutable, (c) IDE refactor renames every callsite, (d) defaults declared in code.
 */
@ConfigurationProperties("practices.app")
public record PracticesAppProperties(
        String name,
        int maxConcurrentRequests
) {
    public PracticesAppProperties {
        if (name == null || name.isBlank()) name = "ax-template";
        if (maxConcurrentRequests <= 0) maxConcurrentRequests = 64;
    }
}
