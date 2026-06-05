/**
 * Favorites / bookmarks client — /api/favorites (add / list / remove / check).
 * Backend: FavoriteController. Auth: any authenticated user.
 * Domain client (NOT a UI primitive) — composes the shared @ax/core authed fetch.
 *
 * Curl-verified shapes (2026-06-05, demo@ax.dev):
 *   POST   /api/favorites                      -> 201/200 FavoriteResponse
 *   GET    /api/favorites                      -> 200 { items, totalElements }
 *   DELETE /api/favorites/{entityType}/{id}    -> 204
 *   GET    /api/favorites/check/{type}/{id}    -> 200 { favorited }
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
};
