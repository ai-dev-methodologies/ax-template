'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import { CheckCheck, PartyPopper, Send } from 'lucide-react';
import { useAuthStore } from '@ax/core';
import { Button, Card, CardContent, Input } from '@ax/ui';
import { ActivityFeed, AnimatedBadge, type ActivityFeedItem } from '@ax/blocks';
import { EmptyState, ErrorState, LoadingState } from '@/components/screen-states';
import {
  useFeed,
  useMarkActivitiesAllRead,
  useMarkActivityRead,
  usePublishActivity,
} from '@/features/feed/hooks';
import { useProfile } from '@/features/profile/hooks';
import { displayName, formatRelative } from '@/lib/format';
import { errorMessage } from '@/lib/errors';
import type { ActivityEvent } from '@/lib/api/activityClient';

const PAGE_SIZE = 20;

function previewOf(event: ActivityEvent): string | undefined {
  const text = event.metadata?.text;
  return typeof text === 'string' ? text : undefined;
}

function verbText(event: ActivityEvent): string {
  switch (event.verb) {
    case 'posted':
      return '님이 새 글을 올렸어요';
    case 'liked':
      return '님이 게시물을 좋아해요';
    case 'commented':
      return '님이 댓글을 남겼어요';
    default:
      return `님이 ${event.objectType}을(를) ${event.verb} 했어요`;
  }
}

function toFeedItem(event: ActivityEvent, selfId?: string | null): ActivityFeedItem {
  return {
    id: event.id,
    actorName: displayName(event.actorUserId, selfId),
    verbText: verbText(event),
    preview: previewOf(event),
    timeText: formatRelative(event.createdAt),
    unread: event.readAt === null,
  };
}

export function FeedScreen() {
  const { accessToken } = useAuthStore();
  const enabled = Boolean(accessToken);
  const router = useRouter();

  const profile = useProfile(enabled);
  const feed = useFeed(0, PAGE_SIZE, enabled);
  const markRead = useMarkActivityRead();
  const markAllRead = useMarkActivitiesAllRead();
  const publish = usePublishActivity();

  const [draft, setDraft] = useState('');

  const myId = profile.data?.userId;
  const events = feed.data?.items ?? [];
  const items = events.map((e) => toFeedItem(e, myId));
  const unreadCount = events.filter((e) => e.readAt === null).length;

  const handlePost = async (e: React.FormEvent): Promise<void> => {
    e.preventDefault();
    const text = draft.trim();
    if (!text || !myId) return;
    const objectId = `post-${Date.now()}`;
    await publish.mutateAsync({
      verb: 'posted',
      objectType: 'post',
      objectId,
      audienceUserIds: [myId],
      metadata: { text },
      idempotencyKey: objectId,
    });
    setDraft('');
  };

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">피드</h1>
          <p className="mt-1 text-sm text-muted-foreground">친구들의 최근 소식을 확인하세요.</p>
        </div>
        <AnimatedBadge text="실시간 소식" color="#c026d3" />
      </header>

      {/* Compose box — publishes a 'posted' activity to the live backend. */}
      <Card className="shadow-md">
        <CardContent className="p-4">
          <form onSubmit={handlePost} className="flex items-center gap-3">
            <label htmlFor="compose" className="sr-only">
              새 글 작성
            </label>
            <Input
              id="compose"
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              placeholder="지금 무슨 생각을 하고 있나요?"
              maxLength={255}
              className="flex-1"
            />
            <Button type="submit" loading={publish.isPending} disabled={!draft.trim() || !myId}>
              <Send aria-hidden />
              <span className="hidden sm:inline">게시</span>
            </Button>
          </form>
          {publish.isError ? (
            <p role="alert" className="mt-2 text-xs font-medium text-destructive">
              {errorMessage(publish.error)}
            </p>
          ) : null}
        </CardContent>
      </Card>

      {unreadCount > 0 ? (
        <div className="flex justify-end">
          <Button
            variant="outline"
            size="sm"
            onClick={() => markAllRead.mutate()}
            loading={markAllRead.isPending}
          >
            <CheckCheck aria-hidden />
            모두 읽음 ({unreadCount})
          </Button>
        </div>
      ) : null}

      {feed.isLoading ? (
        <LoadingState label="피드 불러오는 중" />
      ) : feed.isError ? (
        <ErrorState message={errorMessage(feed.error)} onRetry={() => feed.refetch()} />
      ) : items.length === 0 ? (
        <EmptyState
          icon={<PartyPopper aria-hidden className="h-6 w-6" />}
          title="아직 소식이 없어요"
          description="위에서 첫 글을 남기면 피드가 채워집니다."
        />
      ) : (
        <ActivityFeed
          items={items}
          label="활동 피드"
          onSelect={(id) => {
            const target = events.find((e) => e.id === id);
            if (target) router.push(`/post/${encodeURIComponent(target.objectId)}`);
          }}
          onMarkRead={(id) => markRead.mutate(id)}
          markReadPendingId={markRead.isPending ? (markRead.variables ?? null) : null}
        />
      )}
    </div>
  );
}
