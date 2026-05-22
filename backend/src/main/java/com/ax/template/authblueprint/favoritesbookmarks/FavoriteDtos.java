package com.ax.template.authblueprint.favoritesbookmarks;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class FavoriteDtos {

    private FavoriteDtos() {}

    public record AddFavoriteRequest(
        @NotBlank @Size(max = 64) String entityType,
        @NotBlank @Size(max = 255) String entityId,
        @Size(max = 256) String note
    ) {}

    public record FavoriteResponse(
        UUID id,
        String entityType,
        String entityId,
        String note,
        Instant favoritedAt
    ) {
        public static FavoriteResponse from(Favorite f) {
            return new FavoriteResponse(
                f.getId(), f.getEntityType(), f.getEntityId(), f.getNote(), f.getFavoritedAt());
        }
    }

    public record FavoriteListResponse(List<FavoriteResponse> items, long totalElements) {}

    public record CheckFavoriteResponse(boolean favorited) {}

    public record CountFavoriteResponse(long count) {}
}
