import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  commentClient,
  type Comment,
  type CommentHistory,
  type CommentListResult,
  type CreateCommentInput,
} from '@/lib/api/commentClient';

export const commentKeys = {
  all: ['comments'] as const,
  byEntity: (entityType: string, entityId: string) =>
    [...commentKeys.all, 'by-entity', entityType, entityId] as const,
  history: (id: string) => [...commentKeys.all, 'history', id] as const,
};

export function useComments(entityType: string, entityId: string, enabled: boolean) {
  return useQuery<CommentListResult>({
    queryKey: commentKeys.byEntity(entityType, entityId),
    queryFn: () => commentClient.byEntity(entityType, entityId),
    enabled,
  });
}

export function useCommentHistory(id: string, enabled: boolean) {
  return useQuery<CommentHistory>({
    queryKey: commentKeys.history(id),
    queryFn: () => commentClient.history(id),
    enabled,
  });
}

export function useCreateComment(entityType: string, entityId: string) {
  const qc = useQueryClient();
  return useMutation<Comment, Error, CreateCommentInput>({
    mutationFn: (body) => commentClient.create(body),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: commentKeys.byEntity(entityType, entityId) });
    },
  });
}

export function useEditComment(entityType: string, entityId: string) {
  const qc = useQueryClient();
  return useMutation<Comment, Error, { id: string; body: string }>({
    mutationFn: ({ id, body }) => commentClient.edit(id, body),
    onSuccess: (_data, { id }) => {
      void qc.invalidateQueries({ queryKey: commentKeys.byEntity(entityType, entityId) });
      void qc.invalidateQueries({ queryKey: commentKeys.history(id) });
    },
  });
}

export function useDeleteComment(entityType: string, entityId: string) {
  const qc = useQueryClient();
  return useMutation<void, Error, string>({
    mutationFn: (id) => commentClient.remove(id),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: commentKeys.byEntity(entityType, entityId) });
    },
  });
}
