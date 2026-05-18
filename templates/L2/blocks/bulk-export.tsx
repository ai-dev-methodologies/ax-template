/*
---
template_id: L2/blocks/bulk-export
layer: L2
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "L2 data block — multi-format table export (CSV/XLSX/PDF). CSV uses zero-dependency in-house serializer. XLSX and PDF are dynamically imported so the main chunk delta stays under 30 kB. L4 provides rows + columns; this component owns only the export trigger UI and orchestration."
  - source_type: external
    citation: "Web Performance — dynamic import() for non-critical code paths: only load the xlsx/pdf module when the user initiates an export, not on page load"
    url: "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/import"
    quoted_at: "2026-05-18"
dependencies: [button]
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/
import * as React from 'react'

// ─── types ────────────────────────────────────────────────────────────────────

export type ExportFormat = 'csv' | 'xlsx' | 'pdf'

export interface ExportColumn {
  key: string
  header: string
}

export interface BulkExportProps {
  /** Rows to export (already filtered/sorted by L4) */
  rows: Record<string, unknown>[]
  columns: ExportColumn[]
  /** Base filename without extension */
  filename?: string
  /** Available export formats; default all three */
  formats?: ExportFormat[]
  /** Called when export begins (analytics hook for observability_signal) */
  onExportStart?: (format: ExportFormat) => void
}

// ─── CSV serializer (zero dependency) ────────────────────────────────────────

function escapeCsvCell(value: unknown): string {
  const str = value == null ? '' : String(value)
  if (str.includes('"') || str.includes(',') || str.includes('\n')) {
    return `"${str.replace(/"/g, '""')}"`
  }
  return str
}

function buildCsv(columns: ExportColumn[], rows: Record<string, unknown>[]): string {
  const header = columns.map(c => escapeCsvCell(c.header)).join(',')
  const body = rows.map(row =>
    columns.map(c => escapeCsvCell(row[c.key])).join(',')
  )
  return [header, ...body].join('\n')
}

function downloadBlob(blob: Blob, name: string) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = name
  a.click()
  // Revoke after next tick so the click has time to fire
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

// ─── export handlers (dynamic import for xlsx/pdf) ───────────────────────────

async function exportCsv(
  columns: ExportColumn[],
  rows: Record<string, unknown>[],
  filename: string,
) {
  const csv = buildCsv(columns, rows)
  const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' })
  downloadBlob(blob, `${filename}.csv`)
}

async function exportXlsx(
  columns: ExportColumn[],
  rows: Record<string, unknown>[],
  filename: string,
) {
  // Dynamic import — xlsx is NOT in the main chunk
  const { utils, writeFile } = await import(
    /* webpackChunkName: "xlsx-export" */ 'xlsx'
  )
  const wsData = [
    columns.map(c => c.header),
    ...rows.map(row => columns.map(c => row[c.key] ?? '')),
  ]
  const ws = utils.aoa_to_sheet(wsData)
  const wb = utils.book_new()
  utils.book_append_sheet(wb, ws, 'Export')
  writeFile(wb, `${filename}.xlsx`)
}

async function exportPdf(
  columns: ExportColumn[],
  rows: Record<string, unknown>[],
  filename: string,
) {
  // Dynamic import — jsPDF + autoTable NOT in main chunk
  const { default: jsPDF } = await import(
    /* webpackChunkName: "jspdf-export" */ 'jspdf'
  )
  const { default: autoTable } = await import(
    /* webpackChunkName: "jspdf-autotable" */ 'jspdf-autotable'
  )
  const doc = new jsPDF({ orientation: 'landscape' })
  autoTable(doc, {
    head: [columns.map(c => c.header)],
    body: rows.map(row => columns.map(c => String(row[c.key] ?? ''))),
    styles: { fontSize: 9 },
  })
  doc.save(`${filename}.pdf`)
}

// ─── component ────────────────────────────────────────────────────────────────

const FORMAT_LABELS: Record<ExportFormat, string> = {
  csv: 'CSV',
  xlsx: 'Excel',
  pdf: 'PDF',
}

/**
 * BulkExport — multi-format table export trigger (CSV / XLSX / PDF).
 *
 * Bundle contract: CSV uses an inline zero-dependency serializer (<1 kB).
 * XLSX (xlsx library, ~450 kB) and PDF (jsPDF + autoTable, ~350 kB) are
 * dynamically imported on demand — main chunk delta stays under 30 kB.
 *
 * L4 supplies already-filtered/sorted rows. This component only renders
 * the export button group and orchestrates the async export.
 *
 * L4 usage:
 *   <BulkExport
 *     rows={filteredRows}
 *     columns={EXPORT_COLUMNS}
 *     filename="users-2026-05"
 *     onExportStart={format => track('table.bulk_export.invoked_count', { format })}
 *   />
 */
export default function BulkExport({
  rows,
  columns,
  filename = 'export',
  formats = ['csv', 'xlsx', 'pdf'],
  onExportStart,
}: BulkExportProps) {
  const [busy, setBusy] = React.useState<ExportFormat | null>(null)
  const [error, setError] = React.useState<string | null>(null)

  async function handleExport(format: ExportFormat) {
    if (busy) return
    setBusy(format)
    setError(null)
    onExportStart?.(format)
    try {
      if (format === 'csv') await exportCsv(columns, rows, filename)
      else if (format === 'xlsx') await exportXlsx(columns, rows, filename)
      else await exportPdf(columns, rows, filename)
    } catch (err) {
      setError(`Export failed: ${err instanceof Error ? err.message : 'unknown error'}`)
    } finally {
      setBusy(null)
    }
  }

  return (
    <div className="flex items-center gap-2">
      {formats.map(format => (
        <button
          key={format}
          type="button"
          disabled={busy !== null}
          aria-busy={busy === format}
          aria-label={`Export as ${FORMAT_LABELS[format]}`}
          onClick={() => handleExport(format)}
          className={[
            'inline-flex items-center gap-1.5 rounded-md border px-3 py-1.5 text-sm font-medium transition-colors',
            'focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring',
            'disabled:opacity-50 disabled:cursor-not-allowed',
            busy === format
              ? 'border-primary bg-primary/10 text-primary'
              : 'border-input bg-background hover:bg-accent',
          ].join(' ')}
        >
          {busy === format ? (
            <svg
              aria-hidden="true"
              width="14"
              height="14"
              viewBox="0 0 14 14"
              fill="none"
              stroke="currentColor"
              strokeWidth="1.5"
              className="animate-spin"
            >
              <circle cx="7" cy="7" r="5" strokeOpacity="0.3" />
              <path d="M7 2a5 5 0 0 1 5 5" />
            </svg>
          ) : (
            <svg
              aria-hidden="true"
              width="14"
              height="14"
              viewBox="0 0 14 14"
              fill="none"
              stroke="currentColor"
              strokeWidth="1.5"
            >
              <path d="M7 1v8M4 6l3 3 3-3M2 11h10" />
            </svg>
          )}
          {FORMAT_LABELS[format]}
        </button>
      ))}

      {error && (
        <p role="alert" className="text-xs text-destructive">
          {error}
        </p>
      )}

      {rows.length > 0 && (
        <span className="text-xs text-muted-foreground" aria-live="polite">
          {rows.length.toLocaleString()} rows
        </span>
      )}
    </div>
  )
}
