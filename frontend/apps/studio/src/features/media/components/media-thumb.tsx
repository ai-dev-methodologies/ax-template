'use client';

import React from 'react';
import { FileWarning, ImageIcon } from 'lucide-react';
import { Spinner, cn } from '@ax/ui';
import { useMediaObjectUrl } from '@/features/media/hooks';
import { isImageContentType } from '@/lib/format';

interface MediaThumbProps {
  id: string;
  fileName: string;
  contentType: string;
  /** READY files can be downloaded/previewed; PENDING/QUARANTINED cannot. */
  ready: boolean;
  className?: string;
  /** force-loads eagerly (e.g. the detail hero). Defaults to lazy via in-view. */
  eager?: boolean;
}

/**
 * Authed image renderer. The download endpoint requires the JWT, so a bare
 * <img src> cannot load it (the browser won't attach the Bearer). This fetches
 * the blob through the shared authed download and renders the object URL. For
 * non-image or non-READY files it shows a typed placeholder rather than a broken
 * image. The <img> carries real alt text (the file name) for a11y (WCAG 1.1.1).
 *
 * This is a DOMAIN component (renders the studio's media), not a catalog UI
 * primitive — it composes the catalog Spinner + cn and the media hooks.
 */
export function MediaThumb({
  id,
  fileName,
  contentType,
  ready,
  className,
  eager = false,
}: MediaThumbProps) {
  const previewable = ready && isImageContentType(contentType);
  const { url, isLoading, isError } = useMediaObjectUrl(id, previewable);

  const base = cn(
    'relative grid place-items-center overflow-hidden bg-secondary',
    className,
  );

  if (!previewable) {
    return (
      <div className={base} role="img" aria-label={`${fileName} 미리보기 없음`}>
        <span className="flex flex-col items-center gap-1.5 text-muted-foreground">
          {ready ? (
            <ImageIcon aria-hidden className="h-8 w-8" />
          ) : (
            <FileWarning aria-hidden className="h-8 w-8" />
          )}
          <span className="px-3 text-center text-xs font-medium">
            {ready ? '이미지 미리보기 없음' : '검사 중이거나 차단된 파일'}
          </span>
        </span>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className={base} role="status" aria-live="polite">
        <Spinner className="h-6 w-6 text-[var(--ax-status-accent-fg)]" label={`${fileName} 불러오는 중`} />
      </div>
    );
  }

  if (isError || !url) {
    return (
      <div className={base} role="img" aria-label={`${fileName} 불러오기 실패`}>
        <span className="flex flex-col items-center gap-1.5 text-muted-foreground">
          <FileWarning aria-hidden className="h-8 w-8" />
          <span className="text-xs font-medium">불러오지 못했어요</span>
        </span>
      </div>
    );
  }

  return (
    <div className={base}>
      {/* A plain <img> is required here: the source is an authed blob object URL
          (createObjectURL) — next/image cannot fetch the download endpoint with
          a Bearer header, so its optimizer/loader does not apply. */}
      <img
        src={url}
        alt={fileName}
        loading={eager ? 'eager' : 'lazy'}
        className="h-full w-full object-cover"
      />
    </div>
  );
}
