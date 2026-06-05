/**
 * Search client — POST /api/v1/search (+ /index). Backend: SearchController.
 * Auth: any authenticated user (tenant scope derived from the JWT; clients never
 * pass a tenantId). Blank query -> 400 ProblemDetail.
 * Domain client (NOT a UI primitive) — composes the shared @ax/core authed fetch.
 *
 * The studio indexes an article into the search domain with
 * domain="article" and metadata carrying the objectId so a hit can deep-link to
 * the read view. `id` on the index request is optional (server generates a UUID).
 *
 * Curl-verified shapes (2026-06-05, demo@ax.dev):
 *   POST /api/v1/search        -> 200 { hits[], totalHits, page, size, processingTimeMs }
 *   POST /api/v1/search/index  -> 201 { id }
 *   POST /api/v1/search (blank) -> 400
 */
import { apiFetch } from '@ax/core';

export interface SearchHit {
  id: string;
  domain: string;
  content: string;
  metadata: string | null;
  indexedAt: string;
}

export interface SearchResultPage {
  hits: SearchHit[];
  totalHits: number;
  page: number;
  size: number;
  processingTimeMs: number;
}

export interface SearchInput {
  query: string;
  domain?: string;
  page?: number;
  size?: number;
}

export interface IndexInput {
  domain: string;
  content: string;
  /** JSON string; the studio stores { objectId, title } so a hit can deep-link. */
  metadata?: string;
}

export const searchClient = {
  search: (input: SearchInput): Promise<SearchResultPage> =>
    apiFetch<SearchResultPage>('/v1/search', {
      method: 'POST',
      body: {
        query: input.query,
        domain: input.domain,
        page: input.page ?? 0,
        size: input.size ?? 20,
      },
    }),

  index: (input: IndexInput): Promise<{ id: string }> =>
    apiFetch<{ id: string }>('/v1/search/index', {
      method: 'POST',
      body: { domain: input.domain, content: input.content, metadata: input.metadata },
    }),
};
