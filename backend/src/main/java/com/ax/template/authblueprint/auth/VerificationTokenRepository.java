package com.ax.template.authblueprint.auth;

import com.ax.template.authblueprint.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {
    Optional<VerificationToken> findByTokenAndUsedFalse(String token);
    void deleteByUser(UserEntity user);
    List<VerificationToken> findByUserAndTokenType(UserEntity user, String tokenType);
}
