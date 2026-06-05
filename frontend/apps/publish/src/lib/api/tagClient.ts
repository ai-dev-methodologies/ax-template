/**
 * Tag client — /api/tags. Backend: TagController.
 * Auth: GET (list/get/by-entity/attach/detach) is any authenticated user;
 * definition writes (POST/PUT/DELETE /api/tags[/{id}]) additionally require
 * ROLE_ADMIN (@PreAuthorize). The demo account is ADMIN, so the tags screen can
 * create/edit/delete; attach/detach to an article works for any user.
 * Domain client (NOT a UI primitive) — composes the shared @ax/core authed fetch.
 *
 * The article<->tag link is polymorphic: an attachment is keyed by
 * (entityType, entityId). This studio uses entityType="article" + the item id.
 *
 * Curl-verified shapes (2026-06-05, demo@ax.dev ADMIN):
 *   GET    /api/tags                                   -> 200 { items[], totalElements }
 *   POST   /api/tags                                   -> 201 TagResponse (slug auto-generated)
 *   PUT    /api/tags/{id}                              -> 200 TagResponse
 *   DELETE /api/tags/{id}?cascade=false               -> 204
 *   POST   /api/tags/{id}/attach {entityType,entityId} -> 200|201 TagAttachmentResponse
 *   DELETE /api/tags/{id}/attach/{entityType}/{entityId} -> 204
 *   GET    /api/tags/by-entity/{entityType}/{entityId} -> 200 { items[], totalElements }
 */
import { apiFetch } from '@ax/core';

const ARTICLE_ENTITY = 'article';

export interface Tag {
  id: string;
  name: string;
  slug: string;
  parentTagId: string | null;
  color: string | null;
  createdAt: string;
  createdByUserId: string;
}

export interface TagList {
  items: Tag[];
  totalElements: number;
}

export interface CreateTagInput {
  name: string;
  color?: string;
  parentTagId?: string;
}

export interface UpdateTagInput {
  name?: string;
  color?: string;
}

export const tagClient = {
  list: (): Promise<TagList> => apiFetch<TagList>('/tags'),

  create: (input: CreateTagInput): Promise<Tag> =>
    apiFetch<Tag>('/tags', { method: 'POST', body: input }),

  update: (id: string, input: UpdateTagInput): Promise<Tag> =>
    apiFetch<Tag>(`/tags/${id}`, { method: 'PUT', body: input }),

  remove: (id: string): Promise<void> =>
    apiFetch<void>(`/tags/${id}`, { method: 'DELETE', query: { cascade: 'false' } }),

  // ── article <-> tag links ────────────────────────────────────────────────

  forArticle: (articleId: string): Promise<TagList> =>
    apiFetch<TagList>(`/tags/by-entity/${ARTICLE_ENTITY}/${articleId}`),

  attachToArticle: (tagId: string, articleId: string): Promise<unknown> =>
    apiFetch<unknown>(`/tags/${tagId}/attach`, {
      method: 'POST',
      body: { entityType: ARTICLE_ENTITY, entityId: articleId },
    }),

  detachFromArticle: (tagId: string, articleId: string): Promise<void> =>
    apiFetch<void>(`/tags/${tagId}/attach/${ARTICLE_ENTITY}/${articleId}`, {
      method: 'DELETE',
    }),
};
