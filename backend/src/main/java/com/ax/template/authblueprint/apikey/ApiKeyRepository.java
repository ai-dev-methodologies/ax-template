package com.ax.template.authblueprint.apikey;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    Optional<ApiKey> findByIdAndOwnerUserId(UUID id, String ownerUserId);

    List<ApiKey> findByOwnerUserIdOrderByCreatedAtDesc(String ownerUserId);

    /**
     * Fast lookup used by the authentication filter — narrowed by the {@code hashPrefix}
     * so the constant-time hash comparison in {@link ApiKeyHasher#matches} runs against
     * at most a handful of candidates (collision probability ≈ 0 in practice).
     */
    List<ApiKey> findByHashPrefix(String hashPrefix);

    long countByOwnerUserIdAndStatus(String ownerUserId, ApiKeyStatus status);
}
