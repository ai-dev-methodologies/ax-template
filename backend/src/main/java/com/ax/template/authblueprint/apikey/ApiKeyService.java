package com.ax.template.authblueprint.apikey;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Orchestrates the api-key lifecycle. Owns the boundary between the controller
 * (HTTP concerns) and the persistence + hashing primitives.
 *
 * <p>Trace:
 * <ul>
 *   <li>KEY-AUTHN-001 — {@link #create} / {@link #rotate} return the plaintext
 *       exactly once; no other entry point returns or persists it.</li>
 *   <li>KEY-AUTHZ-002 — every lookup uses
 *       {@link ApiKeyRepository#findByIdAndOwnerUserId}.</li>
 *   <li>KEY-LIFECYCLE-002 — rotation runs in a single {@code @Transactional}
 *       method; old + new visible together or not at all.</li>
 * </ul>
 */
@Service
public class ApiKeyService {

    private final ApiKeyRepository repository;
    private final ApiKeyProperties properties;
    private final Clock clock;

    public ApiKeyService(ApiKeyRepository repository,
                         ApiKeyProperties properties,
                         Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public CreateApiKeyResponse create(String ownerUserId, CreateApiKeyRequest request) {
        enforceQuota(ownerUserId);
        Set<ApiKeyScope> scopes = resolveScopes(request);
        Instant expiresAt = resolveExpiresAt(request);
        return issueNew(ownerUserId, request.name(), scopes, expiresAt);
    }

    @Transactional(readOnly = true)
    public ApiKeyListResponse list(String ownerUserId) {
        List<ApiKey> rows = repository.findByOwnerUserIdOrderByCreatedAtDesc(ownerUserId);
        return new ApiKeyListResponse(
            rows.stream().map(ApiKeyResponse::from).toList(),
            rows.size()
        );
    }

    @Transactional(readOnly = true)
    public ApiKeyResponse get(String ownerUserId, UUID id) {
        return ApiKeyResponse.from(loadOwned(ownerUserId, id));
    }

    @Transactional
    public void revoke(String ownerUserId, UUID id) {
        ApiKey key = loadOwned(ownerUserId, id);
        key.markRevoked(Instant.now(clock));
        repository.save(key);
    }

    /**
     * Atomic rotation — KEY-LIFECYCLE-002. The old key is revoked AND a new row is
     * inserted within the same transaction; if either fails, the whole rotation
     * rolls back and the caller still holds a working old key.
     */
    @Transactional
    public CreateApiKeyResponse rotate(String ownerUserId, UUID id) {
        ApiKey old = loadOwned(ownerUserId, id);
        Set<ApiKeyScope> scopes = old.getScopes();
        Instant expiresAt = old.getExpiresAt();
        String name = old.getName();
        old.markRevoked(Instant.now(clock));
        repository.save(old);
        return issueNew(ownerUserId, name, scopes, expiresAt);
    }

    /** Filter touchpoint — records the moment of last successful use. */
    @Transactional
    public void touchLastUsed(UUID id) {
        Optional<ApiKey> opt = repository.findById(id);
        opt.ifPresent(k -> {
            k.touchLastUsed(Instant.now(clock));
            repository.save(k);
        });
    }

    /**
     * Authentication-time lookup: returns the matching {@link ApiKey} iff a row
     * exists with the same {@code hashPrefix} AND its stored hash matches the
     * provided plaintext under constant-time comparison AND it is currently
     * {@link ApiKey#isActive(Instant)} (status=ACTIVE and not expired).
     */
    @Transactional(readOnly = true)
    public Optional<ApiKey> resolvePlaintext(String plaintext) {
        if (plaintext == null || plaintext.length() < ApiKeyHasher.HASH_PREFIX_LENGTH) {
            return Optional.empty();
        }
        String prefix = ApiKeyHasher.prefixOf(plaintext);
        Instant now = Instant.now(clock);
        return repository.findByHashPrefix(prefix).stream()
            .filter(k -> ApiKeyHasher.matches(plaintext, k.getHashedValue()))
            .filter(k -> k.isActive(now))
            .findFirst();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private ApiKey loadOwned(String ownerUserId, UUID id) {
        return repository.findByIdAndOwnerUserId(id, ownerUserId)
            .orElseThrow(() -> new ApiKeyNotFoundException(id));
    }

    private void enforceQuota(String ownerUserId) {
        long active = repository.countByOwnerUserIdAndStatus(ownerUserId, ApiKeyStatus.ACTIVE);
        if (active >= properties.getMaxKeysPerUser()) {
            throw new TooManyApiKeysException(
                "active api key quota exceeded: " + active + " / " + properties.getMaxKeysPerUser());
        }
    }

    private Set<ApiKeyScope> resolveScopes(CreateApiKeyRequest request) {
        if (request == null || request.scopes() == null || request.scopes().isEmpty()) {
            return EnumSet.of(ApiKeyScope.READ);
        }
        return EnumSet.copyOf(request.scopes());
    }

    private Instant resolveExpiresAt(CreateApiKeyRequest request) {
        Integer days = (request == null) ? null : request.expiresInDays();
        int useDays = (days == null || days <= 0) ? properties.getDefaultExpiresInDays() : days;
        return Instant.now(clock).plus(useDays, ChronoUnit.DAYS);
    }

    private CreateApiKeyResponse issueNew(String ownerUserId,
                                          String name,
                                          Set<ApiKeyScope> scopes,
                                          Instant expiresAt) {
        String plaintext = ApiKeyHasher.newPlaintext();
        ApiKey key = ApiKey.builder()
            .ownerUserId(ownerUserId)
            .name(name)
            .hashPrefix(ApiKeyHasher.prefixOf(plaintext))
            .hashedValue(ApiKeyHasher.hash(plaintext))
            .scopes(scopes)
            .status(ApiKeyStatus.ACTIVE)
            .createdAt(Instant.now(clock))
            .expiresAt(expiresAt)
            .build();
        ApiKey saved = repository.save(key);
        return CreateApiKeyResponse.from(saved, plaintext);
    }
}
