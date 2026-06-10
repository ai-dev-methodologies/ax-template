package com.ax.template.authblueprint.auth;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * References the User aggregate BY ID (DDD reference-by-id — AX-DDD-AUTH-USER retire):
 * the {@code user_id} column is unchanged from the old {@code @ManyToOne} mapping, so the
 * V028 FK constraint still applies; only the Java-side object pointer is gone. Callers
 * resolve the account via the published {@code user.UserAccountService} when needed.
 */
@AggregateRoot
@Entity
@Table(name = "verification_tokens")
public class VerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String token;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    @Column(nullable = false)
    private String tokenType;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
}
