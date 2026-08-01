import { describe, it, expect } from 'vitest'
import { applyOptimisticDelete } from '../../templates/L4/email-outbox/app/(admin)/email-outbox/email-outbox-cache'
import type { OutboxPage } from '../../templates/L4/email-outbox/app/(admin)/email-outbox/email-outbox-view'

// P1 hotfix (final4 wave) — cross-family reviewer finding: the Delete mutation's onMutate
// read/wrote the query cache as `OutboxResponse[]` (`old.filter(...)`), but the cache actually
// holds an OutboxPage pagination envelope whose rows live under `content`. `OutboxPage.filter`
// does not exist, so onMutate threw a TypeError synchronously — BEFORE mutationFn ever ran —
// breaking every loaded-page Delete. The prior email-outbox-view.vitest.tsx suite never caught
// this because it only renders EmailOutboxView (the pure props->JSX component) and never
// exercises the mutation's cache-write logic. This file targets that logic directly via
// applyOptimisticDelete, the react-query-free pure function page.tsx's onMutate now delegates
// to (extracted precisely so it CAN be vitest-imported — see email-outbox-cache.ts's
// file-header note on why page.tsx itself is not directly importable from this test project).

const BASE_PAGE: OutboxPage = {
  content: [
    {
      id: 'row_1',
      recipient: 'user@example.com',
      templateCode: 'welcome',
      subject: 'Welcome!',
      status: 'DLQ',
      retryCount: 3,
      nextAttemptAt: null,
      lastError: 'raw stack trace with a secret',
      createdAt: '2026-07-20T10:00:00Z',
      sentAt: null,
    },
    {
      id: 'row_2',
      recipient: 'other@example.com',
      templateCode: 'receipt',
      subject: 'Your receipt',
      status: 'SENT',
      retryCount: 0,
      nextAttemptAt: null,
      lastError: null,
      createdAt: '2026-07-20T09:00:00Z',
      sentAt: '2026-07-20T09:00:05Z',
    },
  ],
  page: 0,
  size: 20,
  totalElements: 2,
  totalPages: 1,
}

// Reproduces the PRE-FIX onMutate cache write verbatim: it assumed the cache
// held a bare OutboxResponse[] and called `.filter` directly on whatever
// `getQueryData` returned. Run against the REAL OutboxPage envelope shape,
// `.filter` is not a function on that object — this is the exact TypeError
// the cross-family reviewer reproduced.
function preFixOnMutateCacheWrite(old: OutboxPage | undefined, id: string): unknown {
  const cache = old as unknown as { filter?: (fn: (r: { id: string }) => boolean) => unknown }
  return cache ? cache.filter!((r) => r.id !== id) : cache
}

describe('email-outbox delete mutation cache transform (EMAIL-FE-007 optimistic-update-snapshot-rollback)', () => {
  it('REGRESSION: the pre-fix array-shaped onMutate throws on the real OutboxPage envelope', () => {
    expect(() => preFixOnMutateCacheWrite(BASE_PAGE, 'row_1')).toThrow(TypeError)
    expect(() => preFixOnMutateCacheWrite(BASE_PAGE, 'row_1')).toThrow(/filter is not a function/)
  })

  it('applyOptimisticDelete filters the target row out of content and decrements totalElements', () => {
    const next = applyOptimisticDelete(BASE_PAGE, 'row_1')
    expect(next?.content.map((r) => r.id)).toEqual(['row_2'])
    expect(next?.totalElements).toBe(1)
  })

  it('preserves the rest of the pagination envelope untouched', () => {
    const next = applyOptimisticDelete(BASE_PAGE, 'row_1')
    expect(next?.page).toBe(BASE_PAGE.page)
    expect(next?.size).toBe(BASE_PAGE.size)
    expect(next?.totalPages).toBe(BASE_PAGE.totalPages)
  })

  it('floors totalElements at 0 instead of going negative', () => {
    const empty: OutboxPage = { ...BASE_PAGE, content: [], totalElements: 0 }
    const next = applyOptimisticDelete(empty, 'row_1')
    expect(next?.totalElements).toBe(0)
  })

  // ── P3-106 — absent id must not decrement ────────────────────────────────
  // The decrement used to be unconditional (clamped at 0), so an id that is not on
  // the cached page — a stale row, a row on another page, a double-fired confirm —
  // silently understated the total while the list stayed the same length. The count
  // and the rows disagreed until the onSettled re-fetch landed.

  it('P3-106: deleting an id that is NOT on the cached page leaves totalElements alone', () => {
    const next = applyOptimisticDelete(BASE_PAGE, 'row_on_another_page')
    expect(next?.content.map((r) => r.id)).toEqual(['row_1', 'row_2'])
    expect(next?.totalElements).toBe(BASE_PAGE.totalElements)
  })

  it('P3-106: an absent id whose count is NOT at the floor still does not decrement', () => {
    // The pre-fix bug is invisible whenever Math.max(0, …) clamps. This page's
    // totalElements (57 across pages) is far from 0, so an unconditional decrement
    // would show up as 56 here — the clamp cannot mask it.
    const multiPage: OutboxPage = { ...BASE_PAGE, totalElements: 57, totalPages: 3 }
    const next = applyOptimisticDelete(multiPage, 'row_on_another_page')
    expect(next?.totalElements).toBe(57)
  })

  it('P3-106: a no-op delete returns the SAME object, so no re-render is triggered', () => {
    expect(applyOptimisticDelete(BASE_PAGE, 'nope')).toBe(BASE_PAGE)
  })

  it('P3-106 NON-VACUITY: a PRESENT id still decrements exactly once', () => {
    const multiPage: OutboxPage = { ...BASE_PAGE, totalElements: 57, totalPages: 3 }
    const next = applyOptimisticDelete(multiPage, 'row_1')
    expect(next?.totalElements).toBe(56)
    expect(next).not.toBe(multiPage)
  })

  it('returns old (undefined) unchanged when there is no cached page yet', () => {
    expect(applyOptimisticDelete(undefined, 'row_1')).toBeUndefined()
  })

  it('NON-VACUITY: deleting the OTHER row id leaves the opposite row surviving', () => {
    const next = applyOptimisticDelete(BASE_PAGE, 'row_2')
    expect(next?.content.map((r) => r.id)).toEqual(['row_1'])
    expect(next?.totalElements).toBe(1)
  })
})
