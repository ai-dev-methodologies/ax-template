package com.ax.template.authblueprint.sessionmanagement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class SessionDtos {

    private SessionDtos() {}

    public record RegisterSessionRequest(
        @NotBlank @Size(max = 128) String jti,
        @Size(max = 64) String deviceLabel,
        String ipAddress,
        @Size(max = 512) String userAgent,
        @NotNull Instant expiresAt
    ) {}

    public record SessionResponse(
        UUID id,
        SessionStatus status,
        String jti,
        String deviceLabel,
        String ipAddressMasked,
        String userAgentSummary,
        Instant createdAt,
        Instant lastSeenAt,
        Instant expiresAt,
        Instant revokedAt,
        String revokedByUserId,
        boolean expired
    ) {

        public static SessionResponse from(SessionRecord s, Clock clock) {
            return new SessionResponse(
                s.getId(),
                s.getStatus(),
                s.getJti(),
                s.getDeviceLabel(),
                IpAddressMasker.mask(s.getIpAddress()),
                UserAgentSummarizer.summarize(s.getUserAgent()),
                s.getCreatedAt(),
                s.getLastSeenAt(),
                s.getExpiresAt(),
                s.getRevokedAt(),
                s.getRevokedByUserId(),
                s.isExpired(Instant.now(clock))
            );
        }
    }

    public record SessionListResponse(List<SessionResponse> items, long totalElements) {}

    public record RevokeOthersResponse(int revoked, int kept) {}
}
