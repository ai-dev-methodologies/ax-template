import { useMutation, useQueries, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  tagClient,
  type AttachTagInput,
  type CreateTagInput,
  type Tag,
  type TagAttachment,
  type TagListResult,
} from '@/lib/api/tagClient';
import { FILE_ENTITY_TYPE } from '@/features/media/hooks';

export const tagKeys = {
  all: ['tags'] as const,
  list: () => [...tagKeys.all, 'list'] as const,
  byEntity: (entityType: string, entityId: string) =>
    [...tagKeys.all, 'by-entity', entityType, entityId] as const,
};

/** All collections (tags). */
export function useTags(enabled: boolean) {
  return useQuery<TagListResult>({
    queryKey: tagKeys.list(),
    queryFn: () => tagClient.list(),
    enabled,
  });
}

/** Collections (tags) attached to one media file. */
export function useMediaTags(entityId: string, enabled: boolean) {
  return useQuery<TagListResult>({
    queryKey: tagKeys.byEntity(FILE_ENTITY_TYPE, entityId),
    queryFn: () => tagClient.byEntity(FILE_ENTITY_TYPE, entityId),
    enabled,
  });
}

/**
 * Resolve, for each file id, the collections (tags) attached to it. There is no
 * "files-for-a-tag" endpoint, so the Collections screen groups client-side:
 * fetch tags-by-entity for every media id in parallel, then invert the mapping.
 * Returns a map of fileId -> Tag[] plus aggregate loading flag.
 */
export function useTagsForFiles(ids: string[], enabled: boolean): {
  tagsByFile: Record<string, Tag[]>;
  isLoading: boolean;
} {
  const results = useQueries({
    queries: ids.map((id) => ({
      queryKey: tagKeys.byEntity(FILE_ENTITY_TYPE, id),
      queryFn: () => tagClient.byEntity(FILE_ENTITY_TYPE, id),
      enabled,
    })),
  });

  const tagsByFile: Record<string, Tag[]> = {};
  results.forEach((r, i) => {
    tagsByFile[ids[i]] = r.data?.items ?? [];
  });

  return {
    tagsByFile,
    isLoading: enabled && results.some((r) => r.isLoading),
  };
}

/** Create a new collection (ADMIN-only on the backend; the demo is ADMIN). */
export function useCreateTag() {
  const qc = useQueryClient();
  return useMutation<Tag, Error, CreateTagInput>({
    mutationFn: (body) => tagClient.create(body),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: tagKeys.list() });
    },
  });
}

/** Attach a media file to a collection. */
export function useAttachTag() {
  const qc = useQueryClient();
  return useMutation<TagAttachment, Error, { tagId: string; input: AttachTagInput }>({
    mutationFn: ({ tagId, input }) => tagClient.attach(tagId, input),
    onSuccess: (_data, { input }) => {
      void qc.invalidateQueries({ queryKey: tagKeys.byEntity(input.entityType, input.entityId) });
      void qc.invalidateQueries({ queryKey: tagKeys.all });
    },
  });
}

/** Detach a media file from a collection. */
export function useDetachTag() {
  const qc = useQueryClient();
  return useMutation<void, Error, { tagId: string; entityType: string; entityId: string }>({
    mutationFn: ({ tagId, entityType, entityId }) => tagClient.detach(tagId, entityType, entityId),
    onSuccess: (_data, { entityType, entityId }) => {
      void qc.invalidateQueries({ queryKey: tagKeys.byEntity(entityType, entityId) });
      void qc.invalidateQueries({ queryKey: tagKeys.all });
    },
  });
}
