import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  activityClient,
  type ActivityEvent,
  type ActivityFeedPage,
  type MarkAllReadResult,
  type PublishActivityInput,
} from '@/lib/api/activityClient';

export const feedKeys = {
  all: ['feed'] as const,
  page: (page: number, size: number) => [...feedKeys.all, 'page', page, size] as const,
};

export function useFeed(page: number, size: number, enabled: boolean) {
  return useQuery<ActivityFeedPage>({
    queryKey: feedKeys.page(page, size),
    queryFn: () => activityClient.feed(page, size),
    enabled,
    placeholderData: keepPreviousData,
  });
}

export function useMarkActivityRead() {
  const qc = useQueryClient();
  return useMutation<void, Error, string>({
    mutationFn: (id: string) => activityClient.markRead(id),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: feedKeys.all });
    },
  });
}

export function useMarkActivitiesAllRead() {
  const qc = useQueryClient();
  return useMutation<MarkAllReadResult, Error, void>({
    mutationFn: () => activityClient.markAllRead(),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: feedKeys.all });
    },
  });
}

export function usePublishActivity() {
  const qc = useQueryClient();
  return useMutation<ActivityEvent, Error, PublishActivityInput>({
    mutationFn: (body) => activityClient.publish(body),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: feedKeys.all });
    },
  });
}
