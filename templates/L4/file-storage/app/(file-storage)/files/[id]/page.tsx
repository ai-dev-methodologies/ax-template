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
import { useToast } from 'templates/L2/blocks/toast-queue'
import FileDetailView, { type StoredFile } from './file-detail-view'

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

  // R82 — mutation-in-flight-uses-aria-busy: deletePending is threaded through as a prop
  // so FileDetailView (the co-located pure presentational view — P2-42) can render
  // aria-busy on the Delete button itself; the attribute lives there, not in this file,
  // because the button markup moved with the render layer.
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

  return (
    <FileDetailView
      file={file}
      isLoading={isLoading}
      isError={isError}
      dataUpdatedAt={dataUpdatedAt}
      onBack={() => router.push('/files')}
      onDelete={() => deleteMutation.mutate()}
      deletePending={deleteMutation.isPending}
    />
  )
}
