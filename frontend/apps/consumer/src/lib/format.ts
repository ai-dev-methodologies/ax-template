import { format, formatDistanceToNow } from 'date-fns';
import { ko } from 'date-fns/locale';

/** Relative "3분 전" style label — the primary recency label across the feed. */
export function formatRelative(iso: string | null | undefined): string {
  if (!iso) return '방금';
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '방금';
  return formatDistanceToNow(date, { addSuffix: true, locale: ko });
}

/** Absolute timestamp for tooltips / detail headers: 2026-06-05 16:23. */
export function formatTimestamp(iso: string | null | undefined): string {
  if (!iso) return '—';
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '—';
  return format(date, 'yyyy-MM-dd HH:mm');
}

/**
 * Resolve a friendly display name from a backend principal id (a UUID). The
 * consumer feed never exposes raw UUIDs to the user, so we render a stable,
 * short "사용자 a1b2" handle derived from the id's leading hex. When {@code selfId}
 * is supplied and matches, the current user's own activity reads as "나".
 */
export function displayName(
  userId: string | null | undefined,
  selfId?: string | null,
): string {
  if (!userId) return '익명';
  if (selfId && userId === selfId) return '나';
  const head = userId.replace(/-/g, '').slice(0, 4);
  return `사용자 ${head}`;
}
