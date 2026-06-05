import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  tagClient,
  type CreateTagInput,
  type Tag,
  type TagList,
  type UpdateTagInput,
} from '@/lib/api/tagClient';

export const tagKeys = {
  all: ['tags'] as const,
  list: () => [...tagKeys.all, 'list'] as const,
  forArticle: (articleId: string) => [...tagKeys.all, 'article', articleId] as const,
};

/** All defined tags (the tags screen + the editor's assignment picker). */
export function useTags() {
  return useQuery<TagList>({
    queryKey: tagKeys.list(),
    queryFn: () => tagClient.list(),
  });
}

/** Tags currently attached to a given article. */
export function useArticleTags(articleId: string | undefined) {
  return useQuery<TagList>({
    queryKey: tagKeys.forArticle(articleId ?? ''),
    queryFn: () => tagClient.forArticle(articleId as string),
    enabled: Boolean(articleId),
  });
}

export function useCreateTag() {
  const qc = useQueryClient();
  return useMutation<Tag, Error, CreateTagInput>({
    mutationFn: (input) => tagClient.create(input),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: tagKeys.list() });
    },
  });
}

export function useUpdateTag() {
  const qc = useQueryClient();
  return useMutation<Tag, Error, { id: string; input: UpdateTagInput }>({
    mutationFn: ({ id, input }) => tagClient.update(id, input),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: tagKeys.list() });
    },
  });
}

export function useDeleteTag() {
  const qc = useQueryClient();
  return useMutation<void, Error, string>({
    mutationFn: (id) => tagClient.remove(id),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: tagKeys.all });
    },
  });
}

/** Attach a tag to an article. */
export function useAttachTag(articleId: string) {
  const qc = useQueryClient();
  return useMutation<unknown, Error, string>({
    mutationFn: (tagId) => tagClient.attachToArticle(tagId, articleId),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: tagKeys.forArticle(articleId) });
    },
  });
}

/** Detach a tag from an article. */
export function useDetachTag(articleId: string) {
  const qc = useQueryClient();
  return useMutation<void, Error, string>({
    mutationFn: (tagId) => tagClient.detachFromArticle(tagId, articleId),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: tagKeys.forArticle(articleId) });
    },
  });
}
