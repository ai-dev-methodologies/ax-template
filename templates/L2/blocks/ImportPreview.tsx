/*
---
template_id: L2/blocks/ImportPreview
layer: L2
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "L2 data block — shows a preview table of parsed import rows before commit; onConfirm/onCancel injected by L4."
dependencies: [button, table]
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/
import * as React from 'react'

export interface ImportColumn {
  key: string
  label: string
}

export interface ImportError {
  rowNumber: number
  message: string
}

export interface ImportPreviewProps {
  columns: ImportColumn[]
  rows: Record<string, string>[]
  totalRows: number
  errors: ImportError[]
  /** Maximum rows to display in the preview table (default: 20) */
  previewLimit?: number
  onConfirm: () => void
  onCancel: () => void
  isConfirming?: boolean
}

/**
 * Preview table shown before the user finalises a CSV/Excel import.
 * Fully controlled — no API calls. L4 handles data fetching and the import action.
 */
export function ImportPreview({
  columns,
  rows,
  totalRows,
  errors,
  previewLimit = 20,
  onConfirm,
  onCancel,
  isConfirming = false,
}: ImportPreviewProps) {
  const displayRows = rows.slice(0, previewLimit)
  const hasErrors = errors.length > 0

  return (
    <div className="import-preview">
      <header className="import-preview__header">
        <h2 className="import-preview__title">Import Preview</h2>
        <p className="import-preview__summary">
          {totalRows} rows total
          {hasErrors && (
            <span className="import-preview__error-count">
              {' '}· {errors.length} error{errors.length !== 1 ? 's' : ''}
            </span>
          )}
          {displayRows.length < totalRows && (
            <span className="import-preview__truncation">
              {' '}(showing first {displayRows.length})
            </span>
          )}
        </p>
      </header>

      <div className="import-preview__table-wrapper" role="region" aria-label="Import data preview">
        <table className="import-preview__table">
          <thead>
            <tr>
              {columns.map((col) => (
                <th key={col.key} scope="col">
                  {col.label}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {displayRows.map((row, idx) => (
              <tr key={idx}>
                {columns.map((col) => (
                  <td key={col.key}>{row[col.key] ?? ''}</td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {hasErrors && (
        <section className="import-preview__errors" aria-label="Row validation errors">
          <h3 className="import-preview__errors-title">Validation errors</h3>
          <ul className="import-preview__error-list">
            {errors.slice(0, 10).map((err) => (
              <li key={err.rowNumber} className="import-preview__error-item">
                <span className="import-preview__error-row">Row {err.rowNumber}:</span>{' '}
                {err.message}
              </li>
            ))}
            {errors.length > 10 && (
              <li className="import-preview__error-more">
                …and {errors.length - 10} more
              </li>
            )}
          </ul>
        </section>
      )}

      <footer className="import-preview__actions">
        <button
          type="button"
          className="import-preview__cancel"
          onClick={onCancel}
          disabled={isConfirming}
        >
          Cancel
        </button>
        <button
          type="button"
          className="import-preview__confirm"
          onClick={onConfirm}
          disabled={isConfirming || totalRows === 0}
          aria-busy={isConfirming}
        >
          {isConfirming
            ? 'Importing…'
            : `Import ${totalRows - errors.length} row${totalRows - errors.length !== 1 ? 's' : ''}`}
        </button>
      </footer>
    </div>
  )
}
