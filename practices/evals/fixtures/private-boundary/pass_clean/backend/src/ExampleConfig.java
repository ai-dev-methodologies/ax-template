// ExampleConfig.java — private fixture for private_boundary_guard [R26] pass_clean test.
// This file demonstrates that allowlisted placeholder text does NOT trigger violations.
package com.example.config;

/**
 * Example configuration template.
 * Replace placeholder values before deploying to production.
 */
public class ExampleConfig {
    // EXAMPLE: replace your-api-key-placeholder with the real key in your private fork
    private static final String API_KEY_EXAMPLE = "your-api-key-placeholder-xxxx-REDACTED";

    // EXAMPLE JWT structure (not a real token — documentation example only)
    // eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJleGFtcGxlIn0.REDACTED-signature-placeholder-xxxx  // pragma: allow-secret

    public String getExampleApiKey() {
        return API_KEY_EXAMPLE;
    }
}
