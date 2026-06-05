/**
 * File-storage client — /api/files. Backend: FileStorageController.
 * Auth: any authenticated user; files are owner-scoped by the JWT.
 *
 * Domain client (lib/api), NOT a UI primitive. It REUSES the shared @ax/core
 * session everywhere:
 *   - metadata GET + DELETE go through the shared `apiFetch` (JSON helper);
 *   - the multipart upload can't use `apiFetch` (it always JSON-stringifies the
 *     body and pins Content-Type: application/json), so it issues a raw `fetch`
 *     for the file part — but still reads the Bearer from the shared
 *     `useAuthStore.getState()` and parses the RFC 9457 problem body the same way.
 *     It does NOT re-implement auth or the token lifecycle, only the transport.
 *   - the download endpoint is authenticated (a bare <img src> can't carry the
 *     JWT), so a blob download goes through the shared `apiDownload` which adds
 *     the Bearer and returns a Blob; the gallery renders it via createObjectURL.
 *
 * Curl-verified shapes (2026-06-05, demo@ax.dev ADMIN):
 *   POST   /api/files (multipart "file")          -> 201 StoredFile (status READY)
 *   POST   /api/files (application/octet-stream)  -> 415 Unsupported Media Type
 *   GET    /api/files/{id}                         -> 200 StoredFile
 *   GET    /api/files/{id}/download                -> 200 binary (Content-Disposition
 *                                                     attachment; X-Content-Type-Options nosniff)
 *   DELETE /api/files/{id}                         -> 204
 *
 * Upload constraints (blueprints/file-storage-manifest.yaml via FileStorageProperties):
 *   - max file size: 100 MB
 *   - per-user quota: 1 GB (413 + quota-exceeded ProblemDetail when exceeded)
 *   - allowed MIME allowlist incl. image/jpeg, image/png, image/gif, image/webp,
 *     image/svg+xml, application/pdf, text/plain, text/csv, application/zip, …
 *
 * NOTE — there is intentionally NO list endpoint on this domain (each file is
 * strictly owner-scoped + id-addressed). The studio enumerates a creator's media
 * through the favorites index (entityType "file"): every upload is auto-favorited,
 * giving a listable, per-user catalog of file ids the gallery resolves to metadata.
 */
import { apiFetch, apiDownload, useAuthStore, type ProblemDetail } from '@ax/core';

export type FileStatus = 'PENDING' | 'READY' | 'QUARANTINED' | 'DELETED';

export interface StoredFile {
  id: string;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  sha256: string;
  status: FileStatus;
  /** relative path on this server, e.g. /api/files/{id}/download */
  downloadUrl: string;
  uploadedAt: string;
  scannedAt: string | null;
}

/** Persona-/manifest-sourced upload constraints, surfaced to the upload UI. */
export const UPLOAD_LIMITS = {
  maxFileSizeBytes: 100 * 1024 * 1024,
  /** Mirror of FileStorageProperties.allowedMimeTypes (image-first for the studio). */
  allowedMimeTypes: [
    'image/jpeg',
    'image/png',
    'image/gif',
    'image/webp',
    'image/svg+xml',
    'application/pdf',
    'text/plain',
    'text/csv',
    'application/zip',
    'application/gzip',
  ] as const,
} as const;

/** The subset the studio gallery previews inline; also drives the file picker accept=. */
export const IMAGE_ACCEPT = 'image/jpeg,image/png,image/gif,image/webp,image/svg+xml';

function bearer(): Record<string, string> {
  const token = useAuthStore.getState().accessToken;
  return token ? { Authorization: `Bearer ${token}` } : {};
}

export const fileClient = {
  /** Upload one file (multipart). Returns the stored-file metadata (incl. status, downloadUrl). */
  upload: async (file: File): Promise<StoredFile> => {
    const form = new FormData();
    form.append('file', file);
    const res = await fetch('/api/files', {
      method: 'POST',
      headers: { ...bearer() },
      body: form,
    });
    if (!res.ok) {
      const problem = (await res.json().catch(() => null)) as ProblemDetail | null;
      const message =
        problem?.detail || problem?.title || `업로드에 실패했어요 (${res.status})`;
      throw Object.assign(new Error(message), { status: res.status });
    }
    return (await res.json()) as StoredFile;
  },

  /** Owner-scoped metadata for a single file. */
  get: (id: string): Promise<StoredFile> => apiFetch<StoredFile>(`/files/${encodeURIComponent(id)}`),

  /** Authed binary download — returns a Blob the gallery turns into an object URL. */
  download: (id: string): Promise<Blob> =>
    apiDownload(`/files/${encodeURIComponent(id)}/download`).then((r) => r.blob),

  /** Soft-delete a file (owner-only). */
  remove: (id: string): Promise<void> =>
    apiFetch<void>(`/files/${encodeURIComponent(id)}`, { method: 'DELETE' }),
};
