'use client';

import React from 'react';
import Link from 'next/link';
import { BellOff, Check, X } from 'lucide-react';
import { useAuthStore } from '@ax/core';
import { Button, Card, CardContent } from '@ax/ui';
import { StatusBadge } from '@ax/blocks';
import { EmptyState, ErrorState, LoadingState } from '@/components/screen-states';
import {
  useDismissNotification,
  useMarkNotificationRead,
  useNotifications,
} from '@/features/notifications/hooks';
import { formatRelative } from '@/lib/format';
import { errorMessage } from '@/lib/errors';

const PAGE_SIZE = 20;

export default function NotificationsPage() {
  const { accessToken } = useAuthStore();
  const enabled = Boolean(accessToken);
  const notifications = useNotifications(0, PAGE_SIZE, enabled);
  const markRead = useMarkNotificationRead();
  const dismiss = useDismissNotification();

  const items = notifications.data?.content ?? [];

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">알림</h1>
        <p className="mt-1 text-sm text-muted-foreground">새로운 소식과 활동을 확인하세요.</p>
      </header>

      {notifications.isLoading ? (
        <LoadingState label="알림 불러오는 중" />
      ) : notifications.isError ? (
        <ErrorState message={errorMessage(notifications.error)} onRetry={() => notifications.refetch()} />
      ) : items.length === 0 ? (
        <EmptyState
          icon={<BellOff aria-hidden className="h-6 w-6" />}
          title="알림이 없어요"
          description="새로운 활동이 생기면 여기에서 알려드릴게요."
        />
      ) : (
        <ul className="space-y-3">
          {items.map((n) => {
            const unread = n.status === 'UNREAD';
            return (
              <li key={n.id}>
                <Card className={unread ? 'border-[var(--ax-status-accent-fg)]/30 shadow-sm' : 'shadow-sm'}>
                  <CardContent className="flex items-start justify-between gap-4 p-4">
                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="text-sm font-semibold text-foreground">{n.title}</span>
                        <StatusBadge
                          status={unread ? 'submitted' : 'success'}
                          label={unread ? '안 읽음' : '읽음'}
                        />
                      </div>
                      {n.body ? (
                        <p className="mt-1 text-sm text-muted-foreground">{n.body}</p>
                      ) : null}
                      <div className="mt-1.5 flex items-center gap-3 text-xs text-muted-foreground">
                        <span>{formatRelative(n.createdAt)}</span>
                        {n.link ? (
                          <Link
                            href={n.link}
                            className="font-medium text-[var(--ax-status-accent-fg)] underline-offset-2 hover:underline"
                          >
                            바로가기
                          </Link>
                        ) : null}
                      </div>
                    </div>
                    <div className="flex shrink-0 items-center gap-1">
                      {unread ? (
                        <Button
                          variant="ghost"
                          size="sm"
                          aria-label="읽음 처리"
                          loading={markRead.isPending && markRead.variables === n.id}
                          onClick={() => markRead.mutate(n.id)}
                        >
                          <Check aria-hidden />
                        </Button>
                      ) : null}
                      <Button
                        variant="ghost"
                        size="sm"
                        aria-label="알림 삭제"
                        loading={dismiss.isPending && dismiss.variables === n.id}
                        onClick={() => dismiss.mutate(n.id)}
                      >
                        <X aria-hidden />
                      </Button>
                    </div>
                  </CardContent>
                </Card>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
