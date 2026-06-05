'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { Bookmark, BookmarkX } from 'lucide-react';
import { useAuthStore } from '@ax/core';
import { Button, Card, CardContent, ConfirmDialog } from '@ax/ui';
import { EmptyState, ErrorState, LoadingState } from '@/components/screen-states';
import { useFavorites, useRemoveFavorite } from '@/features/favorites/hooks';
import { formatRelative } from '@/lib/format';
import { errorMessage } from '@/lib/errors';
import type { Favorite } from '@/lib/api/favoriteClient';

export default function FavoritesPage() {
  const { accessToken } = useAuthStore();
  const enabled = Boolean(accessToken);
  const favorites = useFavorites(undefined, enabled);
  const remove = useRemoveFavorite();
  const [pending, setPending] = useState<Favorite | null>(null);

  const items = favorites.data?.items ?? [];

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">즐겨찾기</h1>
        <p className="mt-1 text-sm text-muted-foreground">저장해 둔 게시물을 한눈에 확인하세요.</p>
      </header>

      {favorites.isLoading ? (
        <LoadingState label="즐겨찾기 불러오는 중" />
      ) : favorites.isError ? (
        <ErrorState message={errorMessage(favorites.error)} onRetry={() => favorites.refetch()} />
      ) : items.length === 0 ? (
        <EmptyState
          icon={<Bookmark aria-hidden className="h-6 w-6" />}
          title="저장한 항목이 없어요"
          description="피드에서 마음에 드는 게시물을 즐겨찾기에 추가해 보세요."
          action={
            <Button asChild variant="outline" size="sm">
              <Link href="/">피드 둘러보기</Link>
            </Button>
          }
        />
      ) : (
        <ul className="space-y-3">
          {items.map((fav) => (
            <li key={fav.id}>
              <Card className="shadow-sm transition-transform duration-200 ease-out hover:-translate-y-0.5 motion-reduce:hover:translate-y-0">
                <CardContent className="flex items-center justify-between gap-4 p-4">
                  <Link
                    href={`/post/${encodeURIComponent(fav.entityId)}`}
                    className="min-w-0 flex-1 rounded-[var(--radius)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-card"
                  >
                    <span className="block truncate text-sm font-semibold text-foreground">
                      {fav.note?.trim() || fav.entityId}
                    </span>
                    <span className="mt-0.5 block text-xs text-muted-foreground">
                      {fav.entityType} · {formatRelative(fav.favoritedAt)}
                    </span>
                  </Link>
                  <Button
                    variant="ghost"
                    size="sm"
                    aria-label="즐겨찾기 해제"
                    onClick={() => setPending(fav)}
                  >
                    <BookmarkX aria-hidden />
                  </Button>
                </CardContent>
              </Card>
            </li>
          ))}
        </ul>
      )}

      <ConfirmDialog
        open={pending !== null}
        onOpenChange={(open) => {
          if (!open) setPending(null);
        }}
        title="즐겨찾기에서 제거할까요?"
        description={pending?.note?.trim() || pending?.entityId}
        confirmLabel="제거"
        tone="destructive"
        loading={remove.isPending}
        onConfirm={() => {
          if (pending) {
            remove.mutate(
              { entityType: pending.entityType, entityId: pending.entityId },
              { onSuccess: () => setPending(null) },
            );
          }
        }}
      />
    </div>
  );
}
