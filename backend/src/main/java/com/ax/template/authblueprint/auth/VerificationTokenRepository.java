package com.ax.template.authblueprint.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {
    Optional<VerificationToken> findByTokenAndUsedFalse(String token);
    void deleteByUserId(UUID userId);
    List<VerificationToken> findByUserIdAndTokenType(UUID userId, String tokenType);

    /**
     * AUTH-RESET-FAMILY-001 / CWE-640: atomically mark EVERY outstanding unused token of the
     * given type for one user as used. Called inside the successful password-reset transaction
     * so the whole family of the user's reset tokens is invalidated at once — not only the
     * single consumed token — closing the replay window on previously issued reset links.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update VerificationToken v set v.used = true "
        + "where v.userId = :userId and v.tokenType = :tokenType and v.used = false")
    int markAllUnusedAsUsed(@Param("userId") UUID userId, @Param("tokenType") String tokenType);
}
