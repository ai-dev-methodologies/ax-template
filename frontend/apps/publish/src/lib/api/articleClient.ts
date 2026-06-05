/**
 * Article client — the generic CRUD domain at /api/items (used here as
 * "articles"). Backend: ItemController. Auth: any authenticated user; rows are
 * owner-scoped by the JWT subject (clients never pass an ownerId).
 * Domain client (NOT a UI primitive) — composes the shared @ax/core authed fetch.
 *
 * An article's HTML body is stored in the item's `description` field (the editor
 * serializes Tiptap HTML into it). There is NO soft-delete on this domain, so
 * the studio has no trash screen — DELETE is hard.
 *
 * Curl-verified shapes (2026-06-05, demo@ax.dev):
 *   GET    /api/items            -> 200 PageEnvelope { data[], pagination{page,pageSize,totalElements,totalPages,hasMore} }
 *   POST   /api/items            -> 201 ItemResponse
 *   GET    /api/items/{id}       -> 200 ItemResponse
 *   PUT    /api/items/{id}       -> 200 ItemResponse
 *   DELETE /api/items/{id}       -> 204
 */
import { apiFetch } from '@ax/core';

export interface Article {
  id: string;
  /** the article headline */
  title: string;
  /** the article body — Tiptap-serialized HTML */
  description: string | null;
  createdBy: string | null;
  createdAt: string;
  updatedBy: string | null;
  updatedAt: string | null;
}

export interface Pagination {
  page: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  hasMore: boolean;
}

export interface ArticlePage {
  data: Article[];
  pagination: Pagination;
}

export interface ArticleInput {
  title: string;
  /** Tiptap-serialized HTML body. */
  description: string;
}

export const articleClient = {
  list: (page = 0, size = 20): Promise<ArticlePage> =>
    apiFetch<ArticlePage>('/items', { query: { page, size } }),

  get: (id: string): Promise<Article> => apiFetch<Article>(`/items/${id}`),

  create: (input: ArticleInput): Promise<Article> =>
    apiFetch<Article>('/items', { method: 'POST', body: input }),

  update: (id: string, input: ArticleInput): Promise<Article> =>
    apiFetch<Article>(`/items/${id}`, { method: 'PUT', body: input }),

  remove: (id: string): Promise<void> =>
    apiFetch<void>(`/items/${id}`, { method: 'DELETE' }),
};
