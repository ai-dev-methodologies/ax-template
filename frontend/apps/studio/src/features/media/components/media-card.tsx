'use client';

import React from 'react';
import Link from 'next/link';
import { StatusBadge, type StatusKind } from '@ax/blocks';
import { cn } from '@ax/ui';
import { MediaThumb } from './media-thumb';
import { formatBytes } from '@/lib/format';
import type { StoredFile } from '@/lib/api/fileClient';

/** Map a file lifecycle status to the catalog StatusBadge kind + Korean label. */
function statusToBadge(status: StoredFile['status']): { kind: StatusKind; label: string } {
  switch (status) {
    case 'READY':
      return { kind: 'success', label: '공개' };
    case 'PENDING':
      return { kind: 'pending', label: '검사 중' };
    case 'QUARANTINED':
      return { kind: 'failed', label: '차단됨' };
    default:
      return { kind: 'expired', label: '삭제됨' };
  }
}

interface MediaCardProps {
  file: StoredFile;
  /** stagger index for the entrance cascade (0–11, mapped to a delay class) */
  index?: number;
}

/**
 * The persona stagger reads --ax-i for its delay. We set it via a Tailwind
 * arbitrary-property class (not an inline style) so the app stays inline-style
 * free; the cascade caps at 12 (matching the globals.css clamp).
 */
const STAGGER_CLASS = [
  '[--ax-i:0]', '[--ax-i:1]', '[--ax-i:2]', '[--ax-i:3]', '[--ax-i:4]', '[--ax-i:5]',
  '[--ax-i:6]', '[--ax-i:7]', '[--ax-i:8]', '[--ax-i:9]', '[--ax-i:10]', '[--ax-i:11]',
] as const;

export function staggerClass(index: number): string {
  return STAGGER_CLASS[Math.min(Math.max(index, 0), STAGGER_CLASS.length - 1)];
}

/**
 * A gallery tile. Composes the catalog StatusBadge + the studio MediaThumb,
 * wrapped in a link to the media detail. The layered shadow + hover float come
 * from the persona shell (.ax-float / .shadow-md). Domain component, not a
 * catalog primitive.
 */
export function MediaCard({ file, index = 0 }: MediaCardProps) {
  const badge = statusToBadge(file.status);
  return (
    <Link
      href={`/media/${file.id}`}
      className={cn(
        'ax-stagger ax-float group block overflow-hidden rounded-[var(--radius)] border border-border bg-card shadow-md',
        staggerClass(index),
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background',
      )}
    >
      <div className="relative aspect-[4/3] w-full">
        <MediaThumb
          id={file.id}
          fileName={file.fileName}
          contentType={file.contentType}
          ready={file.status === 'READY'}
          className="h-full w-full"
        />
        <span className="absolute right-2.5 top-2.5">
          <StatusBadge status={badge.kind} label={badge.label} />
        </span>
      </div>
      <div className="flex items-center justify-between gap-2 px-4 py-3">
        <span className="ax-display min-w-0 truncate text-base font-bold text-foreground">
          {file.fileName}
        </span>
        <span className="shrink-0 text-xs font-medium text-muted-foreground">
          {formatBytes(file.sizeBytes)}
        </span>
      </div>
    </Link>
  );
}
