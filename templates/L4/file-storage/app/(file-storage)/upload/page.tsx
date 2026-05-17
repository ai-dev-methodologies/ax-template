/*
---
template_id: L4/file-storage/app/(file-storage)/upload/page
layer: L4
domain: file-storage
domain_mode: full_trio
backend_operation_id: uploadFile
evidence:
  - source_type: internal
    rationale: "UPLOAD — uses L1 FileDropzone with accepted MIME types + size limit from manifest. Progress feedback via loading-boundary, errors via error-boundary, success notification via toast-queue (FILE-FE-RENDER-002)."
  - source_type: external
    citation: "react-dropzone v14 — useDropzone hook and FileDropzone composition"
    url: "https://react-dropzone.js.org/"
provenance_class: internal_design
imports_from: [L1, L2]
imports_forbidden: [L4/auth, L4/crud, L4/payment, L4/practices]
---
*/
'use client'

import React, { useState } from 'react'
import { useRouter } from 'next/navigation'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { FileDropzone } from 'templates/L1/components/file-dropzone'
import ErrorBoundary from 'templates/L2/blocks/error-boundary'
import { useToast } from 'templates/L2/blocks/toast-queue'
import type { FileRejection } from 'react-dropzone'

// ─── Types ──────────────────────────────────────────────────────────────────

interface UploadErrorDetail {
  type: string
  detail: string
}

// ─── API ────────────────────────────────────────────────────────────────────

async function uploadFile(file: File, description?: string): Promise<void> {
  const formData = new FormData()
  formData.append('file', file)
  if (description) formData.append('description', description)

  const res = await fetch('/api/files', { method: 'POST', body: formData })
  if (!res.ok) {
    const body: UploadErrorDetail = await res.json().catch(() => ({ type: 'UNKNOWN', detail: '' }))
    throw { status: res.status, type: body.type, detail: body.detail }
  }
}

// ─── Error message map (FILE-FE-ERROR-001) ──────────────────────────────────

function getUploadErrorMessage(err: unknown): string {
  if (err && typeof err === 'object' && 'type' in err) {
    const e = err as { status: number; type: string }
    if (e.type === 'https://ax-template.example/problems/quota-exceeded') {
      return 'Storage quota exceeded. Delete some files to free up space.'
    }
    if (e.type === 'https://ax-template.example/problems/payload-too-large' || e.status === 413) {
      return 'File exceeds the 100 MB limit.'
    }
    if (e.status === 415) {
      return 'File type not supported. Allowed types: PDF, images, documents.'
    }
  }
  return 'Upload failed. Please try again.'
}

// ─── File size helpers ───────────────────────────────────────────────────────

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

// ─── Upload page ─────────────────────────────────────────────────────────────

/**
 * UploadPage — file upload page for the file-storage domain.
 *
 * Uses L1 FileDropzone with:
 *   - accept: MIME type map from blueprints/file-storage-ui-manifest.yaml#upload.accept_prop
 *   - maxSize: 100 MB (FILE-UPLOAD-002)
 *   - multiple: true (up to 10 files)
 *
 * On success: invalidates listFiles query + navigates to /files + shows toast.
 * On error: shows specific message mapped from backend ProblemDetail.type (FILE-FE-ERROR-001).
 *
 * Fork instructions:
 *   1. Add a description textarea if your use case needs file descriptions.
 *   2. Add tag input for file categorization.
 *   3. For large files: add an XHR-based upload with onUploadProgress to show byte progress.
 *   4. For S3 direct upload: replace the fetch call with a presigned POST flow.
 */
export default function UploadPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const { addToast } = useToast()

  const [files, setFiles] = useState<File[]>([])
  const [rejections, setRejections] = useState<FileRejection[]>([])
  const [uploadError, setUploadError] = useState<string | null>(null)

  const mutation = useMutation({
    mutationFn: async (filesToUpload: File[]) => {
      // Upload files sequentially to avoid server overload
      for (const file of filesToUpload) {
        await uploadFile(file)
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['files'] })
      addToast({ message: 'Files uploaded successfully', variant: 'success' })
      setFiles([])
      setUploadError(null)
      router.push('/files')
    },
    onError: (err) => {
      setUploadError(getUploadErrorMessage(err))
    },
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (files.length === 0) return
    setUploadError(null)
    mutation.mutate(files)
  }

  const handleReject = (newRejections: FileRejection[]) => {
    setRejections(newRejections)
  }

  return (
    <ErrorBoundary>
      <div className="mx-auto max-w-2xl space-y-[--space-6]">
        <div>
          <h1 className="text-[length:--text-xl] font-[number:--weight-semibold]">
            Upload Files
          </h1>
          <p className="mt-[--space-1] text-[length:--text-sm] text-[--color-text-muted]">
            PDF, 이미지, 문서 · 최대 100 MB
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-[--space-4]">
          {/* L1 FileDropzone (SP14) — DO NOT modify the L1 component */}
          <FileDropzone
            accept={{
              'application/pdf': ['.pdf'],
              'application/msword': ['.doc'],
              'application/vnd.openxmlformats-officedocument.wordprocessingml.document': ['.docx'],
              'application/vnd.ms-excel': ['.xls'],
              'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': ['.xlsx'],
              'text/plain': ['.txt'],
              'text/csv': ['.csv'],
              'image/jpeg': ['.jpg', '.jpeg'],
              'image/png': ['.png'],
              'image/gif': ['.gif'],
              'image/webp': ['.webp'],
              'application/zip': ['.zip'],
            }}
            maxSize={100 * 1024 * 1024}
            maxFiles={10}
            multiple={true}
            value={files}
            onChange={setFiles}
            onReject={handleReject}
            disabled={mutation.isPending}
            hint="PDF, 이미지, 문서 · 최대 100 MB"
          />

          {/* Rejection feedback (FILE-FE-A11Y-003: aria-live region) */}
          {rejections.length > 0 && (
            <div role="alert" aria-live="polite" className="rounded-[--radius-md] border border-[--color-error] bg-[--color-error-subtle] p-[--space-3]">
              <p className="text-[length:--text-sm] font-[number:--weight-medium] text-[--color-error]">
                일부 파일을 업로드할 수 없습니다:
              </p>
              <ul className="mt-[--space-1] list-inside list-disc text-[length:--text-xs] text-[--color-error]">
                {rejections.map((r) => (
                  <li key={r.file.name}>
                    {r.file.name} ({formatBytes(r.file.size)}) — {r.errors[0]?.message}
                  </li>
                ))}
              </ul>
            </div>
          )}

          {/* Upload error feedback (FILE-FE-ERROR-001: aria-live, FILE-FE-A11Y-003) */}
          {uploadError && (
            <div role="alert" aria-live="polite" className="rounded-[--radius-md] border border-[--color-error] bg-[--color-error-subtle] p-[--space-3]">
              <p className="text-[length:--text-sm] text-[--color-error]">{uploadError}</p>
            </div>
          )}

          {/* Upload button with loading state */}
          <button
            type="submit"
            disabled={files.length === 0 || mutation.isPending}
            className="w-full rounded-[--radius-md] bg-[--color-accent] px-[--space-4] py-[--space-2] text-[length:--text-sm] font-[number:--weight-medium] text-white disabled:cursor-not-allowed disabled:opacity-50"
            aria-disabled={files.length === 0 || mutation.isPending}
          >
            {mutation.isPending
              ? `업로드 중... (${files.length}개 파일)`
              : `${files.length}개 파일 업로드`}
          </button>
        </form>
      </div>
    </ErrorBoundary>
  )
}
