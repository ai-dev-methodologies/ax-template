import { format, formatDistanceToNow } from 'date-fns';
import { ko } from 'date-fns/locale';

/** Relative "3분 전" style label. */
export function formatRelative(iso: string | null | undefined): string {
  if (!iso) return '방금';
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '방금';
  return formatDistanceToNow(date, { addSuffix: true, locale: ko });
}

/** Editorial dateline: "2026년 6월 5일" — the read view's byline date. */
export function formatDateline(iso: string | null | undefined): string {
  if (!iso) return '—';
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '—';
  return format(date, 'yyyy년 M월 d일', { locale: ko });
}

/** Absolute timestamp for tooltips / table cells: 2026-06-05 16:23. */
export function formatTimestamp(iso: string | null | undefined): string {
  if (!iso) return '—';
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '—';
  return format(date, 'yyyy-MM-dd HH:mm');
}

/** Strip HTML tags to a plain-text excerpt for list cards (no markup leakage). */
export function excerpt(html: string | null | undefined, max = 140): string {
  if (!html) return '';
  const text = html
    .replace(/<[^>]*>/g, ' ')
    .replace(/&nbsp;/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
  if (text.length <= max) return text;
  return `${text.slice(0, max).trimEnd()}…`;
}
