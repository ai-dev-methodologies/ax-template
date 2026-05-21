package com.ax.template.authblueprint.apikey;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read-side DTO for list / get / delete responses. CRITICALLY: does NOT carry the
 * plaintext {@code value} field — KEY-AUTHN-001 / KEY-STORAGE-002. Only the prefix
 * is exposed for human display.
 */
public record ApiKeyResponse(
    UUID id,
    String prefix,
    String name,
    List<ApiKeyScope> scopes,
    ApiKeyStatus status,
    Instant createdAt,
    Instant expiresAt,
    Instant lastUsedAt
) {

    public static ApiKeyResponse from(ApiKey key) {
        return new ApiKeyResponse(
            key.getId(),
            key.getHashPrefix(),
            key.getName(),
            List.copyOf(key.getScopes()),
            key.getStatus(),
            key.getCreatedAt(),
            key.getExpiresAt(),
            key.getLastUsedAt()
        );
    }
}
