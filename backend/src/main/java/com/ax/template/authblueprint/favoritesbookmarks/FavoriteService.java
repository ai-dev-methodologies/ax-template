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
