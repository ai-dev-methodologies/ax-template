import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { activityClient, type ActivityFeedResult } from '@/lib/api/activityClient';

export const activityKeys = {
  all: ['activity'] as const,
  feed: (page: number, size: number) => [...activityKeys.all, 'feed', page, size] as const,
};

/** The creator's activity timeline (uploads, likes, posts). */
export function useActivityFeed(page = 0, size = 20, enabled = true) {
  return useQuery<ActivityFeedResult>({
    queryKey: activityKeys.feed(page, size),
    queryFn: () => activityClient.list(page, size, false),
    enabled,
  });
}

/** Mark one activity row read. */
export function useMarkActivityRead() {
  const qc = useQueryClient();
  return useMutation<void, Error, string>({
    mutationFn: (id) => activityClient.markRead(id),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: activityKeys.all });
    },
  });
}

/** Mark the whole feed read. */
export function useMarkAllActivityRead() {
  const qc = useQueryClient();
  return useMutation<{ markedCount: number }, Error, void>({
    mutationFn: () => activityClient.markAllRead(),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: activityKeys.all });
    },
  });
}
