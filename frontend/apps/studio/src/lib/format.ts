/** Small presentational formatters for the studio. Pure functions, no UI. */

/** Human file size, e.g. 70 -> "70 B", 36648 -> "35.8 KB". */
export function formatBytes(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes < 0) return '–';
  if (bytes < 1024) return `${bytes} B`;
  const units = ['KB', 'MB', 'GB', 'TB'];
  let value = bytes / 1024;
  let unit = 0;
  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024;
    unit += 1;
  }
  return `${value.toFixed(value < 10 ? 1 : 0)} ${units[unit]}`;
}

/** Relative Korean time, e.g. "방금 전", "3분 전", "2시간 전", "어제", "5일 전". */
export function formatRelativeKo(iso: string): string {
  const then = new Date(iso).getTime();
  if (Number.isNaN(then)) return '';
  const diffSec = Math.round((Date.now() - then) / 1000);
  if (diffSec < 45) return '방금 전';
  const diffMin = Math.round(diffSec / 60);
  if (diffMin < 60) return `${diffMin}분 전`;
  const diffHour = Math.round(diffMin / 60);
  if (diffHour < 24) return `${diffHour}시간 전`;
  const diffDay = Math.round(diffHour / 24);
  if (diffDay === 1) return '어제';
  if (diffDay < 7) return `${diffDay}일 전`;
  return new Date(then).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
}

/** True when a content-type names an image the studio can preview inline. */
export function isImageContentType(contentType: string): boolean {
  return /^image\//i.test(contentType);
}
