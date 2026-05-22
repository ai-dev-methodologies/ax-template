package com.ax.template.authblueprint.favoritesbookmarks;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "favorites-bookmarks")
public class FavoritesProperties {

    /** FAV-VALID-002 — soft cap; new add beyond is rejected with FAVORITES_QUOTA_EXCEEDED. */
    private int maxFavoritesPerUser = 1000;

    public int getMaxFavoritesPerUser() { return maxFavoritesPerUser; }
    public void setMaxFavoritesPerUser(int v) { this.maxFavoritesPerUser = v; }
}
