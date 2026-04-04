package com.ax.template.authblueprint.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProviderLinkRepository extends JpaRepository<ProviderLink, UUID> {
    List<ProviderLink> findByUser(UserEntity user);
    Optional<ProviderLink> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);
    Optional<ProviderLink> findByUserAndProvider(UserEntity user, OAuthProvider provider);
    boolean existsByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);
    long countByUser(UserEntity user);
}
