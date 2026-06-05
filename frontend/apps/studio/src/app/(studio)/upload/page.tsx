'use client';

import React, { useCallback, useRef, useState } from 'react';
import Link from 'next/link';
import { CheckCircle2, ImagePlus, UploadCloud } from 'lucide-react';
import { Alert, Button, Input, Label, cn } from '@ax/ui';
import { PageHeader } from '@/components/page-header';
import { MediaThumb } from '@/components/media-thumb';
import { IMAGE_ACCEPT, UPLOAD_LIMITS, type StoredFile } from '@/lib/api/fileClient';
import { useUploadMedia } from '@/features/media/hooks';
import { formatBytes } from '@/lib/format';
import { errorMessage } from '@/lib/errors';

const ALLOWED = new Set<string>(UPLOAD_LIMITS.allowedMimeTypes);

/** Client-side pre-check mirroring the backend allowlist + size cap. */
function validate(file: File): string | null {
  if (file.type && !ALLOWED.has(file.type)) {
    return `지원하지 않는 형식이에요 (${file.type || '알 수 없음'}). 이미지·PDF·텍스트·zip만 올릴 수 있어요.`;
  }
  if (file.size > UPLOAD_LIMITS.maxFileSizeBytes) {
    return `파일이 너무 커요 (${formatBytes(file.size)}). 최대 ${formatBytes(UPLOAD_LIMITS.maxFileSizeBytes)}까지 가능해요.`;
  }
  return null;
}

/**
 * Upload — a real multipart file upload to file-storage. Supports drag/drop AND
 * the catalog Input file picker. Validates against the backend's allowlist +
 * size cap before sending, shows an in-flight progress affordance, surfaces RFC
 * 9457 errors, and renders the stored file (status + preview) on success. Each
 * upload is auto-favorited (so it appears in the gallery) and publishes an
 * activity (see useUploadMedia).
 */
export default function UploadPage() {
  const upload = useUploadMedia();
  const [stored, setStored] = useState<StoredFile | null>(null);
  const [localError, setLocalError] = useState<string | null>(null);
  const [dragging, setDragging] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  const handleFile = useCallback(
    (file: File) => {
      setLocalError(null);
      setStored(null);
      const problem = validate(file);
      if (problem) {
        setLocalError(problem);
        return;
      }
      upload.mutate(file, {
        onSuccess: ({ file: saved }) => setStored(saved),
      });
    },
    [upload],
  );

  const onInputChange = (e: React.ChangeEvent<HTMLInputElement>): void => {
    const file = e.target.files?.[0];
    if (file) handleFile(file);
    // reset so the same file can be re-picked
    e.target.value = '';
  };

  const onDrop = (e: React.DragEvent<HTMLDivElement>): void => {
    e.preventDefault();
    setDragging(false);
    const file = e.dataTransfer.files?.[0];
    if (file) handleFile(file);
  };

  return (
    <section aria-labelledby="upload-heading" className="max-w-2xl">
      <span id="upload-heading" className="sr-only">
        업로드
      </span>
      <PageHeader
        title="업로드"
        description="작품을 드래그하거나 파일을 선택해 스튜디오에 올리세요. 이미지·PDF·텍스트·zip을 지원해요."
      />

      {/* Drag-and-drop zone. The visible label + hidden input keep the picker
          accessible; the zone itself is a labelled region. */}
      <div
        role="group"
        aria-label="파일 업로드 영역"
        onDragOver={(e) => {
          e.preventDefault();
          setDragging(true);
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={onDrop}
        className={cn(
          'flex flex-col items-center justify-center gap-4 rounded-[var(--radius)] border-2 border-dashed bg-card/60 px-6 py-14 text-center transition-colors shadow-sm',
          dragging
            ? 'border-[var(--ax-status-accent-fg)] bg-[var(--ax-status-accent-bg)]'
            : 'border-border',
        )}
      >
        <span
          aria-hidden
          className="inline-flex h-16 w-16 items-center justify-center rounded-[var(--radius)] bg-[var(--ax-status-accent-bg)] text-[var(--ax-status-accent-fg)]"
        >
          <UploadCloud className="h-8 w-8" />
        </span>
        <div className="space-y-1">
          <p className="ax-display text-xl font-bold text-foreground">
            여기로 작품을 끌어다 놓으세요
          </p>
          <p className="text-sm text-muted-foreground">
            또는 아래에서 파일을 선택하세요 · 최대 {formatBytes(UPLOAD_LIMITS.maxFileSizeBytes)}
          </p>
        </div>

        <div className="w-full max-w-sm space-y-1.5 text-left">
          <Label htmlFor="file-input">파일 선택</Label>
          <Input
            ref={inputRef}
            id="file-input"
            type="file"
            accept={IMAGE_ACCEPT}
            onChange={onInputChange}
            disabled={upload.isPending}
            aria-describedby="file-help"
          />
          <p id="file-help" className="text-xs text-muted-foreground">
            이미지(JPEG·PNG·GIF·WebP·SVG)를 권장해요. 갤러리에서 미리보기로 표시됩니다.
          </p>
        </div>

        <Button
          type="button"
          size="lg"
          loading={upload.isPending}
          onClick={() => inputRef.current?.click()}
        >
          {!upload.isPending && <ImagePlus aria-hidden />}
          {upload.isPending ? '업로드 중' : '파일 선택'}
        </Button>
      </div>

      {/* In-flight progress affordance (the upload is a single request; we show a
          determinate-feeling status rather than a fake percent). */}
      {upload.isPending ? (
        <p className="mt-4 text-sm font-medium text-[var(--ax-status-accent-fg)]" role="status" aria-live="polite">
          작품을 스튜디오로 보내는 중이에요…
        </p>
      ) : null}

      {localError ? (
        <div className="mt-4">
          <Alert variant="error">{localError}</Alert>
        </div>
      ) : null}

      {upload.isError && !localError ? (
        <div className="mt-4">
          <Alert variant="error">{errorMessage(upload.error)}</Alert>
        </div>
      ) : null}

      {/* Stored result. */}
      {stored ? (
        <div className="ax-rise mt-6 overflow-hidden rounded-[var(--radius)] border border-border bg-card shadow-md">
          <div className="flex items-center gap-2 border-b border-border px-5 py-3 text-[var(--ax-status-success-fg)]">
            <CheckCircle2 aria-hidden className="h-5 w-5" />
            <span className="font-semibold">업로드 완료!</span>
          </div>
          <div className="flex flex-col gap-4 p-5 sm:flex-row">
            <div className="aspect-square w-full shrink-0 overflow-hidden rounded-[var(--radius)] sm:w-40">
              <MediaThumb
                id={stored.id}
                fileName={stored.fileName}
                contentType={stored.contentType}
                ready={stored.status === 'READY'}
                className="h-full w-full"
                eager
              />
            </div>
            <dl className="min-w-0 flex-1 space-y-2 text-sm">
              <div className="flex justify-between gap-3">
                <dt className="text-muted-foreground">파일명</dt>
                <dd className="min-w-0 truncate font-medium text-foreground">{stored.fileName}</dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-muted-foreground">형식</dt>
                <dd className="font-medium text-foreground">{stored.contentType}</dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-muted-foreground">크기</dt>
                <dd className="font-medium tabular-nums text-foreground">{formatBytes(stored.sizeBytes)}</dd>
              </div>
              <div className="flex justify-between gap-3">
                <dt className="text-muted-foreground">상태</dt>
                <dd className="font-medium text-foreground">{stored.status}</dd>
              </div>
              <div className="pt-2">
                <Button asChild variant="outline" size="sm">
                  <Link href={`/media/${stored.id}`}>작품 자세히 보기</Link>
                </Button>
              </div>
            </dl>
          </div>
        </div>
      ) : null}
    </section>
  );
}
