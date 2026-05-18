/*
---
template_id: L2/blocks/ImportProgressBar
layer: L2
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "L2 data block — displays real-time import progress; polling or SSE data supplied by L4; fully controlled."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/
import * as React from 'react'

export type ImportStatus = 'pending' | 'running' | 'completed' | 'failed'

export interface ImportProgressBarProps {
  importedCount: number
  totalRows: number
  errorCount: number
  status: ImportStatus
  onDismiss?: () => void
}

/**
 * Real-time import progress bar.
 * Fully controlled — L4 supplies counts from polling or SSE.
 */
export function ImportProgressBar({
  importedCount,
  totalRows,
  errorCount,
  status,
  onDismiss,
}: ImportProgressBarProps) {
  const pct = totalRows > 0 ? Math.min(100, Math.round((importedCount / totalRows) * 100)) : 0
  const isActive = status === 'running' || status === 'pending'
  const isDone = status === 'completed' || status === 'failed'

  return (
    <div
      className={`import-progress import-progress--${status}`}
      role="region"
      aria-label="Import progress"
    >
      <div className="import-progress__header">
        <span className="import-progress__label">
          {status === 'pending' && 'Import queued…'}
          {status === 'running' && `Importing ${importedCount} / ${totalRows} rows`}
          {status === 'completed' && `Import complete — ${importedCount} rows imported`}
          {status === 'failed' && 'Import failed'}
        </span>

        {isActive && (
          <span className="import-progress__spinner" aria-hidden="true" />
        )}

        {isDone && onDismiss && (
          <button
            type="button"
            className="import-progress__dismiss"
            onClick={onDismiss}
            aria-label="Dismiss import result"
          >
            ✕
          </button>
        )}
      </div>

      <div
        role="progressbar"
        aria-valuenow={pct}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-label={`${pct}% complete`}
        className="import-progress__track"
      >
        <div
          className="import-progress__bar"
          style={{ width: `${pct}%` }}
        />
      </div>

      {errorCount > 0 && (
        <p
          className="import-progress__errors"
          aria-live="polite"
        >
          {errorCount} row{errorCount !== 1 ? 's' : ''} skipped due to errors
        </p>
      )}
    </div>
  )
}
