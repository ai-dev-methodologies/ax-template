/*
---
template_id: L4/file-storage/app/(file-storage)/files/[id]/page
layer: L4
domain: file-storage
domain_mode: full_trio
backend_operation_id: getFile
evidence:
  - source_type: internal
    rationale: "DETAIL — file metadata display with download button. Polls getFile every 3s while status=PENDING to detect scan completion (FILE-FE-ERROR-002). Status badges use text+color (FILE-FE-A11Y-002). Download calls downloadFile op."
  - source_type: external
    citation: "TanStack Query v5 — refetchInterval for polling"
    url: "https://tanstack.com/query/latest/docs/framework/react/guides/query-options#refetchinterval"
provenance_class: internal_design
imports_from: [L2]
imports_forbidden: [L4/auth, L4/crud, L4/payment, L4/practices]
---
*/
'use client'

import React from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import ErrorBoundary from 'templates/L2/blocks/error-boundary'
import ConfirmDialog from 'templates/L2/blocks/confirm-dialog'
import { useToast } from 'templates/L2/blocks/toast-queue'

// ─── Types ──────────────────────────────────────────────────────────────────

type FileStatus = 'PENDING' | 'READY' | 'QUARANTINED' | 'DELETED'

interface StoredFile {
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

// ─── Helpers ─────────────────────────────────────────────────────────────────

/** FILE-FE-RENDER-004: never show raw byte counts */
function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

// ─── Status badge (FILE-FE-A11Y-002: text + color, not color alone) ──────────

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

// ─── API ────────────────────────────────────────────────────────────────────

async function fetchFile(id: string): Promise<StoredFile> {
  const res = await fetch(`/api/files/${id}`)
  if (!res.ok) throw new Error(`Failed to fetch file: ${res.status}`)
  return res.json()
}

async function deleteFile(id: string): Promise<void> {
  const res = await fetch(`/api/files/${id}`, { method: 'DELETE' })
  if (!res.ok) throw new Error('Failed to delete file')
}

// ─── File detail page ────────────────────────────────────────────────────────

interface FileDetailPageProps {
  params: { id: string }
}

/**
 * FileDetailPage — shows file metadata and provides download / delete actions.
 *
 * Polling (FILE-FE-ERROR-002):
 *   - While status=PENDING: refetchInterval=3000ms, Download button shows 'Scan in progress...'
 *   - Once READY: polling stops, download URL is available
 *   - If QUARANTINED: show quarantine error state
 *
 * Fork instructions:
 *   1. Add a file preview panel for images (thumbnail via presigned URL).
 *   2. Add version history if your backend supports multiple versions.
 *   3. Add share link generation (expiring share token).
 */
export default function FileDetailPage({ params }: FileDetailPageProps) {
  const { id } = params
  const router = useRouter()
  const queryClient = useQueryClient()
  const { addToast } = useToast()
  const [showDelete, setShowDelete] = React.useState(false)

  // R82 — dataUpdatedAt destructured and surfaced beside the
  // status badge so operators can confirm the scan-poll cadence.
  const { data: file, isLoading, isError, dataUpdatedAt } = useQuery({
    queryKey: ['files', id],
    queryFn: () => fetchFile(id),
    // FILE-FE-ERROR-002: poll every 3s while status is PENDING
    refetchInterval: (query) => {
      const status = query.state.data?.status
      return status === 'PENDING' ? 3000 : false
    },
    // Don't poll when tab is hidden (FILE-FE-ERROR-002)
    refetchIntervalInBackground: false,
  })

  const deleteMutation = useMutation({
    mutationFn: () => deleteFile(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['files'] })
      addToast({ message: 'File deleted', variant: 'info' })
      router.push('/files')
    },
    onError: () => {
      addToast({ message: 'Failed to delete file', variant: 'error' })
    },
  })

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
        <button onClick={() => router.push('/files')} className="mt-2 text-[length:--text-sm] text-[--color-accent] hover:underline">
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
              {/* R82 — visible polling cadence so the operator can
                  silently confirm freshness during a scan. */}
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
            onClick={() => router.push('/files')}
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
          {/* Download button — disabled while PENDING (FILE-FE-RENDER-003) */}
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
            /* R82 — aria-busy reflects the delete-mutation lifecycle so
               screen readers track the in-flight state of this
               background-polled page mutation (WCAG SC 4.1.3). */
            aria-busy={deleteMutation.isPending || undefined}
            disabled={deleteMutation.isPending}
            className="rounded-[--radius-md] border border-[--color-error] px-[--space-4] py-[--space-2] text-[length:--text-sm] font-[number:--weight-medium] text-[--color-error] hover:bg-[--color-error-subtle] aria-busy:opacity-60 disabled:opacity-50"
          >
            Delete
          </button>
        </div>

        {/* Confirm delete */}
        {showDelete && (
          <ConfirmDialog
            title="Delete file?"
            description={`"${file.name}" will be permanently deleted.`}
            confirmLabel="Delete"
            confirmVariant="destructive"
            onConfirm={() => deleteMutation.mutate()}
            onCancel={() => setShowDelete(false)}
            isLoading={deleteMutation.isPending}
          />
        )}
      </div>
    </ErrorBoundary>
  )
}
