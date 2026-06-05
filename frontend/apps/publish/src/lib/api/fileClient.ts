/**
 * File client — POST /api/files (multipart). Backend: FileStorageController.
 * Auth: any authenticated user; files are owner-scoped by the JWT.
 *
 * The shared @ax/core `apiFetch` is JSON-only (it always stringifies the body),
 * so a multipart upload cannot go through it. This client therefore issues a raw
 * `fetch` for the file part BUT still REUSES the shared session: the Bearer token
 * is read from the shared @ax/core authStore (`useAuthStore.getState()`), and the
 * RFC 9457 problem body is parsed the same way. It does NOT re-implement auth or
 * the token lifecycle — only the multipart transport the JSON helper can't carry.
 *
 * The studio uses this to upload an article cover image. The response
 * `downloadUrl` is a relative path on this server (/api/files/{id}/download),
 * which the read view renders through the same /api proxy.
 *
 * Curl-verified shapes (2026-06-05, demo@ax.dev):
 *   POST /api/files (image/png) -> 201 { id, fileName, contentType, sizeBytes,
 *                                        sha256, status:"READY", downloadUrl, ... }
 */
import { useAuthStore, type ProblemDetail } from '@ax/core';

export interface StoredFile {
  id: string;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  sha256: string;
  status: 'PENDING' | 'READY' | 'QUARANTINED' | 'DELETED';
  /** relative path on this server, e.g. /api/files/{id}/download */
  downloadUrl: string;
  uploadedAt: string;
  scannedAt: string | null;
}

function bearer(): Record<string, string> {
  const token = useAuthStore.getState().accessToken;
  return token ? { Authorization: `Bearer ${token}` } : {};
}

export const fileClient = {
  /** Upload a cover image. Returns the stored-file metadata (incl. downloadUrl). */
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
        problem?.detail || problem?.title || `업로드에 실패했습니다 (${res.status})`;
      throw new Error(message);
    }
    return (await res.json()) as StoredFile;
  },
};
