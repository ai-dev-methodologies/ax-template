/*
---
template_id: L4/file-storage/app/(file-storage)/files/[id]/file-detail-view
layer: L4
domain: file-storage
domain_mode: full_trio
evidence:
  - source_type: internal
    rationale: "Pure presentational extraction from (file-storage)/files/[id]/page.tsx (BACKLOG
      P2-42 render-testability pass-1 closure — same class as (crud)/items/[id]/item-detail-view.
      tsx): the page's data-fetch/mutation orchestration (useQuery/useMutation/useRouter/useToast)
      is a hard dependency-resolution boundary for a vitest that imports this file directly from
      outside frontend/ — the @tanstack/react-query bare specifier does not resolve for a module
      living in templates/L4/... (see frontend/tests/audit-log-redaction-render.vitest.tsx's own
      note on the same class of gap). templates/L2/blocks/toast-queue has the SAME class of gap
      one level deeper (it statically imports the 'sonner' npm package) — this view never imports
      it; the toast side-effect stays entirely inside the page's mutation callbacks, invisible to
      this render surface. templates/L2/blocks/{error-boundary,confirm-dialog} have zero
      external-npm deps and are safe to import/render directly. NOTE: the pre-existing page.tsx
      never passed ConfirmDialog its required `open` prop (so it always evaluated `!open` -> true
      and returned null — the delete confirmation could never actually appear) and passed a
      `confirmVariant` prop the component does not accept (it takes `destructive: boolean`); both
      fixed here while extracting, since a vitest asserting the confirm flow would otherwise fail
      against the real (latently broken) component."
---
*/
import * as React from 'react'
import ErrorBoundary from 'templates/L2/blocks/error-boundary'
import ConfirmDialog from 'templates/L2/blocks/confirm-dialog'

// ─── types ──────────────────────────────────────────────────────────────────

export type FileStatus = 'PENDING' | 'READY' | 'QUARANTINED' | 'DELETED'

export interface StoredFile {
  id: string
  name: string
  contentType: string
  sizeBytes: number
  status: FileStatus
  description: string | null
  uploadedAt: string
  expiresAt: string | null
  downloadUrl: string | null
}

export interface FileDetailViewProps {
  file: StoredFile | undefined
  isLoading: boolean
  isError: boolean
  dataUpdatedAt: number
  onBack: () => void
  onDelete: () => void
  deletePending: boolean
}

// ─── helpers ──────────────────────────────────────────────────────────────────

/** FILE-FE-RENDER-004: never show raw byte counts */
function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

// ─── status badge (FILE-FE-A11Y-002: text + color, not color alone) ──────────

const STATUS_CONFIG: Record<FileStatus, { label: string; className: string }> = {
  PENDING:     { label: 'Scanning',    className: 'bg-amber-100 text-amber-800' },
  READY:       { label: 'Ready',       className: 'bg-green-100 text-green-800' },
  QUARANTINED: { label: 'Quarantined', className: 'bg-red-100 text-red-800' },
  DELETED:     { label: 'Deleted',     className: 'bg-gray-100 text-gray-600' },
}

function StatusBadge({ status }: { status: FileStatus }) {
  const { label, className } = STATUS_CONFIG[status]
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-sm font-medium ${className}`}>
      {label}
    </span>
  )
}

// ─── component ──────────────────────────────────────────────────────────────

/**
 * FileDetailView — pure presentational render of a stored file's metadata + actions.
 *
 * Deliberately has ZERO data-fetching/mutation/toast dependencies (no useQuery/useMutation/
 * useToast) — the caller (`(file-storage)/files/[id]/page.tsx`) owns the polling query, the
 * delete mutation, and the toast side-effects, passing the resolved `file` + a `showDelete`
 * confirm-dialog toggle boundary in via props. This keeps the component a plain props -> JSX
 * function, which is what makes it renderable in a unit test.
 */
export default function FileDetailView({
  file,
  isLoading,
  isError,
  dataUpdatedAt,
  onBack,
  onDelete,
  deletePending,
}: FileDetailViewProps) {
  const [showDelete, setShowDelete] = React.useState(false)

  if (isLoading) {
    return (
      <div className="flex h-48 items-center justify-center">
        <span className="text-[--color-text-muted]">Loading...</span>
      </div>
    )
  }

  if (isError || !file) {
    return (
      <div role="alert" className="rounded-[--radius-md] border border-[--color-error] bg-[--color-error-subtle] p-[--space-4]">
        <p className="text-[length:--text-sm] text-[--color-error]">File not found.</p>
        <button onClick={onBack} className="mt-2 text-[length:--text-sm] text-[--color-accent] hover:underline">
          Back to files
        </button>
      </div>
    )
  }

  return (
    <ErrorBoundary>
      <div className="mx-auto max-w-2xl space-y-[--space-6]">
        {/* Header */}
        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-[length:--text-xl] font-[number:--weight-semibold] break-all">
              {file.name}
            </h1>
            <div className="mt-[--space-2] flex items-center gap-[--space-2]">
              <StatusBadge status={file.status} />
              {file.status === 'PENDING' && (
                <span
                  className="text-[length:--text-xs] text-[--color-text-muted]"
                  aria-live="polite"
                >
                  Scanning for viruses...
                </span>
              )}
              <span
                className="text-[length:--text-xs] text-[--color-text-muted]"
                aria-live="polite"
              >
                {dataUpdatedAt
                  ? `Updated ${new Date(dataUpdatedAt).toLocaleTimeString()}`
                  : ''}
              </span>
            </div>
          </div>
          <button
            onClick={onBack}
            className="text-[length:--text-sm] text-[--color-text-muted] hover:text-[--color-text]"
          >
            ← Back
          </button>
        </div>

        {/* Quarantine warning */}
        {file.status === 'QUARANTINED' && (
          <div
            role="alert"
            aria-live="polite"
            className="rounded-[--radius-md] border border-[--color-error] bg-[--color-error-subtle] p-[--space-4]"
          >
            <p className="text-[length:--text-sm] font-[number:--weight-medium] text-[--color-error]">
              This file has been quarantined
            </p>
            <p className="mt-1 text-[length:--text-xs] text-[--color-error]">
              Our virus scanner detected potential malware. The file cannot be downloaded.
            </p>
          </div>
        )}

        {/* Metadata */}
        <dl className="grid grid-cols-2 gap-[--space-3] rounded-[--radius-lg] border border-[--color-border] p-[--space-4]">
          <div>
            <dt className="text-[length:--text-xs] text-[--color-text-muted]">Type</dt>
            <dd className="text-[length:--text-sm]">{file.contentType}</dd>
          </div>
          <div>
            <dt className="text-[length:--text-xs] text-[--color-text-muted]">Size</dt>
            <dd className="text-[length:--text-sm]">{formatBytes(file.sizeBytes)}</dd>
          </div>
          <div>
            <dt className="text-[length:--text-xs] text-[--color-text-muted]">Uploaded</dt>
            <dd className="text-[length:--text-sm]">
              {new Date(file.uploadedAt).toLocaleString()}
            </dd>
          </div>
          {file.expiresAt && (
            <div>
              <dt className="text-[length:--text-xs] text-[--color-text-muted]">Expires</dt>
              <dd className="text-[length:--text-sm]">
                {new Date(file.expiresAt).toLocaleDateString()}
              </dd>
            </div>
          )}
          {file.description && (
            <div className="col-span-2">
              <dt className="text-[length:--text-xs] text-[--color-text-muted]">Description</dt>
              <dd className="text-[length:--text-sm]">{file.description}</dd>
            </div>
          )}
        </dl>

        {/* Actions */}
        <div className="flex gap-[--space-3]">
          {file.status === 'READY' && file.downloadUrl ? (
            <a
              href={file.downloadUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="flex-1 rounded-[--radius-md] bg-[--color-accent] px-[--space-4] py-[--space-2] text-center text-[length:--text-sm] font-[number:--weight-medium] text-white"
            >
              Download
            </a>
          ) : file.status === 'PENDING' ? (
            <button
              disabled
              aria-disabled="true"
              className="flex-1 cursor-not-allowed rounded-[--radius-md] bg-[--color-surface-subtle] px-[--space-4] py-[--space-2] text-[length:--text-sm] text-[--color-text-muted]"
            >
              Scan in progress...
            </button>
          ) : null}

          <button
            onClick={() => setShowDelete(true)}
            aria-busy={deletePending || undefined}
            disabled={deletePending}
            className="rounded-[--radius-md] border border-[--color-error] px-[--space-4] py-[--space-2] text-[length:--text-sm] font-[number:--weight-medium] text-[--color-error] hover:bg-[--color-error-subtle] aria-busy:opacity-60 disabled:opacity-50"
          >
            Delete
          </button>
        </div>

        {/* Confirm delete */}
        <ConfirmDialog
          open={showDelete}
          title="Delete file?"
          description={`"${file.name}" will be permanently deleted.`}
          confirmLabel="Delete"
          destructive
          onConfirm={onDelete}
          onCancel={() => setShowDelete(false)}
          isLoading={deletePending}
        />
      </div>
    </ErrorBoundary>
  )
}
