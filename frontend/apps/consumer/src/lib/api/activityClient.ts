/**
 * Activity-feed client — /api/activities (publish / feed / mark-read).
 * Backend: ActivityController. Auth: any authenticated user (caller is derived
 * from the JWT; clients never pass a userId).
 * Domain client (NOT a UI primitive) — composes the shared @ax/core authed fetch.
 *
 * Curl-verified shapes (2026-06-05, demo@ax.dev):
 *   POST /api/activities      -> 201/200 ActivityEventResponse
 *   GET  /api/activities      -> 200 { items, page, size, totalElements }
 *   POST /api/activities/{id}/read     -> 204
 *   POST /api/activities/mark-all-read -> 200 { markedCount }
 */
import { apiFetch } from '@ax/core';

export interface ActivityEvent {
  id: string;
  actorUserId: string;
  verb: string;
  objectType: string;
  objectId: string;
  subjectType: string | null;
  subjectId: string | null;
  metadata: Record<string, unknown>;
  youAreInAudience: boolean;
  createdAt: string;
  readAt: string | null;
}

export interface ActivityFeedPage {
  items: ActivityEvent[];
  page: number;
  size: number;
  totalElements: number;
}

export interface PublishActivityInput {
  verb: string;
  objectType: string;
  objectId: string;
  subjectType?: string;
  subjectId?: string;
  audienceUserIds?: string[];
  metadata?: Record<string, unknown>;
  idempotencyKey?: string;
}

export interface MarkAllReadResult {
  markedCount: number;
}

export const activityClient = {
  feed: (page: number, size: number): Promise<ActivityFeedPage> =>
    apiFetch<ActivityFeedPage>('/activities', { query: { page, size } }),

  publish: (body: PublishActivityInput): Promise<ActivityEvent> =>
    apiFetch<ActivityEvent>('/activities', { method: 'POST', body }),

  markRead: (id: string): Promise<void> =>
    apiFetch<void>(`/activities/${id}/read`, { method: 'POST' }),

  markAllRead: (): Promise<MarkAllReadResult> =>
    apiFetch<MarkAllReadResult>('/activities/mark-all-read', { method: 'POST' }),
};
