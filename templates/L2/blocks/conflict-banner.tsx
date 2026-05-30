/*
---
template_id: L2/blocks/conflict-banner
layer: L2
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "FDW2 frontier: the backend emits the full optimistic-locking contract (428/412/409 + current_etag, common/OptimisticLockingSupport) but the frontend had no way to PRESENT a conflict. This is the L2 presentational half of the seam the L0 use-conflict-resolution hook opens — it consumes that hook's ConflictState<T> (your-value vs server-value vs the latest validator) and renders the read-fresh-then-reconcile choice. Mirrors bulk-result-panel (the L2 half of the BulkResult contract)."
dependencies: []
imports_from: [L0]
imports_forbidden: [L4, app/, lib/]
---
*/
'use client'

import * as React from 'react'
import type {
  ConflictKind,
  ConflictState,
} from 'templates/L0/fork-receiver-kit/use-conflict-resolution'

/** Headline + body copy per conflict kind. Presentational only. */
const COPY: Record<ConflictKind, { title: string; body: string }> = {
  'precondition-required': {
    title: 'Reload required',
    body: 'This record can only be saved against its latest version. Reload to continue editing.',
  },
  stale: {
    title: 'This record changed',
    body: 'Someone updated this record after you opened it. Review the latest values before saving.',
  },
  conflict: {
    title: 'Concurrent change',
    body: 'Another change was saved at the same time as yours. Review the latest values before retrying.',
  },
}

export interface ConflictBannerProps<T> {
  /** The active conflict from `useConflictResolution` (L0). */
  conflict: ConflictState<T>
  /**
   * Render one side of the conflict for display (your value / server value).
   * Falls back to a stable JSON string so the banner works without a renderer.
   */
  renderValue?: (value: T) => React.ReactNode
  /** Primary action: adopt the server value / reload the fresh state. */
  onReload?: () => void
  /**
   * Optional destructive action: keep your value and re-submit against the
   * latest validator. Omit to hide (e.g. when last-write-wins is unsafe).
   */
  onOverwrite?: () => void
  /** Optional dismiss action (e.g. cancel the edit). */
  onDismiss?: () => void
}

function defaultRender<T>(value: T): React.ReactNode {
  if (value === null || value === undefined) return '—'
  if (typeof value === 'string') return value
  return JSON.stringify(value)
}

/**
 * ConflictBanner — present an optimistic-locking conflict (RFC 9110 412/409 /
 * RFC 6585 428) surfaced by the L0 `useConflictResolution` hook. Shows the
 * server value beside your value (and the authoritative validator when the
 * server reported one) plus the reload / overwrite / dismiss choice. Purely
 * presentational and prop-driven: the L4 owner runs the mutation, calls the
 * hook's `resolveFromResponse`, and passes the resulting `ConflictState` here.
 */
export default function ConflictBanner<T>({
  conflict,
  renderValue = defaultRender,
  onReload,
  onOverwrite,
  onDismiss,
}: ConflictBannerProps<T>) {
  const { kind, code, currentEtag, serverValue, yourValue } = conflict
  const copy = COPY[kind]
  const hasValidator = typeof currentEtag === 'string' && currentEtag.length > 0

  return (
    <section
      role="alert"
      aria-live="assertive"
      aria-labelledby="conflict-banner-title"
      className="rounded-md border border-amber-300 bg-amber-50 p-4 text-sm dark:border-amber-500/40 dark:bg-amber-950/30"
    >
      <h2
        id="conflict-banner-title"
        className="font-medium text-amber-900 dark:text-amber-200"
      >
        {copy.title}
      </h2>
      <p className="mt-1 text-amber-800 dark:text-amber-300">{copy.body}</p>

      <dl className="mt-3 grid grid-cols-1 gap-2 sm:grid-cols-2">
        <div className="rounded-sm bg-background/60 px-2 py-1.5">
          <dt className="text-xs font-medium text-muted-foreground">Your value</dt>
          <dd className="mt-0.5 break-words">{renderValue(yourValue)}</dd>
        </div>
        <div className="rounded-sm bg-background/60 px-2 py-1.5">
          <dt className="text-xs font-medium text-muted-foreground">Server value</dt>
          <dd className="mt-0.5 break-words">{renderValue(serverValue)}</dd>
        </div>
      </dl>

      {hasValidator ? (
        <p className="mt-2 text-xs text-muted-foreground">
          Latest version: <code className="font-mono">{currentEtag}</code>
        </p>
      ) : null}

      <div className="mt-3 flex flex-wrap gap-2">
        {onReload ? (
          <button
            type="button"
            onClick={onReload}
            className="inline-flex h-8 items-center rounded-md bg-primary px-3 text-sm font-medium text-primary-foreground hover:bg-primary/90"
          >
            Reload latest
          </button>
        ) : null}
        {onOverwrite ? (
          <button
            type="button"
            onClick={onOverwrite}
            className="inline-flex h-8 items-center rounded-md border border-destructive px-3 text-sm font-medium text-destructive hover:bg-destructive/10"
          >
            Overwrite anyway
          </button>
        ) : null}
        {onDismiss ? (
          <button
            type="button"
            onClick={onDismiss}
            className="inline-flex h-8 items-center rounded-md border border-input px-3 text-sm font-medium hover:bg-accent"
          >
            Dismiss
          </button>
        ) : null}
      </div>

      <p className="sr-only">Conflict code: {code}</p>
    </section>
  )
}
