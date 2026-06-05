/**
 * Search client — POST /api/v1/search.
 * Backend: SearchController. Auth: any authenticated user (tenant scope derived
 * from the JWT; clients never pass a tenantId). Blank query -> 400 ProblemDetail.
 * Domain client (NOT a UI primitive) — composes the shared @ax/core authed fetch.
 *
 * Curl-verified shapes (2026-06-05, demo@ax.dev):
 *   POST /api/v1/search        -> 200 { hits, totalHits, page, size, processingTimeMs }
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
};
