/*
---
template_id: L2/blocks/bulk-result-panel
layer: L2
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "FDW1 (frontend dogfood) rule-of-three: all 3 personas hand-rolled an identical succeeded/failed panel for a bulk mutation. bulk-actions-bar is the toolbar only; bulk-export is file export, not a per-item mutation result. The backend already ships the canonical per-item partial-success contract (common/BulkResult + BulkItemResult + BulkItemError, task #37 / bulk-operation-l0) with NO frontend counterpart. This L2 block consumes that exact shape so a 207 multi-status response renders without re-derivation."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/
import * as React from 'react'

/** Mirror of backend common/BulkItemError — a per-item error core. */
export interface BulkItemError {
  code: string
  message: string
}

/** Mirror of backend common/BulkItemResult<R>. */
export interface BulkItemResult<R = unknown> {
  index: number
  status: 'ok' | 'error'
  result?: R | null
  error?: BulkItemError | null
}

/** Mirror of backend common/BulkResult<R> (207-style partial success). */
export interface BulkResult<R = unknown> {
  total: number
  succeeded: number
  failed: number
  items: BulkItemResult<R>[]
}

export interface BulkResultPanelProps<R = unknown> {
  result: BulkResult<R>
  /**
   * Optional human label for an item (e.g. the row's name) given its result
   * entry — falls back to `Item #{index}`. Lets the panel name failures
   * without the block owning the row model.
   */
  itemLabel?: (item: BulkItemResult<R>) => React.ReactNode
  /** Heading text. Default: "Bulk operation result". */
  title?: string
}

/**
 * BulkResultPanel — render a backend `BulkResult` (per-item partial success).
 * Shows an aria-live summary (succeeded / failed / total) and, when any item
 * failed, an accessible list of the failures with their code + message. Purely
 * presentational and prop-driven (L2): the L4 owner runs the batch and passes
 * the parsed `BulkResult`.
 */
export default function BulkResultPanel<R = unknown>({
  result,
  itemLabel,
  title = 'Bulk operation result',
}: BulkResultPanelProps<R>) {
  const { total, succeeded, failed, items } = result
  const failures = items.filter((it) => it.status === 'error')
  const hasFailures = failed > 0 || failures.length > 0

  return (
    <section
      className="rounded-md border border-border bg-background p-4 text-sm"
      aria-labelledby="bulk-result-title"
    >
      <h2 id="bulk-result-title" className="mb-2 font-medium">
        {title}
      </h2>

      <p
        role="status"
        aria-live="polite"
        className={
          hasFailures
            ? 'text-amber-700 dark:text-amber-400'
            : 'text-emerald-700 dark:text-emerald-400'
        }
      >
        {succeeded} succeeded, {failed} failed of {total}
      </p>

      {hasFailures && (
        <ul role="list" className="mt-3 space-y-1.5">
          {failures.map((it) => (
            <li
              key={it.index}
              className="rounded-sm bg-destructive/10 px-2 py-1.5 text-destructive"
            >
              <span className="font-medium">
                {itemLabel ? itemLabel(it) : `Item #${it.index}`}
              </span>
              {it.error && (
                <>
                  {' — '}
                  <code className="text-xs">{it.error.code}</code>
                  {': '}
                  {it.error.message}
                </>
              )}
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
