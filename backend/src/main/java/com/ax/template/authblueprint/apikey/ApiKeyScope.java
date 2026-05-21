package com.ax.template.authblueprint.apikey;

/**
 * Coarse-grained authorities carried by an {@link ApiKey}.
 *
 * <p>The filter maps each scope to a Spring Security authority
 * ({@code READ → ROLE_API_READ}, {@code WRITE → ROLE_API_WRITE}) — see
 * {@code blueprints/api-key-manifest.yaml#authz.scope_to_authority}.
 * Trace: KEY-AUTHZ-003.
 */
public enum ApiKeyScope {
    READ,
    WRITE;

    public String toAuthority() {
        return switch (this) {
            case READ  -> "ROLE_API_READ";
            case WRITE -> "ROLE_API_WRITE";
        };
    }
}
