/**
 * Comment-thread client — /api/comments (create / reply / edit / delete / list / history).
 * Backend: CommentController. Auth: any authenticated user; edit is author-only,
 * delete is author-or-admin (enforced server-side). Soft-delete masks the body
 * to "[deleted]".
 * Domain client (NOT a UI primitive) — composes the shared @ax/core authed fetch.
 *
 * Curl-verified shapes (2026-06-05, demo@ax.dev):
 *   POST   /api/comments                                -> 201 CommentResponse
 *   GET    /api/comments/by-entity/{type}/{id}          -> 200 { items, totalElements }
 *   PUT    /api/comments/{id}                           -> 200 CommentResponse
 *   DELETE /api/comments/{id}                           -> 204
 *   GET    /api/comments/{id}/history                   -> 200 { commentId, edits }
 */
import { apiFetch } from '@ax/core';

// Mirrors the backend CommentStatus enum exactly: ACTIVE | DELETED. Edits are NOT a status —
// the backend keeps status=ACTIVE and only moves updatedAt, so there is no 'EDITED' value.
export type CommentStatus = 'ACTIVE' | 'DELETED';

export const DELETED_BODY_MASK = '[deleted]';

export interface Comment {
  id: string;
  authorUserId: string;
  entityType: string;
  entityId: string;
  parentCommentId: string | null;
  body: string;
  status: CommentStatus;
  createdAt: string;
  updatedAt: string | null;
  deletedAt: string | null;
  deletedByUserId: string | null;
}

export interface CommentListResult {
  items: Comment[];
  totalElements: number;
}

export interface CommentEdit {
  id: string;
  editedAt: string;
  editedByUserId: string;
  previousBody: string;
}

export interface CommentHistory {
  commentId: string;
  edits: CommentEdit[];
}

export interface CreateCommentInput {
  entityType: string;
  entityId: string;
  body: string;
  parentCommentId?: string;
}

export const commentClient = {
  byEntity: (entityType: string, entityId: string): Promise<CommentListResult> =>
    apiFetch<CommentListResult>(
      `/comments/by-entity/${encodeURIComponent(entityType)}/${encodeURIComponent(entityId)}`,
    ),

  create: (body: CreateCommentInput): Promise<Comment> =>
    apiFetch<Comment>('/comments', { method: 'POST', body }),

  edit: (id: string, newBody: string): Promise<Comment> =>
    apiFetch<Comment>(`/comments/${id}`, { method: 'PUT', body: { body: newBody } }),

  remove: (id: string): Promise<void> =>
    apiFetch<void>(`/comments/${id}`, { method: 'DELETE' }),

  history: (id: string): Promise<CommentHistory> =>
    apiFetch<CommentHistory>(`/comments/${id}/history`),
};
