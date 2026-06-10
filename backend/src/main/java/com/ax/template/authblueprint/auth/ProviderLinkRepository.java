package com.ax.template.authblueprint.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProviderLinkRepository extends JpaRepository<ProviderLink, UUID> {
    List<ProviderLink> findByUserId(UUID userId);
    Optional<ProviderLink> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);
    Optional<ProviderLink> findByUserIdAndProvider(UUID userId, OAuthProvider provider);
    long countByUserId(UUID userId);
}
