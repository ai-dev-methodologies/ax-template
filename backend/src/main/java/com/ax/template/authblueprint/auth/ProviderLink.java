package com.ax.template.authblueprint.auth;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * OAuth provider identity link — auth-domain data (relocated from the {@code user} feature;
 * AX-DDD-AUTH-USER retire). References the User aggregate BY ID: the {@code user_id} column
 * is unchanged from the old {@code @ManyToOne} mapping, so the V028 FK constraint still
 * applies; only the Java-side object pointer is gone.
 */
@AggregateRoot
@Entity
@Table(name = "provider_links", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"provider", "provider_user_id"})
})
public class ProviderLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OAuthProvider provider;

    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId;

    @Column
    private String providerEmail;

    @Column(nullable = false, updatable = false)
    private Instant linkedAt = Instant.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public OAuthProvider getProvider() { return provider; }
    public void setProvider(OAuthProvider provider) { this.provider = provider; }

    public String getProviderUserId() { return providerUserId; }
    public void setProviderUserId(String providerUserId) { this.providerUserId = providerUserId; }

    public String getProviderEmail() { return providerEmail; }
    public void setProviderEmail(String providerEmail) { this.providerEmail = providerEmail; }

    public Instant getLinkedAt() { return linkedAt; }
}
