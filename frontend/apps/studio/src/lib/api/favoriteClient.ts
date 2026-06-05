/**
 * Favorites client — /api/favorites. Backend: FavoriteController.
 * Auth: any authenticated user; rows are owner-scoped by the JWT.
 * Domain client (NOT a UI primitive) — composes the shared @ax/core authed fetch.
 *
 * The studio uses favorites for TWO jobs:
 *   1. As the per-user MEDIA INDEX (entityType "file"): the file-storage domain
 *      has no list endpoint, so every upload is favorited and the gallery
 *      enumerates a creator's files by listing favorites of type "file".
 *   2. As the delightful LIKE/REACTION toggle on a media item (the same row).
 *
 * Curl-verified shapes (2026-06-05, demo@ax.dev):
 *   POST   /api/favorites                          -> 201/200 FavoriteResponse
 *   GET    /api/favorites?entityType=file          -> 200 { items, totalElements }
 *   DELETE /api/favorites/{entityType}/{id}        -> 204
 *   GET    /api/favorites/check/{type}/{id}        -> 200 { favorited }
 *   GET    /api/favorites/count/{type}/{id}        -> 200 { count }
 */
import { apiFetch } from '@ax/core';

export interface Favorite {
  id: string;
  entityType: string;
  entityId: string;
  note: string | null;
  favoritedAt: string;
}

export interface FavoriteListResult {
  items: Favorite[];
  totalElements: number;
}

export interface AddFavoriteInput {
  entityType: string;
  entityId: string;
  note?: string;
}

export const favoriteClient = {
  list: (entityType?: string): Promise<FavoriteListResult> =>
    apiFetch<FavoriteListResult>('/favorites', { query: { entityType } }),

  add: (body: AddFavoriteInput): Promise<Favorite> =>
    apiFetch<Favorite>('/favorites', { method: 'POST', body }),

  remove: (entityType: string, entityId: string): Promise<void> =>
    apiFetch<void>(`/favorites/${encodeURIComponent(entityType)}/${encodeURIComponent(entityId)}`, {
      method: 'DELETE',
    }),

  check: (entityType: string, entityId: string): Promise<{ favorited: boolean }> =>
    apiFetch<{ favorited: boolean }>(
      `/favorites/check/${encodeURIComponent(entityType)}/${encodeURIComponent(entityId)}`,
    ),

  count: (entityType: string, entityId: string): Promise<{ count: number }> =>
    apiFetch<{ count: number }>(
      `/favorites/count/${encodeURIComponent(entityType)}/${encodeURIComponent(entityId)}`,
    ),
};
