package com.ax.template.authblueprint.apikey;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response body for {@code POST /api/api-keys} and {@code POST /api/api-keys/{id}/rotate}.
 * The {@code value} field carries the plaintext — this is the ONLY place the system
 * ever returns it (KEY-AUTHN-001).
 */
public record CreateApiKeyResponse(
    UUID id,
    String value,
    String prefix,
    String name,
    List<ApiKeyScope> scopes,
    ApiKeyStatus status,
    Instant createdAt,
    Instant expiresAt
) {

    public static CreateApiKeyResponse from(ApiKey key, String plaintext) {
        return new CreateApiKeyResponse(
            key.getId(),
            plaintext,
            key.getHashPrefix(),
            key.getName(),
            List.copyOf(key.getScopes()),
            key.getStatus(),
            key.getCreatedAt(),
            key.getExpiresAt()
        );
    }
}
