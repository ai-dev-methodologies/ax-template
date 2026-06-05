/**
 * Activity-feed client — /api/activities. Backend: ActivityController.
 * Domain client (NOT a UI primitive) — composes the shared @ax/core authed fetch.
 *
 * The studio uses the feed for the creator's activity timeline (uploads, likes,
 * collection edits) and publishes an "uploaded" event when a file is stored.
 * Responses are no-store (per-caller PII) — that is a backend concern; this
 * client just reads JSON.
 *
 * Curl-verified shapes (2026-06-05, demo@ax.dev):
 *   POST /api/activities                          -> 201/200 ActivityEventResponse
 *   GET  /api/activities?page&size&unreadOnly     -> 200 { items, page, size, totalElements }
 *   POST /api/activities/{id}/read                -> 204
 *   POST /api/activities/mark-all-read            -> 200 { markedCount }
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

export interface ActivityFeedResult {
  items: ActivityEvent[];
  page: number;
  size: number;
  totalElements: number;
}

export interface PublishActivityInput {
  verb: string;
  objectType: string;
  objectId: string;
  metadata?: Record<string, unknown>;
  idempotencyKey?: string;
}

export const activityClient = {
  list: (page = 0, size = 20, unreadOnly = false): Promise<ActivityFeedResult> =>
    apiFetch<ActivityFeedResult>('/activities', { query: { page, size, unreadOnly: String(unreadOnly) } }),

  publish: (body: PublishActivityInput): Promise<ActivityEvent> =>
    apiFetch<ActivityEvent>('/activities', { method: 'POST', body }),

  markRead: (id: string): Promise<void> =>
    apiFetch<void>(`/activities/${encodeURIComponent(id)}/read`, { method: 'POST' }),

  markAllRead: (): Promise<{ markedCount: number }> =>
    apiFetch<{ markedCount: number }>('/activities/mark-all-read', { method: 'POST' }),
};
