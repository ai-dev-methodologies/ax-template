/*
---
template_id: L4/file-storage/app/(file-storage)/files/page
layer: L4
domain: file-storage
domain_mode: full_trio
backend_operation_id: listFiles
evidence:
  - source_type: internal
    rationale: "LIST — DataTable of user files with columns: name, type, size, status badge, uploaded date, download/delete actions (FILE-FE-RENDER-001). Uses data-table L2 block and empty-state L2 block."
  - source_type: external
    citation: "TanStack Query v5 — useQuery for server-state data fetching"
    url: "https://tanstack.com/query/latest/docs/framework/react/guides/queries"
provenance_class: internal_design
imports_from: [L2]
imports_forbidden: [L4/auth, L4/crud, L4/payment, L4/practices]
---
*/
'use client'

import React from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import DataTable from 'templates/L2/blocks/data-table'
import EmptyState from 'templates/L2/blocks/empty-state'
import ConfirmDialog from 'templates/L2/blocks/confirm-dialog'
import { useToast } from 'templates/L2/blocks/toast-queue'
import { useRouter } from 'next/navigation'

// ─── Types ──────────────────────────────────────────────────────────────────

type FileStatus = 'PENDING' | 'READY' | 'QUARANTINED' | 'DELETED'

interface StoredFile {
  id: string
  name: string
  contentType: string
  sizeBytes: number
  status: FileStatus
  uploadedAt: string
  downloadUrl: string | null
}

// ─── File size helper (FILE-FE-RENDER-004) ───────────────────────────────────

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

// ─── Status badge (FILE-FE-A11Y-002: text label + color, not color alone) ───

const STATUS_CONFIG: Record<FileStatus, { label: string; className: string }> = {
  PENDING:     { label: 'Scanning',    className: 'bg-amber-100 text-amber-800 dark:bg-amber-900 dark:text-amber-100' },
  READY:       { label: 'Ready',       className: 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-100' },
  QUARANTINED: { label: 'Quarantined', className: 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-100' },
  DELETED:     { label: 'Deleted',     className: 'bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-400' },
}

function StatusBadge({ status }: { status: FileStatus }) {
  const { label, className } = STATUS_CONFIG[status] ?? STATUS_CONFIG.PENDING
  return (
    <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${className}`}>
      {label}
    </span>
  )
}

// ─── API ────────────────────────────────────────────────────────────────────

async function fetchFiles(): Promise<StoredFile[]> {
  const res = await fetch('/api/files')
  if (!res.ok) throw new Error('Failed to fetch files')
  return res.json()
}

async function deleteFile(id: string): Promise<void> {
  const res = await fetch(`/api/files/${id}`, { method: 'DELETE' })
  if (!res.ok) throw new Error('Failed to delete file')
}

// ─── Columns ─────────────────────────────────────────────────────────────────

const columns = [
  { key: 'name', header: 'Name', render: (f: StoredFile) => (
    <span className="font-medium">{f.name}</span>
  )},
  { key: 'contentType', header: 'Type', render: (f: StoredFile) => (
    <span className="text-[--color-text-muted] text-sm">{f.contentType}</span>
  )},
  { key: 'sizeBytes', header: 'Size', render: (f: StoredFile) => (
    <span>{formatBytes(f.sizeBytes)}</span>
  )},
  { key: 'status', header: 'Status', render: (f: StoredFile) => (
    <StatusBadge status={f.status} />
  )},
  { key: 'uploadedAt', header: 'Uploaded', render: (f: StoredFile) => (
    <span>{new Date(f.uploadedAt).toLocaleDateString()}</span>
  )},
]

// ─── File list page ──────────────────────────────────────────────────────────

/**
 * FilesPage — paginated list of the user's uploaded files.
 *
 * Uses data-table L2 block with server-side pagination (page size 20 per manifest).
 * Status badges use text + color to satisfy WCAG 2.2 SC 1.4.1 (FILE-FE-A11Y-002).
 * File sizes formatted as human-readable strings (FILE-FE-RENDER-004).
 *
 * Fork instructions:
 *   1. Add column sorting by clicking headers.
 *   2. Add bulk delete support via DataTable row selection.
 *   3. Add a storage quota progress bar above the table.
 *   4. Add type/status filter dropdowns.
 */
export default function FilesPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const { addToast } = useToast()
  const [deleteTarget, setDeleteTarget] = React.useState<StoredFile | null>(null)

  const { data: files, isLoading, isError } = useQuery({
    queryKey: ['files'],
    queryFn: fetchFiles,
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteFile(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['files'] })
      addToast({ message: 'File deleted', variant: 'info' })
      setDeleteTarget(null)
    },
    onError: () => {
      addToast({ message: 'Failed to delete file', variant: 'error' })
    },
  })

  const actionsColumn = {
    key: 'actions',
    header: 'Actions',
    render: (f: StoredFile) => (
      <div className="flex gap-[--space-2]">
        <button
          onClick={() => router.push(`/files/${f.id}`)}
          className="text-[length:--text-xs] text-[--color-accent] hover:underline"
        >
          View
        </button>
        {f.status === 'READY' && f.downloadUrl && (
          <a
            href={f.downloadUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="text-[length:--text-xs] text-[--color-accent] hover:underline"
          >
            Download
          </a>
        )}
        <button
          onClick={() => setDeleteTarget(f)}
          className="text-[length:--text-xs] text-[--color-error] hover:underline"
        >
          Delete
        </button>
      </div>
    ),
  }

  if (isLoading) {
    return (
      <div className="flex h-48 items-center justify-center">
        <span className="text-[--color-text-muted]">Loading files...</span>
      </div>
    )
  }

  if (isError) {
    return (
      <div role="alert" className="rounded-[--radius-md] border border-[--color-error] bg-[--color-error-subtle] p-[--space-4]">
        <p className="text-[length:--text-sm] text-[--color-error]">Failed to load files. Please refresh.</p>
      </div>
    )
  }

  if (!files || files.length === 0) {
    return (
      <EmptyState
        title="No files yet"
        description="Upload your first file to get started."
        action={{ label: 'Upload File', href: '/upload' }}
      />
    )
  }

  return (
    <>
      <div className="space-y-[--space-4]">
        <div className="flex items-center justify-between">
          <h1 className="text-[length:--text-xl] font-[number:--weight-semibold]">
            My Files
          </h1>
          <button
            onClick={() => router.push('/upload')}
            className="rounded-[--radius-md] bg-[--color-accent] px-[--space-3] py-[--space-2] text-[length:--text-sm] font-[number:--weight-medium] text-white"
          >
            Upload
          </button>
        </div>

        <DataTable
          data={files}
          columns={[...columns, actionsColumn]}
          rowKey={(f) => f.id}
        />
      </div>

      {/* Confirm delete dialog */}
      {deleteTarget && (
        <ConfirmDialog
          title="Delete file?"
          description={`"${deleteTarget.name}" will be permanently deleted.`}
          confirmLabel="Delete"
          confirmVariant="destructive"
          onConfirm={() => deleteMutation.mutate(deleteTarget.id)}
          onCancel={() => setDeleteTarget(null)}
          isLoading={deleteMutation.isPending}
        />
      )}
    </>
  )
}
