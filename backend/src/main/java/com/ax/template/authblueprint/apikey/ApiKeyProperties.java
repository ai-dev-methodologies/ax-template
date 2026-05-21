package com.ax.template.authblueprint.apikey;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration binding for the api-key domain.
 *
 * <p>Defaults mirror {@code blueprints/api-key-manifest.yaml#issuance}. Fork-receivers
 * override via {@code application.yml} when their policy differs.
 */
@ConfigurationProperties(prefix = "api-key")
public class ApiKeyProperties {

    /** Per-user soft cap. New POST is rejected with 400 TOO_MANY_KEYS once exceeded. */
    private int maxKeysPerUser = 50;

    /** Default expiration window applied when {@code expiresInDays} is omitted from the request. */
    private int defaultExpiresInDays = 365;

    public int getMaxKeysPerUser() { return maxKeysPerUser; }
    public void setMaxKeysPerUser(int v) { this.maxKeysPerUser = v; }

    public int getDefaultExpiresInDays() { return defaultExpiresInDays; }
    public void setDefaultExpiresInDays(int v) { this.defaultExpiresInDays = v; }
}
