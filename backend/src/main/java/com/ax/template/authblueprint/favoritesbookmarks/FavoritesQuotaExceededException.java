package com.ax.template.authblueprint.favoritesbookmarks;

/** FAV-VALID-002 — mapped to HTTP 400 FAVORITES_QUOTA_EXCEEDED. */
public class FavoritesQuotaExceededException extends RuntimeException {
    public FavoritesQuotaExceededException(String detail) {
        super(detail);
    }
}
