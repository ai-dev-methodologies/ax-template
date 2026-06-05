import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  favoriteClient,
  type AddFavoriteInput,
  type Favorite,
  type FavoriteListResult,
} from '@/lib/api/favoriteClient';

export const favoriteKeys = {
  all: ['favorites'] as const,
  list: (entityType?: string) => [...favoriteKeys.all, 'list', entityType ?? 'all'] as const,
  check: (entityType: string, entityId: string) =>
    [...favoriteKeys.all, 'check', entityType, entityId] as const,
};

export function useFavorites(entityType: string | undefined, enabled: boolean) {
  return useQuery<FavoriteListResult>({
    queryKey: favoriteKeys.list(entityType),
    queryFn: () => favoriteClient.list(entityType),
    enabled,
  });
}

export function useIsFavorited(entityType: string, entityId: string, enabled: boolean) {
  return useQuery<{ favorited: boolean }>({
    queryKey: favoriteKeys.check(entityType, entityId),
    queryFn: () => favoriteClient.check(entityType, entityId),
    enabled,
  });
}

export function useAddFavorite() {
  const qc = useQueryClient();
  return useMutation<Favorite, Error, AddFavoriteInput>({
    mutationFn: (body) => favoriteClient.add(body),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: favoriteKeys.all });
    },
  });
}

export function useRemoveFavorite() {
  const qc = useQueryClient();
  return useMutation<void, Error, { entityType: string; entityId: string }>({
    mutationFn: ({ entityType, entityId }) => favoriteClient.remove(entityType, entityId),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: favoriteKeys.all });
    },
  });
}
