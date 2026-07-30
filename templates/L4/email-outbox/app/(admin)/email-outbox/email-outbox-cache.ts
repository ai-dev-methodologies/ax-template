/*
---
template_id: L4/email-outbox/app/(admin)/email-outbox/email-outbox-cache
layer: L4
domain: email-outbox
domain_mode: full_trio
evidence:
  - source_type: internal
    rationale: "Cross-family P1 finding: the Delete mutation's onMutate read/wrote the query
      cache as a bare OutboxResponse[] (`old.filter(...)`), but the cache actually holds an
      OutboxPage pagination envelope whose rows live under `content` — `old.filter` does not
      exist on that shape, so onMutate threw a TypeError synchronously, before mutationFn ever
      ran, breaking every loaded-page Delete. Extracted to its own react-query-free pure
      function (same rationale as email-outbox-view.tsx's presentational extraction) so the
      fix is directly vitest-importable: page.tsx's @tanstack/react-query import cannot
      resolve for a module living in templates/L4/... outside frontend/ (see
      email-outbox-view.tsx's file-header note on the identical dependency-resolution gap)."
  - source_type: external
    citation: "TanStack Query v5 — Optimistic Updates via the cache (setQueryData in onMutate)"
    url: "https://tanstack.com/query/latest/docs/framework/react/guides/optimistic-updates"
provenance_class: internal_design
imports_from: []
imports_forbidden: [L1, L2, L3]
---
*/
import type { OutboxPage } from './email-outbox-view'

/**
 * applyOptimisticDelete — pure cache transform for the Delete mutation's
 * onMutate step (EMAIL-FE-007 / optimistic-update-snapshot-rollback).
 *
 * The query cache holds an OutboxPage pagination envelope
 * (`content: OutboxResponse[]` + `page`/`size`/`totalElements`/`totalPages`),
 * not a bare array. This filters the target row out of `content` and
 * decrements `totalElements` (floored at 0), preserving every other envelope
 * field untouched — mirrors the same convention used by the
 * favorites-bookmarks `remove` mutation's onMutate.
 */
export function applyOptimisticDelete(
  old: OutboxPage | undefined,
  id: string,
): OutboxPage | undefined {
  if (!old) return old
  return {
    ...old,
    content: old.content.filter((r) => r.id !== id),
    totalElements: Math.max(0, old.totalElements - 1),
  }
}
