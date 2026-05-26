package com.ax.template.authblueprint.favoritesbookmarks;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.ax.template.authblueprint.favoritesbookmarks.FavoriteDtos.AddFavoriteRequest;
import com.ax.template.authblueprint.favoritesbookmarks.FavoriteDtos.FavoriteListResponse;
import com.ax.template.authblueprint.favoritesbookmarks.FavoriteDtos.FavoriteResponse;

@Service
public class FavoriteService {

    private final FavoriteRepository repository;
    private final FavoritesProperties properties;
    private final Clock clock;

    public FavoriteService(FavoriteRepository repository,
                           FavoritesProperties properties,
                           Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * R78 iter1 F4 (HIGH) — {@code request.note()} is stored VERBATIM on
     * the row without scrubbing. See {@link Favorite#note} for the PII
     * disclosure contract; fork-receivers wiring downstream audit /
     * analytics / cross-user features MUST treat the note as user-owned
     * potentially-PII free-text and apply their privacy policy at the
     * downstream surface.
     *
     * <p>R78 iter1 F3 (MEDIUM) — concurrent same-key adds under default
     * READ_COMMITTED isolation race past this find-or-create check and
     * raise {@code DataIntegrityViolationException} at commit. The
     * controller {@code handleConcurrentDuplicate} ExceptionHandler
     * translates that to a 409 + {@code FAVORITE_CONCURRENT_DUPLICATE}
     * code so the client can retry idempotently.
     *
     * <p>R78 iter1 F2 (scope_deferral) — quota TOCTOU between
     * {@link #enforceQuota} and {@link FavoriteRepository#save} is
     * intentionally NOT closed in the catalog. Fork-receivers choose
     * their concurrency model (pessimistic row lock on the count query,
     * application-level user-scoped lock, database trigger, advisory
     * lock). Bumping {@code favorites-bookmarks.max-favorites-per-user}
     * significantly without one of these strategies risks a silent
     * quota breach under burst load.
     */
    @Transactional
    public AddResult add(String userId, AddFavoriteRequest request) {
        return repository.findByUserIdAndEntityTypeAndEntityId(userId, request.entityType(), request.entityId())
            .map(existing -> new AddResult(FavoriteResponse.from(existing), false))
            .orElseGet(() -> {
                enforceQuota(userId);
                Favorite row = Favorite.builder()
                    .userId(userId)
                    .entityType(request.entityType())
                    .entityId(request.entityId())
                    .note(request.note())
                    .favoritedAt(Instant.now(clock))
                    .build();
                return new AddResult(FavoriteResponse.from(repository.save(row)), true);
            });
    }

    @Transactional
    public void remove(String userId, String entityType, String entityId) {
        repository.deleteByUserAndEntity(userId, entityType, entityId);
    }

    @Transactional(readOnly = true)
    public FavoriteListResponse list(String userId, String entityType) {
        List<Favorite> rows = (entityType == null || entityType.isBlank())
            ? repository.findByUserIdOrderByFavoritedAtDesc(userId)
            : repository.findByUserIdAndEntityTypeOrderByFavoritedAtDesc(userId, entityType);
        List<FavoriteResponse> items = rows.stream().map(FavoriteResponse::from).toList();
        return new FavoriteListResponse(items, items.size());
    }

    @Transactional(readOnly = true)
    public boolean isFavorited(String userId, String entityType, String entityId) {
        return repository.findByUserIdAndEntityTypeAndEntityId(userId, entityType, entityId).isPresent();
    }

    @Transactional(readOnly = true)
    public long count(String entityType, String entityId) {
        return repository.countByEntityTypeAndEntityId(entityType, entityId);
    }

    private void enforceQuota(String userId) {
        long current = repository.countByUserId(userId);
        if (current >= properties.getMaxFavoritesPerUser()) {
            throw new FavoritesQuotaExceededException(
                "favorites quota exceeded: " + current + " / " + properties.getMaxFavoritesPerUser());
        }
    }

    public record AddResult(FavoriteResponse response, boolean created) {}
}
