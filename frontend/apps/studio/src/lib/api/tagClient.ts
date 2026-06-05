/**
 * Tag-categorization client — /api/tags. Backend: TagController.
 * Domain client (NOT a UI primitive) — composes the shared @ax/core authed fetch.
 *
 * The studio uses tags as COLLECTIONS: a media item is grouped into collections
 * by attaching tags (entityType "file"); the Collections screen lists tags and
 * resolves each to its attached media via by-entity / the favorites index.
 *
 * Auth: list / get / attach / detach / by-entity are open to any authenticated
 * user; create / update / delete are ROLE_ADMIN (the demo is ADMIN, so creating
 * a collection works in this app).
 *
 * Curl-verified shapes (2026-06-05, demo@ax.dev ADMIN):
 *   GET    /api/tags                                  -> 200 { items, totalElements }
 *   POST   /api/tags                                  -> 201 TagResponse   (ADMIN)
 *   POST   /api/tags/{id}/attach                      -> 201/200 TagAttachmentResponse
 *   DELETE /api/tags/{id}/attach/{entityType}/{id}    -> 204
 *   GET    /api/tags/by-entity/{entityType}/{id}      -> 200 { items, totalElements }
 */
import { apiFetch } from '@ax/core';

export interface Tag {
  id: string;
  name: string;
  slug: string;
  parentTagId: string | null;
  color: string | null;
  createdAt: string;
  createdByUserId: string;
}

export interface TagListResult {
  items: Tag[];
  totalElements: number;
}

export interface TagAttachment {
  id: string;
  tagId: string;
  entityType: string;
  entityId: string;
  attachedAt: string;
  attachedByUserId: string;
}

export interface CreateTagInput {
  name: string;
  color?: string;
  parentTagId?: string;
}

export interface AttachTagInput {
  entityType: string;
  entityId: string;
}

export const tagClient = {
  list: (parent?: string): Promise<TagListResult> =>
    apiFetch<TagListResult>('/tags', { query: { parent } }),

  get: (id: string): Promise<Tag> => apiFetch<Tag>(`/tags/${encodeURIComponent(id)}`),

  create: (body: CreateTagInput): Promise<Tag> =>
    apiFetch<Tag>('/tags', { method: 'POST', body }),

  attach: (tagId: string, body: AttachTagInput): Promise<TagAttachment> =>
    apiFetch<TagAttachment>(`/tags/${encodeURIComponent(tagId)}/attach`, { method: 'POST', body }),

  detach: (tagId: string, entityType: string, entityId: string): Promise<void> =>
    apiFetch<void>(
      `/tags/${encodeURIComponent(tagId)}/attach/${encodeURIComponent(entityType)}/${encodeURIComponent(entityId)}`,
      { method: 'DELETE' },
    ),

  byEntity: (entityType: string, entityId: string): Promise<TagListResult> =>
    apiFetch<TagListResult>(
      `/tags/by-entity/${encodeURIComponent(entityType)}/${encodeURIComponent(entityId)}`,
    ),
};
