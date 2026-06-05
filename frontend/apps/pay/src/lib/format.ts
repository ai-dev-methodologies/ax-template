/**
 * Small presentation helpers (non-money). Money lives in money.ts so the
 * currency rules stay in one place.
 */

/** Format an ISO timestamp as a Korean short date-time, e.g. "26. 6. 5. 21:30". */
export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '—';
  return new Intl.DateTimeFormat('ko-KR', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(d);
}

/** Format an ISO timestamp as a Korean short date only. */
export function formatDate(iso: string | null | undefined): string {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '—';
  return new Intl.DateTimeFormat('ko-KR', { dateStyle: 'medium' }).format(d);
}

/** Human byte size, tabular-friendly (e.g. "1.2 KB"). */
export function formatBytes(bytes: number | null | undefined): string {
  if (bytes === null || bytes === undefined) return '—';
  if (bytes < 1024) return `${bytes} B`;
  const kb = bytes / 1024;
  if (kb < 1024) return `${kb.toFixed(1)} KB`;
  return `${(kb / 1024).toFixed(1)} MB`;
}

/** Short id for display (first segment of a UUID). */
export function shortId(id: string | null | undefined): string {
  if (!id) return '—';
  return id.split('-')[0];
}
