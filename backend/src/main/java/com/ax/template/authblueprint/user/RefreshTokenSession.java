package com.ax.template.authblueprint.user;

import java.time.Instant;

public record RefreshTokenSession(
    String sessionId,
    String userId,
    Instant issuedAt,
    Instant expiresAt,
    Instant graceWindowUntil
) {
}
