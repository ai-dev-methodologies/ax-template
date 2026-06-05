/**
 * Notification client — /api/notifications (list / mark-read / dismiss).
 * Backend: NotificationController. Auth: any authenticated user (owner-only,
 * caller derived from JWT). POST (send) is ADMIN-only and is NOT exposed here —
 * notifications are produced by backend domains, not the consumer UI.
 * Domain client (NOT a UI primitive) — composes the shared @ax/core authed fetch.
 *
 * Curl-verified shapes (2026-06-05, demo@ax.dev):
 *   GET    /api/notifications              -> 200 { content, totalElements, totalPages, page, size }
 *                                            (+ X-Unread-Count response header)
 *   PATCH  /api/notifications/{id}/read    -> 200 NotificationResponse
 *   DELETE /api/notifications/{id}         -> 204
 *
 * Unread count: rather than reading the X-Unread-Count header (apiFetch returns
 * the parsed body only), we query the `status=UNREAD` filter with size=1 and use
 * its `totalElements` — a stable, body-only source the proxy/cache cannot strip.
 */
import { apiFetch } from '@ax/core';

export type NotificationStatus = 'UNREAD' | 'READ';

export interface AppNotification {
  id: string;
  type: string;
  title: string;
  body: string | null;
  link: string | null;
  status: NotificationStatus;
  createdAt: string;
  updatedAt: string;
}

export interface NotificationPage {
  content: AppNotification[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export const notificationClient = {
  list: (page: number, size: number, status?: 'UNREAD' | 'READ' | 'ALL'): Promise<NotificationPage> =>
    apiFetch<NotificationPage>('/notifications', { query: { page, size, status } }),

  unreadCount: async (): Promise<number> => {
    const page = await apiFetch<NotificationPage>('/notifications', {
      query: { page: 0, size: 1, status: 'UNREAD' },
    });
    return page.totalElements;
  },

  markRead: (id: string): Promise<AppNotification> =>
    apiFetch<AppNotification>(`/notifications/${id}/read`, { method: 'PATCH' }),

  dismiss: (id: string): Promise<void> =>
    apiFetch<void>(`/notifications/${id}`, { method: 'DELETE' }),
};
