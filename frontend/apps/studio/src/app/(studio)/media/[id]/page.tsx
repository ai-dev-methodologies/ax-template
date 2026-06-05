'use client';

import React, { use, useState } from 'react';
import Link from 'next/link';
import { ArrowLeft, Download, Plus, Trash2, X } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@ax/core';
import { Alert, Button, ConfirmDialog, cn } from '@ax/ui';
import { AvatarGroup, StatusBadge, type StatusKind } from '@ax/blocks';
import { MediaThumb } from '@/components/media-thumb';
import { ReactionButton } from '@/components/reaction-button';
import { ScreenError, ScreenLoading } from '@/components/screen-states';
import { useDeleteMedia, useMediaDetail } from '@/features/media/hooks';
import { useAttachTag, useDetachTag, useMediaTags, useTags } from '@/features/tags/hooks';
import { useProfile } from '@/features/profile/hooks';
import { fileClient, type StoredFile } from '@/lib/api/fileClient';
import { formatBytes, formatRelativeKo } from '@/lib/format';
import { errorMessage } from '@/lib/errors';

function statusBadge(status: StoredFile['status']): { kind: StatusKind; label: string } {
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

/**
 * Media detail — preview (authed blob) + the creator (AvatarGroup) + collections
 * (tags, attach/detach) + the signature reaction (favorite/like with the pop) +
 * an authed download + delete. Composes catalog blocks (AvatarGroup, StatusBadge)
 * + @ax/ui (Button, ConfirmDialog, Alert) + the studio domain components.
 */
export default function MediaDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const router = useRouter();
  const enabled = Boolean(useAuthStore((s) => s.accessToken));

  const detail = useMediaDetail(id, enabled);
  const profile = useProfile(enabled);
  const mediaTags = useMediaTags(id, enabled);
  const allTags = useTags(enabled);
  const attach = useAttachTag();
  const detach = useDetachTag();
  const del = useDeleteMedia();

  const [showPicker, setShowPicker] = useState(false);
  const [downloading, setDownloading] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);

  const handleDownload = async (): Promise<void> => {
    if (!detail.data) return;
    setDownloading(true);
    try {
      const blob = await fileClient.download(id);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = detail.data.fileName;
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
    } finally {
      setDownloading(false);
    }
  };

  const handleDelete = async (): Promise<void> => {
    await del.mutateAsync(id);
    router.push('/gallery');
  };

  if (detail.isLoading) return <ScreenLoading label="작품 불러오는 중" />;
  if (detail.isError || !detail.data)
    return <ScreenError error={new Error(errorMessage(detail.error) || '작품을 찾을 수 없어요.')} />;

  const file = detail.data;
  const badge = statusBadge(file.status);
  const attachedTags = mediaTags.data?.items ?? [];
  const attachedIds = new Set(attachedTags.map((t) => t.id));
  const available = (allTags.data?.items ?? []).filter((t) => !attachedIds.has(t.id));
  const creatorName = profile.data?.email ?? '크리에이터';

  return (
    <article className="space-y-6">
      <Button asChild variant="ghost" size="sm">
        <Link href="/gallery">
          <ArrowLeft aria-hidden />갤러리로
        </Link>
      </Button>

      <div className="grid gap-7 lg:grid-cols-[1.4fr_1fr]">
        {/* Preview */}
        <div className="overflow-hidden rounded-[var(--radius)] border border-border bg-card shadow-lg">
          <div className="relative aspect-[4/3] w-full">
            <MediaThumb
              id={file.id}
              fileName={file.fileName}
              contentType={file.contentType}
              ready={file.status === 'READY'}
              className="h-full w-full"
              eager
            />
            <span className="absolute right-3 top-3">
              <StatusBadge status={badge.kind} label={badge.label} />
            </span>
          </div>
        </div>

        {/* Meta + actions */}
        <div className="space-y-6">
          <header className="space-y-2">
            <h1 className="ax-display break-words text-2xl font-extrabold tracking-tight text-foreground">
              {file.fileName}
            </h1>
            <p className="text-sm text-muted-foreground">
              {formatRelativeKo(file.uploadedAt)} · {formatBytes(file.sizeBytes)} · {file.contentType}
            </p>
          </header>

          {/* Creator */}
          <section aria-label="크리에이터" className="flex items-center gap-3">
            <AvatarGroup members={[{ name: creatorName }]} label="크리에이터" />
            <div className="min-w-0">
              <p className="truncate text-sm font-semibold text-foreground">{creatorName}</p>
              <p className="text-xs text-muted-foreground">이 작품의 크리에이터</p>
            </div>
          </section>

          {/* Reaction (signature delight) + download */}
          <section aria-label="반응" className="flex flex-wrap items-center gap-3">
            <ReactionButton entityId={file.id} enabled={enabled} note={file.fileName} />
            <Button
              variant="outline"
              size="lg"
              loading={downloading}
              onClick={handleDownload}
              disabled={file.status !== 'READY'}
            >
              {!downloading && <Download aria-hidden />}
              다운로드
            </Button>
          </section>

          {/* Collections (tags) */}
          <section aria-labelledby="collections-heading" className="space-y-3">
            <div className="flex items-center justify-between">
              <h2 id="collections-heading" className="ax-display text-lg font-bold text-foreground">
                컬렉션
              </h2>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setShowPicker((v) => !v)}
                aria-expanded={showPicker}
              >
                <Plus aria-hidden />컬렉션에 추가
              </Button>
            </div>

            {mediaTags.isLoading ? (
              <ScreenLoading label="컬렉션 불러오는 중" />
            ) : attachedTags.length === 0 ? (
              <p className="text-sm text-muted-foreground">아직 어떤 컬렉션에도 담기지 않았어요.</p>
            ) : (
              <ul className="flex flex-wrap gap-2">
                {attachedTags.map((tag) => (
                  <li key={tag.id}>
                    <span className="inline-flex items-center gap-1.5 rounded-full border border-[var(--ax-status-accent-bg)] bg-[var(--ax-status-accent-bg)] px-3 py-1 text-sm font-semibold text-[var(--ax-status-accent-fg)]">
                      {tag.name}
                      <button
                        type="button"
                        onClick={() =>
                          detach.mutate({ tagId: tag.id, entityType: 'file', entityId: file.id })
                        }
                        disabled={detach.isPending}
                        aria-label={`${tag.name} 컬렉션에서 빼기`}
                        className="rounded-full text-muted-foreground transition-colors hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:opacity-50"
                      >
                        <X aria-hidden className="h-3.5 w-3.5" />
                      </button>
                    </span>
                  </li>
                ))}
              </ul>
            )}

            {/* Picker of available collections */}
            {showPicker ? (
              <div className="rounded-[var(--radius)] border border-border bg-card p-3 shadow-sm">
                {available.length === 0 ? (
                  <p className="text-sm text-muted-foreground">
                    추가할 수 있는 컬렉션이 없어요.{' '}
                    <Link href="/collections" className="font-medium text-[var(--ax-status-accent-fg)] underline">
                      컬렉션 만들기
                    </Link>
                  </p>
                ) : (
                  <ul className="flex flex-wrap gap-2">
                    {available.map((tag) => (
                      <li key={tag.id}>
                        <button
                          type="button"
                          onClick={() =>
                            attach.mutate({
                              tagId: tag.id,
                              input: { entityType: 'file', entityId: file.id },
                            })
                          }
                          disabled={attach.isPending}
                          className={cn(
                            'rounded-full border border-dashed border-border px-3 py-1 text-sm font-medium text-muted-foreground transition-colors',
                            'hover:border-[var(--ax-status-accent-fg)] hover:text-[var(--ax-status-accent-fg)]',
                            'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:opacity-50',
                          )}
                        >
                          + {tag.name}
                        </button>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            ) : null}

            {attach.isError ? <Alert variant="error">{errorMessage(attach.error)}</Alert> : null}
          </section>

          {/* Danger zone */}
          <section aria-label="작품 삭제" className="border-t border-border pt-4">
            <Button
              variant="destructive"
              size="sm"
              loading={del.isPending}
              onClick={() => setConfirmOpen(true)}
            >
              {!del.isPending && <Trash2 aria-hidden />}작품 삭제
            </Button>
            <ConfirmDialog
              open={confirmOpen}
              onOpenChange={setConfirmOpen}
              title="이 작품을 삭제할까요?"
              description="삭제하면 갤러리에서 사라지고 되돌릴 수 없어요."
              confirmLabel="삭제"
              cancelLabel="취소"
              tone="destructive"
              loading={del.isPending}
              onConfirm={handleDelete}
            />
            {del.isError ? (
              <div className="mt-3">
                <Alert variant="error">{errorMessage(del.error)}</Alert>
              </div>
            ) : null}
          </section>
        </div>
      </div>
    </article>
  );
}
