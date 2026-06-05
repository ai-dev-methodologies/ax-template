import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  notificationClient,
  type AppNotification,
  type NotificationPage,
} from '@/lib/api/notificationClient';

export const notificationKeys = {
  all: ['notifications'] as const,
  page: (page: number, size: number) => [...notificationKeys.all, 'page', page, size] as const,
  unread: ['notifications', 'unread-count'] as const,
};

export function useNotifications(page: number, size: number, enabled: boolean) {
  return useQuery<NotificationPage>({
    queryKey: notificationKeys.page(page, size),
    queryFn: () => notificationClient.list(page, size),
    enabled,
    placeholderData: keepPreviousData,
  });
}

export function useUnreadCount(enabled: boolean) {
  return useQuery<number>({
    queryKey: notificationKeys.unread,
    queryFn: () => notificationClient.unreadCount(),
    enabled,
    staleTime: 30_000,
  });
}

export function useMarkNotificationRead() {
  const qc = useQueryClient();
  return useMutation<AppNotification, Error, string>({
    mutationFn: (id) => notificationClient.markRead(id),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: notificationKeys.all });
    },
  });
}

export function useDismissNotification() {
  const qc = useQueryClient();
  return useMutation<void, Error, string>({
    mutationFn: (id) => notificationClient.dismiss(id),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: notificationKeys.all });
    },
  });
}
