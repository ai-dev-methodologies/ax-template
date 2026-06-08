package com.ax.template.authblueprint.apikey;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * ApiKey — persistence record. Stores the SHA-256 hex digest of the plaintext;
 * the plaintext itself is never persisted (KEY-AUTHN-001) and never exposed via
 * any DTO (KEY-STORAGE-002).
 *
 * <p>Trace:
 * <ul>
 *   <li>KEY-STORAGE-001 — {@code hashedValue} is the SHA-256 hex digest</li>
 *   <li>KEY-STORAGE-002 — {@code hashedValue} is {@link JsonIgnore @JsonIgnore} for
 *       defense in depth in case an accidental DTO ever holds the entity directly</li>
 *   <li>KEY-STORAGE-003 — column is {@code nullable=false, updatable=false}</li>
 *   <li>KEY-AUTHZ-002  — every lookup filters on {@code (id, ownerUserId)}</li>
 * </ul>
 */
@AggregateRoot
@Entity
@Table(
    name = "api_keys",
    indexes = {
        @Index(name = "ix_api_keys_owner_status", columnList = "owner_user_id,status"),
        @Index(name = "ix_api_keys_hash_prefix", columnList = "hash_prefix")
    }
)
public class ApiKey {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false, updatable = false, length = 255)
    private String ownerUserId;

    @Column(name = "name", length = 128)
    private String name;

    /**
     * First 8 chars of the plaintext value (e.g. {@code ak_abcde}). Safe to display
     * and to use as a lookup key — knowing the prefix alone does not authenticate.
     */
    @Column(name = "hash_prefix", nullable = false, updatable = false, length = 16)
    private String hashPrefix;

    /**
     * SHA-256 hex digest of the full plaintext value. Never serialized to clients —
     * exposing this would let an attacker brute-force the plaintext offline.
     */
    @JsonIgnore
    @Column(name = "hashed_value", nullable = false, updatable = false, length = 64)
    private String hashedValue;

    @ElementCollection(fetch = FetchType.EAGER, targetClass = ApiKeyScope.class)
    @Enumerated(EnumType.STRING)
    @jakarta.persistence.CollectionTable(
        name = "api_key_scopes",
        joinColumns = @jakarta.persistence.JoinColumn(name = "api_key_id"),
        indexes = @Index(name = "ix_api_key_scopes_key", columnList = "api_key_id")
    )
    @Column(name = "scope", nullable = false, length = 32)
    private Set<ApiKeyScope> scopes = EnumSet.noneOf(ApiKeyScope.class);

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ApiKeyStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    /** Required by JPA. */
    protected ApiKey() {}

    private ApiKey(Builder b) {
        this.id = (b.id != null) ? b.id : UUID.randomUUID();
        this.ownerUserId = b.ownerUserId;
        this.name = b.name;
        this.hashPrefix = b.hashPrefix;
        this.hashedValue = b.hashedValue;
        this.scopes = (b.scopes != null && !b.scopes.isEmpty())
            ? EnumSet.copyOf(b.scopes)
            : EnumSet.of(ApiKeyScope.READ);
        this.status = (b.status != null) ? b.status : ApiKeyStatus.ACTIVE;
        this.createdAt = (b.createdAt != null) ? b.createdAt : Instant.now();
        this.expiresAt = b.expiresAt;
    }

    public UUID getId() { return id; }
    public String getOwnerUserId() { return ownerUserId; }
    public String getName() { return name; }
    public String getHashPrefix() { return hashPrefix; }

    @JsonIgnore
    public String getHashedValue() { return hashedValue; }

    public Set<ApiKeyScope> getScopes() {
        return EnumSet.copyOf(scopes.isEmpty() ? EnumSet.of(ApiKeyScope.READ) : scopes);
    }

    public ApiKeyStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public Instant getLastUsedAt() { return lastUsedAt; }

    /** Package-private — only the service should mutate lifecycle fields. */
    void markRevoked(Instant when) {
        if (this.status == ApiKeyStatus.REVOKED) {
            return;
        }
        this.status = ApiKeyStatus.REVOKED;
        this.revokedAt = when;
    }

    void touchLastUsed(Instant when) {
        this.lastUsedAt = when;
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && now.isAfter(expiresAt);
    }

    public boolean isActive(Instant now) {
        return status == ApiKeyStatus.ACTIVE && !isExpired(now);
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private String ownerUserId;
        private String name;
        private String hashPrefix;
        private String hashedValue;
        private Set<ApiKeyScope> scopes;
        private ApiKeyStatus status;
        private Instant createdAt;
        private Instant expiresAt;

        public Builder id(UUID v) { this.id = v; return this; }
        public Builder ownerUserId(String v) { this.ownerUserId = v; return this; }
        public Builder name(String v) { this.name = v; return this; }
        public Builder hashPrefix(String v) { this.hashPrefix = v; return this; }
        public Builder hashedValue(String v) { this.hashedValue = v; return this; }
        public Builder scopes(Set<ApiKeyScope> v) { this.scopes = v; return this; }
        public Builder status(ApiKeyStatus v) { this.status = v; return this; }
        public Builder createdAt(Instant v) { this.createdAt = v; return this; }
        public Builder expiresAt(Instant v) { this.expiresAt = v; return this; }

        public ApiKey build() { return new ApiKey(this); }
    }
}
