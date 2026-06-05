import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  favoriteClient,
  type AddFavoriteInput,
  type Favorite,
} from '@/lib/api/favoriteClient';
import { mediaKeys } from '@/features/media/hooks';

export const favoriteKeys = {
  all: ['favorites'] as const,
  check: (entityType: string, entityId: string) =>
    [...favoriteKeys.all, 'check', entityType, entityId] as const,
  count: (entityType: string, entityId: string) =>
    [...favoriteKeys.all, 'count', entityType, entityId] as const,
};

/** Is this entity liked/favorited by the caller? */
export function useIsFavorited(entityType: string, entityId: string, enabled: boolean) {
  return useQuery<{ favorited: boolean }>({
    queryKey: favoriteKeys.check(entityType, entityId),
    queryFn: () => favoriteClient.check(entityType, entityId),
    enabled,
  });
}

/** Global like count for an entity (any user). */
export function useFavoriteCount(entityType: string, entityId: string, enabled: boolean) {
  return useQuery<{ count: number }>({
    queryKey: favoriteKeys.count(entityType, entityId),
    queryFn: () => favoriteClient.count(entityType, entityId),
    enabled,
  });
}

export function useAddFavorite() {
  const qc = useQueryClient();
  return useMutation<Favorite, Error, AddFavoriteInput>({
    mutationFn: (body) => favoriteClient.add(body),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: favoriteKeys.all });
      void qc.invalidateQueries({ queryKey: mediaKeys.index() });
    },
  });
}

export function useRemoveFavorite() {
  const qc = useQueryClient();
  return useMutation<void, Error, { entityType: string; entityId: string }>({
    mutationFn: ({ entityType, entityId }) => favoriteClient.remove(entityType, entityId),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: favoriteKeys.all });
      void qc.invalidateQueries({ queryKey: mediaKeys.index() });
    },
  });
}
