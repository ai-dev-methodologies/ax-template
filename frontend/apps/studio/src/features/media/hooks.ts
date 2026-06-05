import { useEffect, useState } from 'react';
import { useMutation, useQueries, useQuery, useQueryClient } from '@tanstack/react-query';
import { fileClient, type StoredFile } from '@/lib/api/fileClient';
import { favoriteClient } from '@/lib/api/favoriteClient';
import { activityClient } from '@/lib/api/activityClient';

/** The entityType the studio uses to index files in the favorites + tag domains. */
export const FILE_ENTITY_TYPE = 'file';

export const mediaKeys = {
  all: ['media'] as const,
  /** the favorites-derived index of this creator's file ids */
  index: () => [...mediaKeys.all, 'index'] as const,
  detail: (id: string) => [...mediaKeys.all, 'detail', id] as const,
  blob: (id: string) => [...mediaKeys.all, 'blob', id] as const,
};

/**
 * The creator's media index. file-storage has no list endpoint, so we enumerate
 * the per-user catalog through the favorites index (entityType "file"): every
 * upload is auto-favorited (see {@link useUploadMedia}). Returns the file ids
 * newest-first (the favorites endpoint returns rows; we sort by favoritedAt desc).
 */
export function useMediaIndex(enabled: boolean) {
  return useQuery<string[]>({
    queryKey: mediaKeys.index(),
    enabled,
    queryFn: async () => {
      const result = await favoriteClient.list(FILE_ENTITY_TYPE);
      return [...result.items]
        .sort((a, b) => b.favoritedAt.localeCompare(a.favoritedAt))
        .map((f) => f.entityId);
    },
  });
}

/** Resolve a single file's metadata. */
export function useMediaDetail(id: string, enabled: boolean) {
  return useQuery<StoredFile>({
    queryKey: mediaKeys.detail(id),
    enabled,
    queryFn: () => fileClient.get(id),
  });
}

/**
 * Resolve every file id in the index to its metadata in parallel. Returns the
 * loaded {@link StoredFile} list (skipping ids that errored, e.g. a favorited
 * file that was later hard-deleted) plus aggregate loading/error flags.
 */
export function useMediaList(ids: string[], enabled: boolean) {
  const results = useQueries({
    queries: ids.map((id) => ({
      queryKey: mediaKeys.detail(id),
      queryFn: () => fileClient.get(id),
      enabled,
    })),
  });

  const files = results
    .map((r) => r.data)
    .filter((f): f is StoredFile => Boolean(f));

  return {
    files,
    isLoading: enabled && results.some((r) => r.isLoading),
    isError: results.length > 0 && results.every((r) => r.isError),
  };
}

/**
 * Authed object URL for an image file. A bare <img src="/api/files/{id}/download">
 * cannot carry the JWT (the browser won't attach the Bearer), so we download the
 * blob through the shared apiDownload (which adds the token) and expose it as an
 * object URL. The URL is revoked on unmount / id change to avoid a leak.
 */
export function useMediaObjectUrl(id: string, enabled: boolean): {
  url: string | null;
  isLoading: boolean;
  isError: boolean;
} {
  const query = useQuery<Blob>({
    queryKey: mediaKeys.blob(id),
    enabled,
    queryFn: () => fileClient.download(id),
    staleTime: 10 * 60_000,
  });

  const [url, setUrl] = useState<string | null>(null);
  useEffect(() => {
    if (!query.data) {
      setUrl(null);
      return;
    }
    const objectUrl = URL.createObjectURL(query.data);
    setUrl(objectUrl);
    return () => {
      URL.revokeObjectURL(objectUrl);
    };
  }, [query.data]);

  return { url, isLoading: query.isLoading, isError: query.isError };
}

export interface UploadResult {
  file: StoredFile;
}

/**
 * Upload a file and wire it into the studio: store the binary, auto-favorite it
 * (so it appears in the gallery index), and publish an "uploaded" activity. The
 * favorite + activity are best-effort side effects — a failure there does not
 * fail the upload (the file is already stored), but the index/feed are
 * invalidated either way so the new media shows up.
 */
export function useUploadMedia() {
  const qc = useQueryClient();
  return useMutation<UploadResult, Error, File>({
    mutationFn: async (file) => {
      const stored = await fileClient.upload(file);
      await Promise.allSettled([
        favoriteClient.add({
          entityType: FILE_ENTITY_TYPE,
          entityId: stored.id,
          note: stored.fileName,
        }),
        activityClient.publish({
          verb: 'uploaded',
          objectType: FILE_ENTITY_TYPE,
          objectId: stored.id,
          metadata: { fileName: stored.fileName, contentType: stored.contentType },
          idempotencyKey: `upload-${stored.id}`,
        }),
      ]);
      return { file: stored };
    },
    onSuccess: ({ file }) => {
      qc.setQueryData(mediaKeys.detail(file.id), file);
      void qc.invalidateQueries({ queryKey: mediaKeys.index() });
      void qc.invalidateQueries({ queryKey: ['activity'] });
      void qc.invalidateQueries({ queryKey: ['favorites'] });
    },
  });
}

/** Delete a media file (and drop its favorite index row). */
export function useDeleteMedia() {
  const qc = useQueryClient();
  return useMutation<void, Error, string>({
    mutationFn: async (id) => {
      // The file delete and the favorite-index cleanup are independent — fire
      // both in parallel. The favorite removal is best-effort (allSettled), but
      // the file delete must succeed, so re-throw its rejection if it failed.
      const [fileResult] = await Promise.allSettled([
        fileClient.remove(id),
        favoriteClient.remove(FILE_ENTITY_TYPE, id),
      ]);
      if (fileResult.status === 'rejected') throw fileResult.reason;
    },
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: mediaKeys.index() });
    },
  });
}
