package com.ax.template.authblueprint.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {
    Optional<VerificationToken> findByTokenAndUsedFalse(String token);
    void deleteByUserId(UUID userId);
    List<VerificationToken> findByUserIdAndTokenType(UUID userId, String tokenType);
}
