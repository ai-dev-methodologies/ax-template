package com.ax.template.authblueprint.user;

import java.time.Instant;

public record ProviderLink(
    AuthProvider provider,
    String providerUserId,
    Instant connectedAt
) {
}
