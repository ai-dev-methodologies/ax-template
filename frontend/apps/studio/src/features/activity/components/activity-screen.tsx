'use client';

import React, { useMemo } from 'react';
import { useRouter } from 'next/navigation';
import { Activity as ActivityIcon } from 'lucide-react';
import { useAuthStore } from '@ax/core';
import { Button } from '@ax/ui';
import { ActivityFeed, type ActivityFeedItem } from '@ax/blocks';
import { PageHeader } from '@/components/page-header';
import { ScreenEmpty, ScreenError, ScreenLoading } from '@/components/screen-states';
import {
  useActivityFeed,
  useMarkActivityRead,
  useMarkAllActivityRead,
} from '@/features/activity/hooks';
import { useProfile } from '@/features/profile/hooks';
import type { ActivityEvent } from '@/lib/api/activityClient';
import { formatRelativeKo } from '@/lib/format';
import { errorMessage } from '@/lib/errors';

/** Map an ActivityStreams verb to friendly Korean copy. */
function verbText(verb: string): string {
  switch (verb) {
    case 'uploaded':
      return '님이 작품을 올렸어요';
    case 'liked':
      return '님이 좋아요를 눌렀어요';
    case 'commented':
      return '님이 댓글을 남겼어요';
    case 'posted':
      return '님이 게시물을 올렸어요';
    default:
      return `님이 ${verb} 했어요`;
  }
}

function previewText(event: ActivityEvent): string | undefined {
  const meta = event.metadata ?? {};
  const fileName = typeof meta.fileName === 'string' ? meta.fileName : undefined;
  const text = typeof meta.text === 'string' ? meta.text : undefined;
  return fileName ?? text;
}

/**
 * Activity — the creator's timeline (uploads, likes, posts). Composes the
 * catalog ActivityFeed block: the app maps each ActivityEvent DTO to the block's
 * row shape, owns the data + the mark-read callbacks, and the block owns layout,
 * the read affordance, a11y, and the staggered entrance (calmed under
 * reduced-motion by the shell).
 */
export function ActivityScreen() {
  const router = useRouter();
  const enabled = Boolean(useAuthStore((s) => s.accessToken));
  const feed = useActivityFeed(0, 30, enabled);
  const profile = useProfile(enabled);
  const markRead = useMarkActivityRead();
  const markAll = useMarkAllActivityRead();

  const myId = profile.data?.userId;
  const myName = profile.data?.email ?? '나';

  const items = useMemo<ActivityFeedItem[]>(() => {
    const events = feed.data?.items ?? [];
    return events.map((event) => ({
      id: event.id,
      actorName: event.actorUserId === myId ? myName : '크리에이터',
      verbText: verbText(event.verb),
      preview: previewText(event),
      timeText: formatRelativeKo(event.createdAt),
      unread: event.readAt === null,
    }));
  }, [feed.data, myId, myName]);

  const hasUnread = items.some((i) => i.unread);

  return (
    <section aria-labelledby="activity-heading">
      <span id="activity-heading" className="sr-only">
        활동
      </span>
      <PageHeader
        title="활동"
        description="업로드, 좋아요, 게시물까지 — 스튜디오의 흐름을 한 곳에서 따라가세요."
        action={
          hasUnread ? (
            <Button
              variant="outline"
              size="lg"
              loading={markAll.isPending}
              onClick={() => markAll.mutate()}
            >
              모두 읽음 처리
            </Button>
          ) : undefined
        }
      />

      {feed.isLoading ? (
        <ScreenLoading label="활동 불러오는 중" />
      ) : feed.isError ? (
        <ScreenError error={new Error(errorMessage(feed.error))} />
      ) : items.length === 0 ? (
        <ScreenEmpty
          icon={<ActivityIcon className="h-10 w-10" />}
          title="아직 활동이 없어요"
          description="작품을 올리면 여기에 활동이 쌓여요."
          action={
            <Button size="lg" onClick={() => router.push('/upload')}>
              작품 올리기
            </Button>
          }
        />
      ) : (
        <ActivityFeed
          items={items}
          label="활동 피드"
          onMarkRead={(id) => markRead.mutate(id)}
          markReadPendingId={markRead.isPending ? markRead.variables ?? null : null}
        />
      )}
    </section>
  );
}
